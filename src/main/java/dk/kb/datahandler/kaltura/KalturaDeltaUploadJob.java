package dk.kb.datahandler.kaltura;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kaltura.client.enums.MediaType;

import dk.kb.datahandler.config.ServiceConfig;
import dk.kb.kaltura.client.DsKalturaClient;
import dk.kb.storage.model.v1.MappingDto;
import dk.kb.storage.util.DsStorageClient;
import dk.kb.util.webservice.exception.InternalServiceException;

/**
 * This class has a single public method to upload all kaltura streams with from mTime (in Solr) and higher values. 
 */
public class KalturaDeltaUploadJob {

    private static DsKalturaClient kalturaClient = null;
    private static final Logger log = LoggerFactory.getLogger(KalturaDeltaUploadJob.class);

   
    /**
    * <p>
    * Start job that will upload missing streams to kaltura.
    * <p>
    * Workflow:
    * <p>
    * 1) Extract records from Solr using mTimeFrom and with the condition
    * access_malfunction:false AND production_code_allowed:true AND NOT
    * kaltura_id:* Only extract the few fields from solr that are required:
    * title,description,file_id,id,resource_description,originates_from 
    * 
    * <p>
    * 2) Calculate full path to the stream. Calculation depend on if record is DOMS or Preservica. Skip if files does not exist.
    * <p>
    *  3) Check that the file_id has not been uploaded to kaltura before. Skip stream if it is found in kaltura.
    * <p>   
    * 4) For each new stream:
    * <p>
    *  4.1) Upload the stream to kaltura. (Notice some streams do not have extension, but this seems not to be an issue with kaltura).
    * <p>
    *  4.2) Update the record in storage with the kaltura_id. This will mark the storage record as modified.
    * <p>
    * After completion the facade method will start a solr delta-index job.
    * 
    * @param mTimeFrom Upload all streams for records in solr with mTimeFrom higher than this value.
    * 
    * @throws InternalServiceException If any Solr or Kaltura call fails. Stop uploading more. Maybe allow single kaltura upload jobs to fail later.
    * 
    */
    
    public static int uploadStreamsToKaltura(long mTimeFrom) throws InternalServiceException{

        boolean moreSolrRecords = true;
        long mTimeFromCurrent = mTimeFrom;
        int numberStreamsUploaded=0;
        String uploadTagForKaltura=getUploadTagForKaltura();  

        String dsStorageUrl = ServiceConfig.getDsStorageUrl();
        DsStorageClient storageClient = new DsStorageClient(dsStorageUrl);
                        
        while (moreSolrRecords) {
            try {
                SolrDocumentList docs = fetchSolrRecords(mTimeFromCurrent, 500);
                if (docs.getNumFound() == 0) {
                    moreSolrRecords = false; // End loop
                }

                for (SolrDocument doc : docs) {
                   String resourceDescription=(String) doc.getFieldValue("resource_description");

                   String title="";//Default
                   ArrayList<String> titles=(ArrayList<String>) doc.getFieldValue("title"); //multivalue
                   if (titles.size() >0) {
                       title = titles.get(0);// take first
                   }                   
                   String description=(String) doc.getFieldValue("description");
                   String fileId=(String) doc.getFieldValue("file_id");
                   String id=(String) doc.getFieldValue("id");
                   String originatesFrom= (String) doc.getFieldValue("originates_from");                                       
                   long mTime= (Long) doc.getFieldValue("internal_storage_mTime");
                   
                   mTimeFromCurrent=mTime; //update mTime for next call                   
                   
                 // Check file not already in kaltura. 
                 try {
                                    
                   //upload stream
                   MediaType mediaType = KalturaUtil.getMediaType(resourceDescription);
                   int flavourParamId = KalturaUtil.getFlavourParamId(mediaType);
                   String path =KalturaUtil.generateStreamPath(fileId, originatesFrom, resourceDescription);
                   log.info("starting uploading stream='{}' with title='{}",path,title);
                 
                   if (!validateFileExists(path)) {
                       log.warn("File does not exist='{}'. Skipping upload", path);     
                       continue;
                   }
                   
                   boolean inKaltura=doesFileIdExistInKaltura(fileId);
                   if (inKaltura) {
                       log.warn("Stream allready found in kaltura. FileId="+fileId);
                       continue; //Do not try upload
                   }
                   
                   try {
                     String kalturaId=uploadStream(title, fileId, description, path, uploadTagForKaltura,mediaType,flavourParamId);
                     log.info("Upload stream='{}' and got kalturaId='{}'",path,kalturaId);
                     numberStreamsUploaded++;
                     //update storage record with kalturaId
                     
                     MappingDto mapping = new MappingDto();
                     mapping.setReferenceId(fileId);
                     mapping.setKalturaId(kalturaId);
                     storageClient.mappingPost(mapping);
                     log.info("Updated kaltura mapping in storage for fileId:"+fileId);                     
                   }
                   catch(Exception e) {  //Stop delta job
                       log.error("Failed uploading stream to kaltura with fileId='{', path='{}', title='{}'",fileId,path,title);
                       throw new InternalServiceException("Failed uploading stream to kaltura with fileId="+fileId);
                   }
                                    
                 }
                 catch(Exception e) { 
                 //Totally stop all uploads if a single call fails. Change strategy if this does seem to happen sporadic
                   log.error("Error kaltura lookup for fileId:"+fileId,e);
                   throw new InternalServiceException("Error kaltura lookup for fileId:"+fileId);                         
                 }                                      
                }
                // Check file not already in kaltura

            } catch (SolrServerException | IOException e) {
                // Can not fetch more records. Stop delta upload
                moreSolrRecords=false;
                log.error("Could not fetch more solr records from mTime:" + mTimeFromCurrent);                
                throw new InternalServiceException("Could not fetch more solr records from mTime:" + mTimeFromCurrent);
            }              
        }
        return numberStreamsUploaded;

    }

