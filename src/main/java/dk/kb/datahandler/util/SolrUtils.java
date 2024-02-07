package dk.kb.datahandler.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import dk.kb.datahandler.config.ServiceConfig;
import dk.kb.datahandler.model.v1.IndexTypeDto;
import dk.kb.datahandler.solr.SolrIndexResponse;
import dk.kb.datahandler.solr.SolrResponseHeader;
import dk.kb.present.model.v1.FormatDto;
import dk.kb.present.util.DsPresentClient;
import dk.kb.util.webservice.stream.ContinuationInputStream;
import org.apache.http.client.utils.URIBuilder;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;

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
        SolrClient solrClient = new HttpSolrClient.Builder(solrQueryUrl).build();

        String storageMTime = "internal_storage_mTime";

        // Perform a query
        SolrQuery query = new SolrQuery("origin:"+ origin + " AND " + storageMTime + ":*");
        query.setFields(storageMTime);
        query.setSort(storageMTime, SolrQuery.ORDER.desc);
        query.setRows(1);
        // Have to add facet and highlights like this as the query.setFacet and query.setHighlight aren't appended
        // to the query.
        query.add("facet", "false");
        query.add("hl", "false");

        // Parse response to get last modified field
        QueryResponse response = solrClient.query(query);

        if (!response.getResults().isEmpty()) {
            Long lastStorageMTime = (Long) response.getResults().get(0).getFieldValue(storageMTime);
            return lastStorageMTime;
        } else {
            return 0L;
        }
    }

    /**
     * Index documents from a given oringin into the configured solr index.
     * @param origin    where the records come from. Has to be registered with DS-Storage
     * @param sinceTime A long representation of time since epoch.
     * @return          A status on how many records have been indexed.
     */
    public static String indexOrigin(String origin, Long sinceTime) throws IOException, URISyntaxException {
        //DS-present client
        DsPresentClient presentClient = new DsPresentClient(ServiceConfig.getConfig());
        // Solr update client
        URL solrUpdateUrl=new URIBuilder(ServiceConfig.getSolrUpdateUrl())
                .setParameter("commit", "true")
                .build().toURL();

        boolean hasMore=true;
        long batchSize= ServiceConfig.getSolrBatchSize();

        Long documents = 0L;
        String solrResponse = "";
        SolrIndexResponse finalResponse = new SolrIndexResponse();


        while (hasMore) {
            try (ContinuationInputStream<Long> solrDocsStream =
                         presentClient.getRecordsJSON(origin, sinceTime,batchSize, FormatDto.SOLRJSON)) {

                //POST request to Solr using the inputstream
                HttpURLConnection solrServerConnection = (HttpURLConnection) solrUpdateUrl.openConnection();
                solrResponse = HttpPostUtil.callPost(solrServerConnection, solrDocsStream , "application/json");

                if (!solrResponse.contains("\"status\":0")) {
                    log.error("Unexpectected reply from solr: '" + solrResponse + "'"); //Example: {  "responseHeader":{    "rf":1,    "status":0,    "QTime":1348}}
                    throw new IOException ("Unexpected status from solr: '" + solrResponse + "'");
                }
                if (solrDocsStream.getRecordCount() != null) {
                    documents += solrDocsStream.getRecordCount();
                }

                updateFinalResponse(solrResponse, finalResponse, documents);

                hasMore=solrDocsStream.hasMore();
                if (hasMore) {
                    sinceTime=solrDocsStream.getContinuationToken(); //Next batch start from here.
                }
            }
        }
        log.info("Solr index completed for origin: '{}', mTime: {}, #docs: {}",
                origin, sinceTime, documents);

        return solrIndexObjectAsJSON(finalResponse);
    }


    /**
     * Update the final {@code SolrIndexResponse} with the content from the single {@code individualSolrResponse}.
     * The updated {@code SolrIndexResponse} is used as the response for the endpoint
     * {@link dk.kb.datahandler.api.v1.impl.DsDatahandlerApiServiceImpl#indexSolr(String, Long, IndexTypeDto)}. This
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
                                            Long documents) throws JsonProcessingException {

        SolrResponseHeader currentResponseHeader = new SolrResponseHeader(individualSolrResponse);

        finalResponse.setLastSolrResponseHeader(currentResponseHeader);
        finalResponse.setAllDocumentsIndexed(documents);
        finalResponse.incrementCombinedQTime(currentResponseHeader.getqTime());
    }

    private static String solrIndexObjectAsJSON(SolrIndexResponse solrIndexResponse) throws JsonProcessingException {
        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        return ow.writeValueAsString(solrIndexResponse);
    }

}
