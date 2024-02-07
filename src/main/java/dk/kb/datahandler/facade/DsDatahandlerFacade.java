package dk.kb.datahandler.facade;

import dk.kb.datahandler.config.ServiceConfig;
import dk.kb.datahandler.model.v1.OaiJobDto;
import dk.kb.datahandler.model.v1.OaiTargetDto;
import dk.kb.datahandler.oai.*;
import dk.kb.datahandler.util.HarvestTimeUtil;
import dk.kb.datahandler.util.SolrUtils;
import dk.kb.storage.client.v1.DsStorageApi;
import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.storage.model.v1.OriginCountDto;
import dk.kb.storage.util.DsStorageClient;
import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.webservice.exception.InvalidArgumentServiceException;
import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


public class DsDatahandlerFacade {

    private static final Logger log = LoggerFactory.getLogger(DsDatahandlerFacade.class);
    
    private static DsStorageClient storageClient;
    
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
        ArrayList<String> errorRecords= new ArrayList<>();

        ZipEntry entry;

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
                log.info("Ingesting record filename from zip: '{}' and id: '{}'", fileName, recordId);
                DsRecordDto dsRecord = new DsRecordDto();
                dsRecord.setId(recordId); 
                dsRecord.setOrigin(origin);
                dsRecord.setData(record_string);                                                 
                dsAPI.recordPost(dsRecord);  
                                                                
            }
            catch(Exception e) {
                errorRecords.add(fileName);
                log.error("Error parsing xml record for file: '{}'", fileName, e);
            }
        }

        IOUtils.closeQuietly(zis);        
        return errorRecords;         
    }

    
    /**  
     *  Will start a index flow of records from ds-storage into solr. 
     * <p>
     *  1) Call ds-present that will extract records from ds-storage and xslt transform them into solr-add documents json.
     *  2) Send the input stream with json documents directly to solr so it is not kept in memory.
     *  
     * @param origin Origin must be defined on the ds-present server.
     * @param mTimeFrom Will only index records with a last modification time (mTime) after this value. 
     * @exception InternalServiceException Will throw exception is the dsPresentCollectionName is not known, or if server communication fails.
     */    
    @SuppressWarnings("unchecked")
    public static String indexSolrFull(String origin, Long mTimeFrom)  throws Exception{
   
        if (mTimeFrom==null) {
            mTimeFrom=0L;
        }

        return SolrUtils.indexOrigin(origin, mTimeFrom);
    }

    /**
     * Indexes ds-storage records that haven't been indexed in solr through the following workflow:
     * Get latest  ds-storage modification time for records in existing solr index.
     * Then fetch newer records from ds-storage, transform to solr documents in ds-present and index into solr.
     * @param origin to index records from.
     */
    public static String indexSolrDelta(String origin) throws IOException, SolrServerException, URISyntaxException {
        Long lastStorageMTime = SolrUtils.getLatestMTimeForOrigin(origin);

        return SolrUtils.indexOrigin(origin, lastStorageMTime);
    }

    /**
     * Starts a delta OAI harvest job for the target. The job will continue from last timestamp
     * saved on the file system for that target.
     * If OAI strategy for the target is dayOnly, the harvest process will be split into days instead of a single job.
     * The job will harvest records from the OAI server and ingest them into DS-storage  
     *  
     * @param  oaiTargetName the location of the image, relative to the url argument
     * @return Number of harvested records.
     */    
    public static Integer oaiIngestDelta(String oaiTargetName) throws Exception {   
        OaiTargetDto oaiTargetDto = ServiceConfig.getOaiTargets().get(oaiTargetName);       
    	if (oaiTargetDto.getDayOnly()) {
    		Integer delta=oaiIngestDeltaImplByDay(oaiTargetName); 
    		return delta;    		
    	}
    	else {
    		Integer delta = oaiIngestDeltaImpl(oaiTargetName);	
    		return delta;
    	}    	
    }
    
    
    public static Integer oaiIngestDeltaImplByDay(String oaiTargetName) throws Exception {                

        //Test no job is running before starting new for same target
        validateNotAlreadyRunning(oaiTargetName);        
        OaiTargetDto oaiTargetDto = ServiceConfig.getOaiTargets().get(oaiTargetName);                
        if (oaiTargetDto== null) {
            throw new InvalidArgumentServiceException("No target found in configuration with name:'" + oaiTargetName +
                    "' . See the config method for list of configured targets.");
        }

        OaiTargetJob job = createNewJob(oaiTargetDto);        

        //register job
        OaiJobCache.addNewJob(job);
        
        //Notice we cut date down to day here always. 2023-10-25T12:51:58Z -> 2023-10-25
        String dayStamp = HarvestTimeUtil.loadLastHarvestTime(oaiTargetDto);
        if (dayStamp== null) {
        	dayStamp=oaiTargetDto.getStartDay();        
        }

        //Now we have start date and we have to batch to every day from dayStamp until tomorrow
        //Every day is also be batched with resumptionToken as standard OAI
        
        String nextDay=HarvestTimeUtil.getNextDayIfNot2DaysInFuture(dayStamp);
        
        int totalNumber=0;
        while (nextDay != null) {        	 

        	try {                       
                int number= oaiIngest(job , dayStamp,nextDay);
                OaiJobCache.finishJob(job, number,false);//No error
                totalNumber+=number;
            }
            catch(Exception e) {
                log.error("Oai delta harvest did not complete succesfull: '{}'", oaiTargetName);
                job.setCompletedTime(System.currentTimeMillis());
                OaiJobCache.finishJob(job, 0,true);//Error                        
                throw new Exception(e);
            }
            
           //Add 1 day to from and to day
        	dayStamp=nextDay;
        	nextDay=HarvestTimeUtil.getNextDayIfNot2DaysInFuture(nextDay); //when null it will stop
        	
        }
        return totalNumber;
    }

    
   
    public static Integer oaiIngestDeltaImpl(String oaiTargetName) throws Exception {                

        //Test no job is running before starting new for same target
        validateNotAlreadyRunning(oaiTargetName);        
        OaiTargetDto oaiTargetDto = ServiceConfig.getOaiTargets().get(oaiTargetName);                
        if (oaiTargetDto== null) {
            throw new InvalidArgumentServiceException("No target found in configuration with name:'" + oaiTargetName +
                    "' . See the config method for list of configured targets.");
        }

        OaiTargetJob job = createNewJob(oaiTargetDto);        

        //register job
        OaiJobCache.addNewJob(job);

        try {
            String datestamp = HarvestTimeUtil.loadLastHarvestTime(oaiTargetDto);        
            int number= oaiIngest(job , datestamp,null);
            OaiJobCache.finishJob(job, number,false);//No error
            return number;
        }
        catch(Exception e) {
            log.error("Oai delta harvest did not complete succesfull: '{}'", oaiTargetName);
            job.setCompletedTime(System.currentTimeMillis());
            OaiJobCache.finishJob(job, 0,true);//Error                        
            throw new Exception(e);
        }
    }

    /**
     * Starts a full OAI harvest job for the target.
     * The job will harvest records from the OAI server and ingest them into DS-storage  
     * If OAI strategy for the target is dayOnly, the harvest process will be split into days instead of a single job.
     * After full ingest ds-storage will be called again and all older recorders from before the ingest fill be deleted. 
     *  
     * @param  oaiTargetName the location of the image, relative to the url argument
     * @return Number of harvested records.
     */        
    public static Integer oaiIngestFull(String oaiTargetName) throws Exception {
    	
        OaiTargetDto oaiTargetDto = ServiceConfig.getOaiTargets().get(oaiTargetName);       
       	if (oaiTargetDto.getDayOnly()) {
       		Integer delta= oaiIngestDeltaImplByDay(oaiTargetName); //even if full, it will be split into many delta's
       		return delta;    		
       	}
       	else {
       		Integer delta = oaiIngestFull(oaiTargetName);	
       		return delta;
       	}    	
    	
    	
    }
   
    public static Integer oaiIngestFullImpl(String oaiTargetName) throws Exception {

    	
        //Test no job is running before starting new for same target
        validateNotAlreadyRunning(oaiTargetName);

        OaiTargetDto oaiTargetDto = ServiceConfig.getOaiTargets().get(oaiTargetName);   
        if (oaiTargetDto== null) {
            throw new InvalidArgumentServiceException("No target found in configuration with name:'"+oaiTargetName +
                    "' . See the config method for list of configured targets.");
        }

        OaiTargetJob job = createNewJob(oaiTargetDto);
        //Get last modified record from ds-storage. After the full Ingest, all records earlier that this (included) will be deleted from ds-storage
        DsStorageApi dsAPI = getDsStorageApiClient(); 
    	List<OriginCountDto> originStatistics = dsAPI.getOriginStatistics();

    	
     	long lastModifiedForOrigin= getLastModifiedTimeForOrigin(originStatistics, oaiTargetDto.getDatasource());
        
        //register job
        OaiJobCache.addNewJob(job);            

        try {

            int number= oaiIngest(job , null,null);        
            OaiJobCache.finishJob(job, number,false);//No error

            //Delete old records in storage from before.
            Integer numberDeleted = dsAPI.deleteRecordsForOrigin(oaiTargetDto.getDatasource(), 0L, lastModifiedForOrigin);
            log.info("After full ingest for origin={}, deleted {} old records in storage",
                    oaiTargetDto.getDatasource(), numberDeleted);
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
    public static List<OaiJobDto> getJobs() {
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
    protected static Integer oaiIngest(OaiTargetJob job, String from, String until) throws Exception {

        //In the OAI spec, the from parameter can be both yyyy-MM-dd or full UTC timestamp (2021-10-09T09:42:03Z)        
        //But COP only supports the short version. So when this is called use short format
        //Dirty but quick solution fix. Best would be if COP could fix it

        OaiTargetDto oaiTargetDto = job.getDto();

        if (from != null && oaiTargetDto.getUrl().indexOf("kb.dk/cop/")> 0) {
            from = from.substring(0,10);               
        }

        // TODO: Change this to datasource in the OpenAPI specification
        String origin=oaiTargetDto.getDatasource();
        String targetName = oaiTargetDto.getName();

        DsStorageClient dsAPI = getDsStorageApiClient();
        OaiHarvestClient client = new OaiHarvestClient(job,from,until);
        OaiResponse response = client.next();

        OaiResponseFilter oaiFilter;
        if (oaiTargetDto.getFilter() == null) {
            throw new IllegalStateException("The filter for OaiTargetDto '" + targetName + "' was null");
        }
        switch (oaiTargetDto.getFilter()) {
            case DIRECT:
                oaiFilter = new OaiResponseFilter(origin, dsAPI);
                break;
            case PRESERVICA:
                oaiFilter = new OaiResponseFilterPreservica(origin, dsAPI);
                break;
            default: throw new UnsupportedOperationException(
                    "Unknown filter '" + oaiTargetDto.getFilter() + "' for target '" + targetName + "'");
        }

        while (response.getRecords().size() >0) {

            OaiRecord lastRecord = response.getRecords().get(response.getRecords().size()-1);

            oaiFilter.addToStorage(response);

            log.info("Ingested '{}' records from origin: '{}' out of a total of '{}' records.",
                    oaiFilter.getProcessed(), origin, response.getTotalRecords());

            //Update timestamp with timestamp from last OAI record.
            HarvestTimeUtil.updateDatestampForOaiTarget(oaiTargetDto,lastRecord.getDateStamp());

            response = client.next(); //load next (may be empty)            
        }

        if (response.isError()) {
            throw new InternalServiceException("Error during harvest for target:" + job.getDto().getName() +
                    " after harvesting " + oaiFilter.getProcessed() + " records");
        }

        log.info("Completed ingesting origin '{}' successfully with {} records", origin, oaiFilter.getProcessed());
        return oaiFilter.getProcessed();
    }

    /**
     * Generates a {@link OaiTargetJob} from a {@link OaiTargetDto}.
     * <p>
     * The job will have a unique timestamp used as ID.  
     *   
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

    private static DsStorageClient getDsStorageApiClient() {
        if (storageClient!= null) {
          return storageClient;
        }
          
        String dsLicenseUrl = ServiceConfig.getDsStorageUrl();                                
        storageClient = new DsStorageClient(dsLicenseUrl);               
        return storageClient;
    }

    private static long getLastModifiedTimeForOrigin(List<OriginCountDto> originStatistics, String origin) {
    	for (OriginCountDto dto :originStatistics) {
    		if (dto.getOrigin() != null && dto.getOrigin().equals(origin)) {
    			return dto.getLatestMTime() == null ? 0L : dto.getLatestMTime();
    		}
    	}

    	//Can happen if there is no records in the origin
    	log.warn("Origin name was not found in origin-statistics returned from ds-storage. " +
                "Using mTime=0 for Origin: '{}'", origin);
    	return 0L;
    	
    	
    }

    private static synchronized void validateNotAlreadyRunning(String oaiTargetName) {
        boolean alreadyRunning= OaiJobCache.isJobRunningForTarget(oaiTargetName);        
        if (alreadyRunning) {
            throw new InvalidArgumentServiceException("There is already a job running for target:"+oaiTargetName);
        }
    }

}
