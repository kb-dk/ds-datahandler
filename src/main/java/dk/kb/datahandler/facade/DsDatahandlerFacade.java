package dk.kb.datahandler.facade;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import dk.kb.datahandler.oai.OaiResponseFiltering;
import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.webservice.exception.InvalidArgumentServiceException;
import org.apache.commons.io.IOUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import dk.kb.datahandler.config.ServiceConfig;
import dk.kb.datahandler.model.v1.OaiJobDto;
import dk.kb.datahandler.model.v1.OaiTargetDto;
import dk.kb.datahandler.oai.OaiHarvestClient;
import dk.kb.datahandler.oai.OaiJobCache;
import dk.kb.datahandler.oai.OaiRecord;
import dk.kb.datahandler.oai.OaiResponse;
import dk.kb.datahandler.oai.OaiTargetJob;
import dk.kb.datahandler.util.HarvestTimeUtil;
import dk.kb.present.client.v1.DsPresentApi;
import dk.kb.datahandler.util.HttpPostUtil;
import dk.kb.storage.client.v1.DsStorageApi;
import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.storage.util.DsStorageClient;


public class DsDatahandlerFacade {

    private static final Logger log = LoggerFactory.getLogger(DsDatahandlerFacade.class);
    
    private static DsStorageApi storageClient;  
    private static DsPresentApi presentClient;

    /**
     * Ingest records directly into ds-storage from a zip-file containing multiple files that each is an xml-file with a single record
     *  
     * @param  origin The origin for collection documents. The origin must be defined in ds-storage. 
     * @param is Inputstream. Must be a zip-file containing single files that each is an XML record.
     * @return List of strings containing the records that failed parsing.
     */    
    public static ArrayList<String> ingestFromZipfile(String origin, InputStream is) throws Exception {
        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(is));

        DsStorageApi dsAPI = getDsStorageApiClient();
        ArrayList<String> errorRecords= new ArrayList<String>(); 

        ZipEntry entry = null;

        while ((entry = zis.getNextEntry()) != null) {

            String fileName=entry.getName();

            try {                
                String record_string = IOUtils.toString(zis, StandardCharsets.UTF_8);           
                Document record = OaiHarvestClient.sanitizeXml(record_string, null);

                //There are several 'mods:identifier' identifier tags, but the first is the URI always.
                Node item = record.getElementsByTagName("mods:identifier").item(0);            
                String identifier =  item.getTextContent();                               

                //Example: urn:uuid:096c9090-717f-11e0-82d7-002185371280
                identifier=identifier.replaceFirst("urn:uuid:", ""); // Clear this first part from the ID
                
                String recordId= origin+":"+identifier;
                log.info("Ingesting record filename from zip:"+fileName +" id:"+recordId);
                DsRecordDto dsRecord = new DsRecordDto();
                dsRecord.setId(recordId); 
                dsRecord.setOrigin(origin);
                dsRecord.setData(record_string);                                                 
                dsAPI.recordPost(dsRecord);  
                                                                
            }
            catch(Exception e) {
                errorRecords.add(fileName);
                log.error("Error parsing xml record for file:"+fileName, e);              
            }
        }

