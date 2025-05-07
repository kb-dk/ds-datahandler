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
import org.apache.solr.client.solrj.impl.HttpJdkSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kaltura.client.enums.MediaType;

import dk.kb.datahandler.config.ServiceConfig;
import dk.kb.kaltura.client.DsKalturaClient;
import dk.kb.storage.model.v1.StreamErrorTypeDto;
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
    * 2) Calculate full path to the stream. Calculation depend on if record is DOMS or Preservica.
    *    Validate file exists and not too short bytesize. If this happens mark the record with error and skip.
    * <p>
    *  3) Check that the file_id has not been uploaded to kaltura before. If it has use the kaltura internal id for this record.
    * <p>   
    * 4) For each new stream:
    * <p>
    *  4.1) Upload the stream to kaltura. (Notice some streams do not have extension, but this seems not to be an issue with kaltura). The kaltura 'tag'
    *  for upload is 'delta-2025-05-01' where last part is current day. The kaltura tag-field is an internal kaltura field that we can use to see in which batch 
    *  the file was uploaded, and can also be used to search and delete all streams with this tag if something goes wrong.
    * <p>
    *  4.2) Update the record's kalturaid in ds-storage 
    * <p>
    * All records that has been updated with error or kalturaId will have mTime updated to new value.
    * After completion the facade method will start a solr delta-index job. 
    * 
    * @param mTimeFrom Upload all streams for records in solr with mTimeFrom higher than this value. 
    * @throws InternalServiceException If any Solr or Kaltura call fails. Stop uploading more. Maybe allow single kaltura upload jobs to fail later.  
    */
    
    public static int uploadStreamsToKaltura(long mTimeFrom) throws InternalServiceException{

        boolean moreSolrRecords = true;
        long mTimeFromCurrent = mTimeFrom;
        int numberStreamsUploaded=0;
        String uploadTagForKaltura=getUploadTagForKaltura();  
        //The minimumFileSizeInBytesvalue has been defined by Asger+Petur. It has been burned into the kaltura bulk upload and must not be changed, unless we start with a new empty kaltura partnerid.
        //Next time I recommend a much higher value such as 4096 etc, this is a few seconds of audio.
        long minimumFileSizeInBytes=700; 
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
                   long recordMtime  = (Long) doc.getFieldValue("internal_storage_mTime");
                   
                   mTimeFromCurrent=recordMtime ; //update mTime for next call                   
                                 
                   try {                                    
                   //upload stream
                     MediaType mediaType = KalturaUtil.getMediaType(resourceDescription);
                     int flavourParamId = KalturaUtil.getFlavourParamId(mediaType);
                     String path =KalturaUtil.generateStreamPath(fileId, originatesFrom, resourceDescription);
                     log.info("validating stream='{}' with title='{}'",path,title);                 
                     String fileError= hasStreamFileError(path, minimumFileSizeInBytes);
                     if (fileError != null) {
                       log.warn("File does not exist='{}' or size in bytes less than '{}'. Error='{}'. Id='{}'. Skipping upload", path, minimumFileSizeInBytes,fileError,id);     
                       updateKalturaIdForRecord(storageClient, fileId, fileError);
                       continue;                       
                     }
                   
                   // Check file not already in kaltura. 
                   String kalturaInternalId=getInternalIdKaltura(fileId);
                   if (kalturaInternalId != null) {
                       log.warn("Stream allready found in kaltura. FileId='{}' and has kalturaId='{}'. Setting this kalturaId for recordId='{}'", fileId,kalturaInternalId,id);
                       updateKalturaIdForRecord(storageClient, fileId, kalturaInternalId);                       
                       continue; //Do not upload stream
                   }
                   
                   try {
                     String kalturaId=uploadStream(title, fileId, description, path, uploadTagForKaltura,mediaType,flavourParamId);
                     log.info("Upload stream='{}' and got kalturaId='{}'",path,kalturaId);
                     numberStreamsUploaded++; //Success count
                     //update storage record with kalturaId                     
                     updateKalturaIdForRecord(storageClient, fileId, kalturaId);
                     log.info("Updated kaltura mapping in storage for fileId='{}':",fileId);  
                     continue; //Not required since nothing else happens below. But for consistency.
                   }
                   catch(Exception e) {  //Stop delta job
                       log.error("Failed uploading stream to kaltura with fileId='{}', path='{}', title='{}', error='{}'",fileId,path,title,e.getMessage());
                       updateKalturaIdForRecord(storageClient, fileId, StreamErrorTypeDto.API.getValue()); //Mark as API error
                       throw new InternalServiceException("Failed uploading stream to kaltura with fileId="+fileId);
                   }                                    
                 }
                 catch(Exception e) { 
                   //Totally stop all uploads if a single call fails. Change strategy if this does seem to happen sporadic
                   //Delta upload can be started again. We want to detect this error and not ignore it.
                   log.error("Error kaltura lookup for fileId='{}'"+fileId,e);
                   //Do not mark record with error. We need to know why this happens.
                   throw new InternalServiceException("Error kaltura lookup for fileId:"+fileId);                         
                 }                                      
                }
                // Check file not already in kaltura

            } catch (SolrServerException | IOException e) {
                // Can not fetch more records. Stop delta upload
                moreSolrRecords=false;
                log.error("Could not fetch more solr records from mTime={}",mTimeFromCurrent);                
                throw new InternalServiceException("Could not fetch more solr records from mTime:" + mTimeFromCurrent);
            }              
        }
        return numberStreamsUploaded;

    }

    private static void updateKalturaIdForRecord(DsStorageClient storageClient, String fileId, String kalturaInternalId) {     
           storageClient.updateKalturaIdForRecord(fileId, kalturaInternalId);
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
        String solrUrl = ServiceConfig.getSolrQueryUrl();        
        String filterQuery = "access_malfunction:false AND production_code_allowed:true AND NOT kaltura_id:*";  // only valid streams that does not have kaltura id already
        
        HttpJdkSolrClient client = new HttpJdkSolrClient.Builder(solrUrl).build();
                
        String query = "internal_storage_mTime:[" + mTimeFrom + " TO *]"; // mTimeFrom must start with this value or higher.
        String fieldList = "title,description,file_id,id,resource_description,originates_from,internal_storage_mTime"; // only extract fields we need

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
            log.info("Load solr records for delta upload={}", results.getNumFound());
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
        log.info("Starting upload stream. FilePath='{}' with flavorParamId='{}'", filePath,flavourParamId);
        String entryId = kalturaClient.uploadMedia(filePath, referenceId, mediaType, title, description, tag, flavourParamId);
        log.info("Upload completed. FilePath='{}' with filerefence='{}' and got kaltura entryid='{}'" + filePath,referenceId,entryId);
        return entryId;

    }

    /**
     * Check if a file_id already does exist in kaltura. Then it is already uploaded.
     * There can be meta-data errors where different records points to same stream.
     * 
     * @param file_id Our reference to the stream.
     * @return kalturaId or null if does not exist.
     * @throws IOException
     * 
     */
    private static String getInternalIdKaltura(String file_id) throws IOException {

        initKalturaClient();

        String kalturaInternalId = kalturaClient.getKulturaInternalId(file_id);
        if (kalturaInternalId != null) {
            log.warn("Kaltura fileId='{}' is already in kaltura with entry_id='{}'",file_id,kalturaInternalId);
        }
        return kalturaInternalId;
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
    
    /**
     * Validate file exists on the file system and also minimum size in bytes.
     * Will return null if everything is validated or error message on error.
     * Errors are:
     * <p>
     * StreamErrorTypeDto.FILE_TOO_SHORT<b>
     * StreamErrorTypeDto.FILE_MISSING
     * <p>
     * 
     * 
     * @param filePath fill path to stream. 
     * @param minimum size in bytes allowed
     * 
     * @return  StreamErrorTypeDto error or null if file exists has large enough.
     * 
     */
    private static String hasStreamFileError(String filePath,long minimumSizeInBytes) {
      Path path = Paths.get(filePath);         
       if  (!Files.exists(path)){
           return StreamErrorTypeDto.FILE_MISSING.getValue();
       }
       long size=0;
       try {
           size= Files.size(path);
         if (size<minimumSizeInBytes) {
             log.warn("File '{}' exists but below minimum bytesize, size='{}'",filePath,size);
             return StreamErrorTypeDto.FILE_TOO_SHORT.getValue();                          
         }
       }
         catch(Exception e) {
           return StreamErrorTypeDto.FILE_MISSING.getValue(); //Can not happen, but need to return value.
        }       
       log.debug("File '{}' exists and has size='{}'",filePath,size);
       return null;
    }

    private static void initKalturaClient() {
        if (kalturaClient != null) {
            return; // already inititalised
        }

        String kalturaUrl = ServiceConfig.getKalturaUrl();
        Integer partnerId = ServiceConfig.getKalturaPartnerId();
        String adminSecret = null;// We use appTokens instead
        String userId = ServiceConfig.getKalturaUserId();
        String token = ServiceConfig.getKalturaToken();
        String tokenId = ServiceConfig.getKalturaTokenId();
        int dayInSeconds=86400;  //60*60*24
        try {
            kalturaClient = new DsKalturaClient(kalturaUrl, userId, partnerId, token, tokenId, adminSecret, dayInSeconds);
        } catch (Exception e) {
            log.error("Could not instantiate DsKaltura client.", e);
        }

    }

}
