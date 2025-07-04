package dk.kb.datahandler.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import dk.kb.datahandler.config.ServiceConfig;
import dk.kb.datahandler.model.v1.TypeDto;
import dk.kb.datahandler.solr.SolrIndexResponse;
import dk.kb.datahandler.solr.SolrResponseHeader;
import dk.kb.present.model.v1.FormatDto;
import dk.kb.present.util.DsPresentClient;
import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.webservice.stream.ContinuationInputStream;
import org.apache.http.client.utils.URIBuilder;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpJdkSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicLong;

public class SolrUtils {
    private static final Logger log = LoggerFactory.getLogger(SolrUtils.class);

    /**
     * Get the latest MTime for records in the backing storage represented in the existing solr index
     * for the requested origin.
     * @param origin to extract latest mTime for.
     * @return the latest mTime as a long representing time in epoch with three added digits.
     */
    public static Long getLatestMTimeForOrigin(String origin) throws SolrServerException, IOException {
        // Solr query client
        String solrQueryUrl = ServiceConfig.getSolrQueryUrl();
        String storageMTime;
        QueryResponse response;
        try (SolrClient solrClient = new HttpJdkSolrClient.Builder(solrQueryUrl).build()) {

            storageMTime = "internal_storage_mTime";

            // Perform a query
            SolrQuery query = new SolrQuery("origin:" + origin + " AND " + storageMTime + ":*");
            query.setFields(storageMTime);
            query.setSort(storageMTime, SolrQuery.ORDER.desc);
            query.setRows(1);
            // Have to add facet and highlights like this as the query.setFacet and query.setHighlight aren't appended
            // to the query.
            query.add("facet", "false");
            query.add("hl", "false");

            // Parse response to get last modified field
            response = solrClient.query(query);
        }

        if (!response.getResults().isEmpty()) {
            Long lastStorageMTime = (Long) response.getResults().get(0).getFieldValue(storageMTime);
            return lastStorageMTime;
        } else {
            return 0L;
        }
    }

    /**
     * Index documents from a given origin into the configured solr index.
     * @param origin    where the records come from. Has to be registered with DS-Storage
     * @param sinceTime A long representation of time since epoch.
     * @return          A status on how many records have been indexed.
     */
    public static String indexOrigin(String origin, Long sinceTime){
        //DS-present client
        DsPresentClient presentClient = new DsPresentClient(ServiceConfig.getDsPresentUrl());
        // Solr update client
        URL solrUpdateUrl;
                
        try {
            solrUpdateUrl = new URIBuilder(ServiceConfig.getSolrUpdateUrl())
                    .setParameter("commit", "true")
                    .build().toURL();
        } catch (MalformedURLException | URISyntaxException e) {
            log.warn("Update URL for solr could not be constructed. Tried to build URL from '{}'.", ServiceConfig.getSolrUpdateUrl());
            throw new InternalServiceException(e);
        }

        boolean hasMore=true;
        long batchSize= ServiceConfig.getSolrBatchSize();

        Long documents = 0L;
        String solrResponse;
        SolrIndexResponse finalResponse = new SolrIndexResponse();
        log.info("Starting indexing of records with sinceTime: '{}' from origin: '{}'", sinceTime, origin);

        AtomicLong bytesCounter = new AtomicLong();
        
        while (hasMore) {
            try (ContinuationInputStream<Long> solrDocsStream =
                         presentClient.getRecordsJSON(origin, sinceTime,batchSize, FormatDto.SOLRJSON)) {
                log.info("Indexing {} records from DS-storage origin '{}' to solr. '{}' records have been indexed through this request.",
                        solrDocsStream.getRecordCount(), origin, documents);

                //POST request to Solr using the inputstream
                try {
                    HttpURLConnection solrServerConnection = (HttpURLConnection) solrUpdateUrl.openConnection();
                    solrResponse = HttpPostUtil.callPostWithBytesCounter(solrServerConnection, solrDocsStream , "application/json", bytesCounter);

                    if (bytesCounter.get() < 1000L * ServiceConfig.getSolrBatchSize()) {
                        // Solr records contain approx. 1800 bytes, and they are probably only growing in size.
                        // The tiniest I've seen is an average of 1500 bytes measured over 500 record.
                        log.warn("The posted stream contained less than a thousand bytes pr record. " +
                                    "Records could be missing data.");
                    }

                    if (!solrResponse.contains("\"status\":0")) {
                        log.error("Unexpected reply from solr: '" + solrResponse + "'"); //Example: {  "responseHeader":{    "rf":1,    "status":0,    "QTime":1348}}
                        throw new IOException ("Unexpected status from solr: '" + solrResponse + "'");
                    }
                } catch (IOException e) {
                    log.warn("An error occurred when posting the records to Solr at: '{}'", solrUpdateUrl);
                    throw new InternalServiceException(e);
                }

                if (solrDocsStream.getRecordCount() != null) {
                    documents += solrDocsStream.getRecordCount();
                    log.info("indexed #records="+solrDocsStream.getRecordCount());
                }

                updateFinalResponse(solrResponse, finalResponse, documents);

                hasMore=solrDocsStream.hasMore();        
                if (hasMore) {                    
                    sinceTime=solrDocsStream.getContinuationToken(); //Next batch start from here.
                } 
                
            } catch (IOException e) {
                log.warn("An error occurred when streaming records from DsPresent. DsPresentClient.getRecordsJSON() " +
                        "was called with the following params: origin='{}', mTime='{}', maxRecords='{}', format='{}'",
                        origin, sinceTime, batchSize, FormatDto.SOLRJSON);
                throw new InternalServiceException(e);
            }
        }
        log.info("Solr index completed for origin: '{}', mTime: {}, #docs: {}",
                origin, sinceTime, documents);
        
        //Build suggest only if there was any new documents.
        if (finalResponse.getAllDocumentsIndexed() >0) {        
           try {
            log.info("Start building solr suggest index because at least 1 documents was indexed. #=:"+finalResponse.getAllDocumentsIndexed());     
            buildSuggestIndex();          
            log.info("Finished building solr suggest index.");
           }
           catch (IOException | SolrServerException e) {
             log.warn("An error occurred when updating the solr suggester index.");
             throw new InternalServiceException(e);
           }
        }                
        return solrIndexObjectAsJSON(finalResponse);
    }