        IOUtils.closeQuietly(zis);        
        return errorRecords;         
    }

    
    /**  
     *  Will start indexing records from storage into solr. The workflow is
     *  1) Call ds-present that will extract records from ds-storage and xslt transform them into solr-add documents json
     *  2) Send the input stream with json documents directly to solr so it is not kept in memory.
     *  
     * @param dsPresentCollectionName This collectionName must be define on the ds-present server.
     * @exception Will throw exception is the dsPresentCollectionName is not known, or if server communication fails.
     */    
    public static void indexSolr(String dsPresentCollectionName)  throws Exception{
   
        //There is no way to validate dsPresentCollectionName exists unless calling again. The input stream is given directly to solr        
        
        //Below two lines would work if client was generated with streaming Api
        //DsPresentApi presentClient = getDsPresentApiClient();
        //StreamingOutput so = presentClient.getRecords(oaiTarget, 0L, -1L, "SolrJSON");
        
        String presentUrl =ServiceConfig.getDsPresentUrl()+"/records?&maxRecords=100000&format=SolrJSON&collection="+dsPresentCollectionName;
        URL presentURL = new URL(presentUrl);
        String solrUrl = ServiceConfig.getSolrUrl();        
        URL solrURL = new URL(solrUrl);
        
        HttpURLConnection httpURLConnection = (HttpURLConnection) presentURL.openConnection();        
        httpURLConnection.setRequestMethod("GET");        
        try (InputStream inputStream = httpURLConnection.getInputStream()){                 
            //Give input stream to the POST request.        
            HttpURLConnection solrServerConnection = (HttpURLConnection) solrURL.openConnection(); 
            solrServerConnection.setRequestProperty("Content-Type", "application/json");
            String solrResponse = HttpPostUtil.callPost(solrServerConnection, inputStream);
            log.info("Response from solr:"+solrResponse); //Example: {  "responseHeader":{    "rf":1,    "status":0,    "QTime":1348}}           

            if (solrResponse.indexOf("\"status\":0") < 0) {
                throw new Exception ("Unexpected status from solr:"+solrResponse);
            }            
        }
        catch(Exception e) { //
            log.error("Error calling ds-present or solr.",e.getMessage());
            throw new InternalServiceException("Error calling ds-present or solr");
        }
                      
     
    }
    

    /**
     * Starts a delta OAI harvest job for the target. The job will continue from last timestamp
     * saved on the file system for that target.
     * The job will harvest records from the OAI server and ingest them into DS-storage  
     *  
     * @param  oaiTargetName the location of the image, relative to the url argument
     * @return Number of harvested records.
     */    
    public static Integer oaiIngestDelta(String oaiTargetName) throws Exception {                

        //Test no job is running before starting new for same target
        validateNotAlreadyRunning(oaiTargetName);        
        OaiTargetDto oaiTargetDto = ServiceConfig.getOaiTargets().get(oaiTargetName);                
        if (oaiTargetDto== null) {
            throw new InvalidArgumentServiceException("No target found in configuration with name:'" + oaiTargetName + "' . See the config method for list of configured targets.");
        }

        OaiTargetJob job = createNewJob(oaiTargetDto);        

        //register job
        OaiJobCache.addNewJob(job);

        try {
            String datestamp = HarvestTimeUtil.loadLastHarvestTime(oaiTargetDto);        
            int number= oaiIngest(job , datestamp);
            OaiJobCache.finishJob(job, number,false);//No error
            return number;
        }
        catch(Exception e) {
            log.error("Oai delta harvest did not complete succesfull:"+oaiTargetName);
            job.setCompletedTime(System.currentTimeMillis());
            OaiJobCache.finishJob(job, 0,true);//Error                        
            throw new Exception(e);
        }
    }


    /**
     * Starts a full OAI harvest job for the target.
     * The job will harvest records from the OAI server and ingest them into DS-storage  
     *  
     * @param  oaiTargetName the location of the image, relative to the url argument
     * @return Number of harvested records.
     */    
    public static Integer oaiIngestFull(String oaiTargetName) throws Exception {


        //Test no job is running before starting new for same target
        validateNotAlreadyRunning(oaiTargetName);

        OaiTargetDto oaiTargetDto = ServiceConfig.getOaiTargets().get(oaiTargetName);   
        if (oaiTargetDto== null) {
            throw new InvalidArgumentServiceException("No target found in configuration with name:'"+oaiTargetName +"' . See the config method for list of configured targets.");            
        }

        OaiTargetJob job = createNewJob(oaiTargetDto);


        //register job
        OaiJobCache.addNewJob(job);            

        try {

            int number= oaiIngest(job , null);        
            OaiJobCache.finishJob(job, number,false);//No error
            return number;
        }
        catch(Exception e) {
            log.error("Oai full harvest did not complete succesfull:"+oaiTargetName);
            OaiJobCache.finishJob(job, 0, true);//Error
            job.setCompletedTime(System.currentTimeMillis());                                    
            throw new Exception(e);
        }
    }



    /**
     *  Gives a ist of both completed and running jobs with status. Jobs still running will be first.
     *  The completed jobs will only contain last 1000 completed jobs
     *  
     * @return List of jobs with status
     */    
    public static List<OaiJobDto> getJobs() throws Exception {    
        List<OaiJobDto> running=OaiJobCache.getRunningJobsMostRecentFirst();
        List<OaiJobDto> completed=OaiJobCache.getCompletedJobsMostRecentFirst();
        List<OaiJobDto> result = new ArrayList<OaiJobDto>();
        result.addAll(running);
        result.addAll(completed);
        return result;
    }


    /**
     * If from is null it will harvest everything.
     * Format for from is yyyy-MM-dd as this is only one supported by COPs/Cumulus.
     * Will be changed later when more OAI targets comes.
     *  
     */
    protected static Integer oaiIngest(OaiTargetJob job, String from) throws Exception {

        //In the OAI spec, the from parameter can be both yyyy-MM-dd or full UTC timestamp (2021-10-09T09:42:03Z)        
        //But COP only supports the short version. So when this is called use short format
        //Dirty but quick solution fix. Best would be if COP could fix it

        OaiTargetDto oaiTargetDto = job.getDto();

        if (from != null && oaiTargetDto.getUrl().indexOf("kb.dk/cop/")> 0) {
            from = from.substring(0,10);               
        }

        String origin=oaiTargetDto.getOrigin();
        String targetName = oaiTargetDto.getName();

        DsStorageApi dsAPI = getDsStorageApiClient();        
        OaiHarvestClient client = new OaiHarvestClient(job,from);
        OaiResponse response = client.next();

        AtomicInteger totalRecordsCount = new AtomicInteger(0);
        AtomicInteger manRelRefNot2Count = new AtomicInteger(0);

        while (response.getRecords().size() >0) {

            OaiRecord lastRecord = response.getRecords().get(response.getRecords().size()-1);

            if (targetName.startsWith("pvica")){
                OaiResponseFiltering.addToStorageWithPvicaFiltering(response, dsAPI, origin, totalRecordsCount, manRelRefNot2Count);
            } else {
                OaiResponseFiltering.addToStorageWithoutFiltering(response, dsAPI, origin, totalRecordsCount);
            }

            if (manRelRefNot2Count.intValue() > 0){
                log.info("Ingesting '{}' records from origin: '{}' out of a total of '{}' records. " +
                            "'{}' ManifestationRelRef != 2 have been skipped. ",
                        totalRecordsCount, origin, response.getTotalRecords(), manRelRefNot2Count);
            } else {
                log.info("Ingesting '{}' records from origin: '{}' out of a total of '{}' records.",
                        totalRecordsCount, origin, response.getTotalRecords());
            }

            //Update timestamp with timestamp from last OAI record.
            HarvestTimeUtil.updateDatestampForOaiTarget(oaiTargetDto,lastRecord.getDateStamp());

            response = client.next(); //load next (may be empty)            
        }

        if (response.isError()) {
            throw new InternalServiceException("Error during harvest for target:" + job.getDto().getName() + " after harvesting " + totalRecordsCount + " records");
        }

        log.info("Completed ingesting origin successfully:"+origin+ " records:"+totalRecordsCount);
        return totalRecordsCount.intValue();
    }

    /**
     * Generates a OaiRargetJob from an OaiTargetDto.
     * 
     * The job will have a unique timestamp used as ID.  
     *   
     * @param  dto 
     */
    public static synchronized OaiTargetJob createNewJob(OaiTargetDto dto) {                  

        long id = System.currentTimeMillis();
        try {
            Thread.sleep(1); // So next ID is different.
        }
        catch(Exception e) {
            //can not happen, noone will interrupt.            
        }

        OaiTargetJob  job = new OaiTargetJob(id, dto);                
        return job;                
    }

    private static DsStorageApi getDsStorageApiClient() {       
        if (storageClient!= null) {
          return storageClient;
        }
          
        String dsLicenseUrl = ServiceConfig.getDsStorageUrl();                                
        storageClient = new DsStorageClient(dsLicenseUrl);               
        return storageClient;
      }
    
  
    private static DsPresentApi getDsPresentApiClient() {       
        if (presentClient!= null) {
          return presentClient;
        }
          
       // String dsPresentUrl = ServiceConfig.getDsStorageUrl();                                
        String dsPresentUrl = "http://devel11.statsbiblioteket.dk:10001/ds-storage/";
        storageClient = new DsStorageClient(dsPresentUrl);               
        return presentClient;
      }
    
    

    private static synchronized void validateNotAlreadyRunning(String oaiTargetName) {
        boolean alreadyRunning= OaiJobCache.isJobRunningForTarget(oaiTargetName);        
        if (alreadyRunning) {
            throw new InvalidArgumentServiceException("There is already a job running for target:"+oaiTargetName);
        }
    }

}
