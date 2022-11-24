package dk.kb.datahandler.facade;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import dk.kb.datahandler.backend.api.v1.DsStorageApi;
import dk.kb.datahandler.backend.invoker.v1.ApiClient;
import dk.kb.datahandler.backend.model.v1.DsRecordDto;
import dk.kb.datahandler.config.ServiceConfig;
import dk.kb.datahandler.model.v1.OaiJobDto;
import dk.kb.datahandler.model.v1.OaiTargetDto;
import dk.kb.datahandler.oai.OaiHarvestClient;
import dk.kb.datahandler.oai.OaiJobCache;
import dk.kb.datahandler.oai.OaiRecord;
import dk.kb.datahandler.oai.OaiResponse;
import dk.kb.datahandler.oai.OaiTargetJob;
import dk.kb.datahandler.util.HarvestTimeUtil;
import dk.kb.datahandler.webservice.exception.InternalServiceException;
import dk.kb.datahandler.webservice.exception.InvalidArgumentServiceException;


public class DsDatahandlerFacade {

    private static final Logger log = LoggerFactory.getLogger(DsDatahandlerFacade.class);

    /**
     * Ingest records directly into ds-storage from a zip-file containing multiple files that each is an xml-file with a single record
     *  
     * @param  recordBase The recordBase for collection documents. The recordBase must be defined in ds-storage. 
     * @param is Inputstream. Must be a zip-file containing single files that each is an XML record.
     * @return Number of successfully ingested records.
     */    
    public static Integer ingestFromZipfile(String recordBase, InputStream is) throws Exception {
        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(is));

        DsStorageApi dsAPI = getDsStorageApiClient();
        int recordsIngested=0;

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
                
                String recordId= recordBase+":"+identifier;
                log.info("Ingesting record filename from zip:"+fileName +" id:"+identifier);
                DsRecordDto dsRecord = new DsRecordDto();
                dsRecord.setId(recordId); 
                dsRecord.setBase(recordBase);
                dsRecord.setData(record_string);                                                 
                dsAPI.recordPost(dsRecord);  
                recordsIngested++;                                                
            }
            catch(Exception e) {
                log.error("Error parsing xml record for file:"+fileName, e);              
            }
        }

        IOUtils.closeQuietly(zis);        
        return recordsIngested;         
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
            throw new InvalidArgumentServiceException("No target found in configuration with name:'"+oaiTargetName +"' . See the config method for list of configured targets.");            
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

        String recordBase=oaiTargetDto.getRecordBase();

        DsStorageApi dsAPI = getDsStorageApiClient();        
        OaiHarvestClient client = new OaiHarvestClient(job,from);
        OaiResponse response = client.next();
        int totalRecordLoaded=0;
        while (response.getRecords().size() >0) {

            for (OaiRecord  oaiRecord : response.getRecords()) {                
                totalRecordLoaded++;
                String storageId=recordBase+":"+oaiRecord.getId();
                if (oaiRecord.isDeleted()) { //mark for delete
                    dsAPI.markRecordForDelete(storageId);  
                }
                else { //Create or update                
                    DsRecordDto dsRecord = new DsRecordDto();
                    dsRecord.setId(storageId);
                    dsRecord.setBase(recordBase);
                    dsRecord.setData(oaiRecord.getMetadata());                                          
                    dsAPI.recordPost(dsRecord);
                }
            }
            log.info("Ingesting base:"+recordBase + " records:"+totalRecordLoaded +" out of a total of "+response.getTotalRecords());            

            //Update timestamp with timestamp from last OAI record.
            OaiRecord lastRecord = response.getRecords().get(response.getRecords().size()-1);                        
            HarvestTimeUtil.updateDatestampForOaiTarget(oaiTargetDto,lastRecord.getDateStamp());

            response = client.next(); //load next (may be empty)            
        }

        if (response.isError()) {
            throw new InternalServiceException("Error during harvest for target:"+job.getDto().getName() +" after harvesting "+totalRecordLoaded +" records");            
        }

        log.info("Completed ingesting base successfully:"+recordBase+ " records:"+totalRecordLoaded);        
        return totalRecordLoaded;




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
        ApiClient apiClient = new ApiClient();
        apiClient.setHost(ServiceConfig.getDsHost());
        apiClient.setPort(ServiceConfig.getDsPort());
        apiClient.setBasePath(ServiceConfig.getDsBasePath());
        DsStorageApi dsAPI = new DsStorageApi(apiClient);
        return dsAPI;
    }

    private static synchronized void validateNotAlreadyRunning(String oaiTargetName) {
        boolean alreadyRunning= OaiJobCache.isJobRunningForTarget(oaiTargetName);        
        if (alreadyRunning) {
            throw new InvalidArgumentServiceException("There is already a job running for target:"+oaiTargetName);
        }
    }

}