    /**
     * Update the final {@code SolrIndexResponse} with the content from the single {@code individualSolrResponse}.
     * The updated {@code SolrIndexResponse} is used as the response for the endpoint
     * {@link dk.kb.datahandler.api.v1.impl.DsDatahandlerApiServiceImpl#indexSolr(String, Long, TypeDto)}. This
     * updated response contains information on the amount of documents that have been indexed in total and not just
     * during the last batch of the stream.
     * @param individualSolrResponse a string representation of a JSON solr response returned when indexing a batch of
     *                               documents. Eg:
     * <pre>
     * {"responseHeader": {    <br>
     *   "rf":1, <br>
     *   "status":0, <br>
     *   "QTime":1348}}  <br>
     *</pre>
     * @param finalResponse         containing the {@code rf} value from the latest added {@code individualSolrResponse},
     *                              the {@code status} value from the latest added {@code individualSolrResponse}
     *                              the combined {@code QTime} for all added response headers and the total amount
     *                              of documents indexed.
     *
     * @param documents             The total amount of documents indexed.
     */
     public static void updateFinalResponse(String individualSolrResponse, SolrIndexResponse finalResponse,
                                            Long documents) {

        SolrResponseHeader currentResponseHeader = new SolrResponseHeader(individualSolrResponse);

        finalResponse.setLastSolrResponseHeader(currentResponseHeader);
        finalResponse.setAllDocumentsIndexed(documents);
        finalResponse.incrementCombinedQTime(currentResponseHeader.getqTime());
    }

    private static String solrIndexObjectAsJSON(SolrIndexResponse solrIndexResponse) {
        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String result = "";
        try {
            result = ow.writeValueAsString(solrIndexResponse);
        } catch (JsonProcessingException e) {
            log.warn("An error occurred when trying to convert a response from solr to JSON. Indexing has not been affected ");
        }

        return result;
    }

    /**
     * Send a request to build the index for the suggest component in the solr write collection
     */
    public static QueryResponse buildSuggestIndex() throws SolrServerException, IOException {
        String solrUrl = ServiceConfig.getSolrWriteCollectionUrl();
        try (SolrClient solrClient = new HttpJdkSolrClient.Builder(solrUrl).build()) {

            // Perform a query at suggest handler
            SolrQuery query = new SolrQuery();
            query.setRequestHandler("/suggest");
            query.set("suggest.build", "true");
            log.info("Starts building suggest index by querying '{}' with this request: '{}'.", solrUrl, query);

            // Fire the query and build the suggest index
            return solrClient.query(query);
        }
    }

}