    /**
     * Make Solr call to fetch records without a stream registered in kaltura.
     * 
     * @param mTimeFrom Only extract records with mTime higher that this value
     * @param batchSize solr batch size.
     * @return
     * @throws SolrServerException
     * @throws IOException
     */
    public static SolrDocumentList fetchSolrRecords(long mTimeFrom, int batchSize) throws SolrServerException, IOException {
        String solrUrl = "http://ds-devel01.kb.dk:10007/solr/ds";
        String filterQuery = "access_malfunction:false AND production_code_allowed:true AND NOT kaltura_id:*";
        SolrClient client = new Http2SolrClient.Builder(solrUrl).withConnectionTimeout(1, TimeUnit.MINUTES).build();

        String query = "internal_storage_mTime:{" + mTimeFrom + " TO *]"; // mTimeFrom must be higher than startValue
        String fieldList = "title,description,file_id,id,resource_description,originates_from,internal_storage_mTime"; // only extract fields we need
        System.out.println(query);

        try (client) { // autoclosable
            SolrQuery solrQuery = new SolrQuery();
            solrQuery.setQuery(query);
            solrQuery.setFilterQueries(filterQuery);
            solrQuery.set("facet", "false"); // very important. Must overwrite to false. Facets are very slow and expensive.
            solrQuery.set("hl",false);// no highlights
            solrQuery.set("spellcheck", false); //No spellcheck
            solrQuery.add("sort", "internal_storage_mTime ASC"); // increasing order
            solrQuery.add("fl", fieldList);
            solrQuery.setRows(batchSize);
            QueryResponse response = client.query(solrQuery);
            SolrDocumentList results = response.getResults();
            log.info("Load solr records for delta upload:" + results.getNumFound());
            return results;
        }
    }

    /**
     * 
     * 
     * @param title  Title field for kaltura
     * @param description Description field for kaltura
     * @param fileId  This value will be used to calculate path of stream depending on originatesFrom
     * @param originatesFrom      DOMS or Preservica
     * @param resourceDescription VideoObject or AudioObjext
     * @return The Kaltura entry id if upload is successful
     * @throws IOException If upload fails
     */
    public static String uploadStream(String title, String referenceId, String description, String filePath, String tag, MediaType mediaType, int flavourParamId) throws IOException {

        initKalturaClient();
        log.info("Starting upload stream:" + filePath + " with flavorParamId:" + flavourParamId);
        String entryId = kalturaClient.uploadMedia(filePath, referenceId, mediaType, title, description, tag, flavourParamId);
        log.info("Upload completed for stream:" + filePath + " with filerefence:" + referenceId + " and got kaltura entryid:" + entryId);
        return entryId;

    }

    /**
     * Check if a file_id already does exist in kaltura. Do not upload a new stream
     * if it is already uploaded (and maybe failed).
     * 
     * @param file_id Our reference to the stream.
     * @throws IOException
     * 
     */
    private static boolean doesFileIdExistInKaltura(String file_id) throws IOException {

        initKalturaClient();

        String kalturaInternalId = kalturaClient.getKulturaInternalId(file_id);
        if (kalturaInternalId != null) {
            log.warn("Kaltura fileId:" + file_id + " is already in kaltura with entry_id:" + kalturaInternalId);
            return true;
        }
        log.debug("File_id ='{}' was not found in kaltura", file_id);
        return false;
    }

    /*
     * Example: delta-2025-05-01 
     * 
     */
     private static String getUploadTagForKaltura() {        
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd",Locale.getDefault());
        String yyyyMMdd = formatter.format(new Date());
        return "delta-"+yyyyMMdd;                
    }
    
    /*
     * Validate file exists on the file system.
     */
    private static boolean validateFileExists(String filePath) {
        Path path = Paths.get(filePath);
        return Files.exists(path);
    }

    private static void initKalturaClient() {
        if (kalturaClient != null) {
            return; // already inititalised
        }

        String kalturaUrl = ServiceConfig.getConfig().getString("kaltura.url");
        Integer partnerId = ServiceConfig.getConfig().getInteger("kaltura.partnerId");
        String adminSecret = null;// We use appTokens instead
        String userId = ServiceConfig.getConfig().getString("kaltura.userId");
        String token = ServiceConfig.getConfig().getString("kaltura.token");
        String tokenId = ServiceConfig.getConfig().getString("kaltura.tokenId");
        try {
            kalturaClient = new DsKalturaClient(kalturaUrl, userId, partnerId, token, tokenId, adminSecret, 86400);
        } catch (Exception e) {
            log.error("Could not instantiate DsKaltura client.", e);
        }

    }

}
