package dk.kb.datahandler.kaltura;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kaltura.client.enums.MediaType;

import dk.kb.datahandler.config.ServiceConfig;
import dk.kb.kaltura.client.DsKalturaClient;
import dk.kb.util.Resolver;


/**
 * Start job that will upload missing streams to kaltura.
 * 
 * Workflow:
 * 
 * 1) Extract records from Solr using mTimeFrom and with the condition 
 *    access_malfunction:false AND production_code_allowed:true AND NOT kaltura_id:*
 *    Only extract the few fields from solr that are required: title,description,file_id,id,resource_description,originates_from
 * 2) Check the file_id has not been uploaded to kaltura before. Skip stream if it is found   
 * 3) Calculate full path to the stream. Calculation depend on if record is DOMS or Preservica
 * 4) For each new stream:
 * 4.1) Upload the stream. (Notice some streams do not have extension, but this seems not to be an issue with kaltura).
 * 4.2) Update the record in storage with the kaltura_id. This will mark the storage record as modified.
 * 
 *  After completion the facade method will start a solr delta-index job.
 *  
 */
public class KalturaDeltaUploadJob {

    private static DsKalturaClient kalturaClient= null;
    private static final Logger log = LoggerFactory.getLogger(KalturaDeltaUploadJob.class);
 
    
    
    public static void main(String[] args)  throws Exception{        
      
        
        

       // boolean exist= doesFileIdExistInKaltura("5b7f80a4-580d-4ec4-a7e6-7e814af6a110");
        //System.out.print(exist);
       
        
      }


    public static void uploadStreamsToKaltura(long mTimeFrom) {


        /*   
         * 
         if ("VideoObject".equals(resourceDescription)) {             
             mediaType=MediaType.VIDEO;
             flavorParamId=ServiceConfig.getConfig().getInteger("kaltura.flavourParamIdVideo");
         }
         else {
             mediaType=MediaType.AUDIO;
             flavorParamId=ServiceConfig.getConfig().getInteger("kaltura.flavourParamIdAudio");
         }                 

         * 
        String filePath=KalturaFilePathUtil.generateStreamPath(referenceId, originatesFrom, resourceDescription);

        if (!validateFileExists(filePath)) {
            log.error("File stream not found='{}' for referenceId='{}'",filePath,referenceId);
            throw new IOException("File not found:"+filePath);
        }
*/

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
    public static SolrDocumentList fetchSolrRecords(long mTimeFrom, int batchSize) throws SolrServerException, IOException  {
        String solrUrl="http://ds-devel01.kb.dk:10007/solr/ds";
        String filterQuery="access_malfunction:false AND production_code_allowed:true AND NOT kaltura_id:*";    
        SolrClient client = new Http2SolrClient.Builder(solrUrl)     
                .withConnectionTimeout(1, TimeUnit.MINUTES)
                .build();
     
        String query="internal_storage_mTime:{"+mTimeFrom+" TO *]"; // mTimeFrom must be higher than startValue
        String fieldList="title,description,file_id,id,resource_description,originates_from";  //only extract fields we need
        System.out.println(query);
        
        try (client){  //autoclosable                
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(query);
        solrQuery.setFilterQueries(filterQuery); 
        solrQuery.set("facet", "false"); // very important. Must overwrite to false. Facets are very slow and expensive.
        solrQuery.add("sort", "internal_storage_mTime DESC"); //increasing order
        solrQuery.add("fl", fieldList); 
        solrQuery.setRows(batchSize);        
        QueryResponse response = client.query(solrQuery);        
        SolrDocumentList results = response.getResults();
        System.out.println("total hits:"+results.getNumFound());
        System.out.println("results:"+results.size());        
        client.close(); //Add autoclosable 
        return results;
       }
    }

    
    /**
     * 
     * 
     * @param title
     * @param description
     * @param fileId the fileId. This value will be used to calculate path of stream depending on originatesFrom
     * @param originatesFrom DOMS or Preservica
     * @param resourceDescription VideoObject or AudioObjext
     * @return The Kaltura entry id if upload is successfull
     * @throws IOException If upload fails 
     */
    public static String uploadStream(String title, String referenceId,String description,String filePath, String tag, String resourceDescription, MediaType mediaType, int flavourParamId) throws IOException {

        initKalturaClient();
         if (!validateFileExists(filePath)) {
             log.warn("File does not exist='{}'",filePath);
             throw new IOException("File not found:"+filePath);
         }                
        
        log.info("Starting upload stream:"+filePath +" with flavorParamId:"+flavourParamId); 
        String entryId=kalturaClient.uploadMedia(filePath, referenceId,mediaType,  title, description, tag,flavourParamId);
        log.info("Upload completet for stream:"+filePath +" with filerefence:"+referenceId +" and got kaltura entryid:"+entryId);        
        return entryId;
                
    }
    
    /**
     * Check if a file_id already does exist in kaltura. Do not upload a new stream if it is already uploaded (and maybe failed).
     * 
     * @param file_id Our reference to the stream.
     * @throws IOException 
     * 
     */
    private static boolean doesFileIdExistInKaltura(String file_id) throws IOException {
        
         initKalturaClient();
         
         String kalturaInternalId = kalturaClient.getKulturaInternalId(file_id);
         if (kalturaInternalId != null) {             
             log.warn("Kaltura fileId:"+file_id +" is already in kaltura with entry_id:"+kalturaInternalId);
             return true;
         }
         log.debug("File_id ='{}' was not found in kaltura", file_id);
         return false;         
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
            return; //already inititalised
        }
        
        String kalturaUrl = ServiceConfig.getConfig().getString("kaltura.url");
        Integer partnerId = ServiceConfig.getConfig().getInteger("kaltura.partnerId");        
        String adminSecret=null;// We use appTokens instead
        String userId =  ServiceConfig.getConfig().getString("kaltura.userId");                 
        String token= ServiceConfig.getConfig().getString("kaltura.token");        
        String tokenId= ServiceConfig.getConfig().getString("kaltura.tokenId");
        try {
         kalturaClient = new DsKalturaClient(kalturaUrl, userId, partnerId, token,tokenId,adminSecret, 86400);
        }
        catch(Exception e) {
            log.error("Could not instantiate DsKaltura client.",e);
        }
        
    }
    
    
    
}
