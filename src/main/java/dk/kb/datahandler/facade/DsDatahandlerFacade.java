package dk.kb.datahandler.facade;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import dk.kb.datahandler.oai.OaiResponseFilterPreservicaFive;
import dk.kb.datahandler.oai.OaiResponseFilterPreservicaSeven;
import dk.kb.datahandler.oai.plugins.AllowedPlugins;
import dk.kb.datahandler.oai.plugins.Plugin;
import dk.kb.datahandler.oai.plugins.PreservicaManifestationPlugin;
import dk.kb.datahandler.util.LoggingUtils;
import dk.kb.datahandler.util.PreservicaUtils;
import dk.kb.storage.invoker.v1.ApiException;
import dk.kb.storage.model.v1.RecordTypeDto;
import dk.kb.util.webservice.stream.ContinuationInputStream;
import dk.kb.util.webservice.stream.ContinuationStream;
import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import dk.kb.datahandler.config.ServiceConfig;
import dk.kb.datahandler.model.v1.OaiJobDto;
import dk.kb.datahandler.model.v1.OaiTargetDto;
import dk.kb.datahandler.oai.OaiFromUntilInterval;
import dk.kb.datahandler.oai.OaiHarvestClient;
import dk.kb.datahandler.oai.OaiJobCache;
import dk.kb.datahandler.oai.OaiRecord;
import dk.kb.datahandler.oai.OaiResponse;
import dk.kb.datahandler.oai.OaiResponseFilter;
import dk.kb.datahandler.oai.OaiTargetJob;
import dk.kb.datahandler.util.HarvestTimeUtil;
import dk.kb.datahandler.util.SolrUtils;
import dk.kb.storage.client.v1.DsStorageApi;
import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.storage.model.v1.OriginCountDto;
import dk.kb.storage.util.DsStorageClient;
import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.webservice.exception.InvalidArgumentServiceException;

import static java.lang.Thread.sleep;


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
     * Starts a full OAI harvest job for the target.
     * The job will harvest records from the OAI server and ingest them into DS-storage  
     * If OAI strategy for the target is dayOnly, the harvest process will be split into days instead of a single job. 
     *  
     * @param oaiTargetName the location of the image, relative to the url argument
     * @param plugins A list of plugins that can be configured to run during ingest from the OAI target.
     * @return Number of harvested records.
     */        
    public static Integer oaiIngestFull(String oaiTargetName, List<String> plugins) throws Exception {
        OaiTargetDto oaiTargetDto = ServiceConfig.getOaiTargets().get(oaiTargetName);       
        
        //Will be 1 interval for OAI targets that does not need to split into days
        ArrayList<OaiFromUntilInterval> intervals = HarvestTimeUtil.generateFromUntilInterval(oaiTargetDto, null); // from == null, use default start day for OAI target instead
        Integer totalHarvested = oaiIngestJobScheduler(oaiTargetName, intervals, plugins);
        log.info("Full ingest of target={} completed with records={}", oaiTargetName, totalHarvested);
        return totalHarvested;            
    }
   
    
    /**
     * Starts a delta OAI harvest job for the target. The job will continue from last timestamp
     * saved on the file system for that target.
     * If OAI strategy for the target is dayOnly, the harvest process will be split into days instead of a single job.
     * The job will harvest records from the OAI server and ingest them into DS-storage  
     *  
     * @param oaiTargetName The name for the OAI target in the configuration
     * @param plugins A list of plugins that can be configured to run during ingest from the OAI target.
     * @return Number of harvested records.
     */
    public static Integer oaiIngestDelta(String oaiTargetName, List<String> plugins) throws Exception {
        OaiTargetDto oaiTargetDto = ServiceConfig.getOaiTargets().get(oaiTargetName);       
        String lastHarvestTime = HarvestTimeUtil.loadLastHarvestTime(oaiTargetDto);

        //Will be 1 interval for OAI targets that does not need to split into days
        ArrayList<OaiFromUntilInterval> intervals= HarvestTimeUtil.generateFromUntilInterval(oaiTargetDto, lastHarvestTime);
        Integer totalHarvested = oaiIngestJobScheduler(oaiTargetName, intervals, plugins);
        log.info("Delta ingest of target={} completed with records={}", oaiTargetName, totalHarvested);
        return totalHarvested;    	    	
    }
    
  
        
   

    
    /**
     * This method has no specific code for the different OAI targets. Dateformats must be set correct for the target when calling this method. <br>
     * The list of date-intervals must be ascending in time<br> 
     * The date intervals will be harvested in same order as in list. After each interval harvest they persistent last harvesttime will be updated for that OAI target.
     *  <p/>
     * For each interval this method will start a new OAI job and call {@link #oaiIngestPerform(OaiTargetJob, String, String, List)}-method}<br>
     *  
     * @param oaiTargetName the name of the configured oai-target
     * @param fromUntilList List of date intervals. When calling this method the date formats must be in format accepted by the target.
     * @param plugins A list of plugins that can be configured to run during ingest from the OAI target.
     * @return Total number of records harvest from all intervals. Records that are discarded will not be counted.
     *
     */
     protected static Integer oaiIngestJobScheduler(String oaiTargetName,ArrayList<OaiFromUntilInterval> fromUntilList, List<String> plugins) throws Exception {
         int totalNumber=0;
                  
         log.info("Starting jobs from number of FromUntilIntervals:"+fromUntilList.size() +" for target:"+oaiTargetName);
         for (OaiFromUntilInterval fromUntil: fromUntilList) {         
             //Test no job is running before starting new for same target
             validateNotAlreadyRunning(oaiTargetName);  //If we want to multithread preservica harvest, this has to be removed      
             OaiTargetDto oaiTargetDto = ServiceConfig.getOaiTargets().get(oaiTargetName);                
             if (oaiTargetDto== null) {
                 throw new InvalidArgumentServiceException("No target found in configuration with name:'" + oaiTargetName +
                    "' . See the config method for list of configured targets.");
             }

             OaiTargetJob job = createNewJob(oaiTargetDto);        

             //register job
             OaiJobCache.addNewJob(job);
              
             try {                       
                int number= oaiIngestPerform(job , fromUntil.getFrom(),fromUntil.getUntil(), plugins);
                OaiJobCache.finishJob(job, number,false);//No error
                totalNumber+=number;
             }
             catch(Exception e) {
                log.error("Oai delta harvest did not complete succesfull: '{}'", oaiTargetName);
                job.setCompletedTime(System.currentTimeMillis());
                OaiJobCache.finishJob(job, 0,true);//Error                        
                throw new Exception(e);
             }            
         }
        return totalNumber;
    }

    
      

    

    /**
     *  Gives a ist of both completed and running jobs with status. Jobs still running will be first.
     *  The completed jobs will only contain last 10000 completed jobs
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
     * This method will be called by the {@link #oaiIngestJobScheduler(String, ArrayList, List)}-method}<br>
     * The scheduler method will setup the job and responsible for status of the job. <br>
     * The target will be harvest full for this interval using the resumptionToken from the response and call recursively.<br>
     * For each successful response the persistent datestamp for the OAI target will be updated with datestamp from last parsed records.<br>
     *      
     * @param job The configured OAI target
     * @param from Datestamp format that will be accepted for that OAI target
     * @param until Datestamp format that will be accepted for that OAI target
     * @param plugins A list of plugins that can be configured to run during ingest from the OAI target.
     * @return Number of harvested records for this date interval. Records discarded by filter etc. will not be counted.
     * @throws Exception If anything expected happens. OAI target does not respond, invalid xml, XSTL (filtering) failed etc.  
     */
     private static Integer oaiIngestPerform(OaiTargetJob job, String from, String until, List<String> plugins) throws Exception {

        //In the OAI spec, the from parameter can be both yyyy-MM-dd or full UTC timestamp (2021-10-09T09:42:03Z)               
        //But COP only supports the short version. So when this is called use short format
        //Preservica seems to only accept full UTC format
        //Dirty but quick solution fix. Best would be if COP could fix it

        OaiTargetDto oaiTargetDto = job.getDto();

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
                oaiFilter = new OaiResponseFilterPreservicaSeven(origin, dsAPI);
                break;
            case PRESERVICA5:
                oaiFilter = new OaiResponseFilterPreservicaFive(origin, dsAPI);
                break;
            default: throw new UnsupportedOperationException(
                    "Unknown filter '" + oaiTargetDto.getFilter() + "' for target '" + targetName + "'");
        }

        while (response.getRecords().size() >0) {

            OaiRecord lastRecord = response.getRecords().get(response.getRecords().size()-1);

            handlePlugins(plugins, oaiFilter);
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
     * Analyse which plugins have been selected and add correctly specified plugins to the OAI filter in use.
     * @param plugins list of plugins that are to be added to the OAI-PMH workflow.
     * @param oaiFilter the filter used to handle the OAI-PMH response.
     */
    private static void handlePlugins(List<String> plugins, OaiResponseFilter oaiFilter) throws IOException {
        if (plugins.contains(AllowedPlugins.FETCH_MANIFESTATION.getName())) {
            Plugin preservicaManifestationPlugin = new PreservicaManifestationPlugin();
            oaiFilter.addPlugin(preservicaManifestationPlugin);
        }
        /* EXAMPLE OF IMPLEMENTING A SPEECH-TO-TEXT PLUGIN
        if (plugins.contains(AllowedPlugins.FETCH_MANIFESTATION.getName())){
            Plugin aiPlugin = new SpeechToTextPlugin();
            oaiFilter.addPlugin();
        }*/
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
            sleep(1); // So next ID is different.
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

    /**
     * Method to update records in Preservica 7 related origins in backing {@code DsStorage} with children IDs.
     * The method filters incoming records on IDs representing InformationObjects from Preservica 7, then tries to fetch
     * a manifestation for the record and updates the storage record with the manifestation as a childrenID.
     * @param origin to update records in.
     * @param mTimeFrom to update records from.
     * @return a count of records that have been updated.
     */
    public static long updateManifestationForRecords(String origin, Long mTimeFrom) throws InterruptedException, IOException {
        long processedRecords= 0L;
        log.info("STARTED MANIFESTATION PLUGIN");
        LoggingUtils.writeToFile("STARTED MANIFESTATION PLUGIN", "src/main/resources/manifestationTimes.txt");
        try {
            DsStorageClient storageClient = new DsStorageClient(ServiceConfig.getDsStorageUrl());
            Plugin manifestationPlugin = new PreservicaManifestationPlugin();


           /* List<DsRecordDto> listOfRecords = storageClient.getRecordsModifiedAfter(origin, RecordTypeDto.DELIVERABLEUNIT, mTimeFrom, -1L);
            long currentTime = System.currentTimeMillis();
            int count = 0;
            for (DsRecordDto record : listOfRecords) {
                count += 1;
                if (count % 10 == 0){
                    log.info("10 Records have been updated in '{}' milliseconds.", currentTime - System.currentTimeMillis());
                    currentTime = System.currentTimeMillis();
                }
                manifestationPlugin.apply(record);

                if (record.getChildrenIds() != null && !record.getChildrenIds().isEmpty()){
                    log.info("Posting record with id: '{}'", record.getId());
                    log.warn("Record has childrenIds '{}'", record.getChildrenIds());
                    storageClient.recordPost(record);
                    //PreservicaUtils.safeRecordPost(storageClient, record);
                }
            }*/

            AtomicInteger counter = new AtomicInteger(0);
            AtomicLong currentTime = new AtomicLong(System.currentTimeMillis());
            long maxRecords = 0;

            List<OriginCountDto> stats = storageClient.getOriginStatistics();
            for (OriginCountDto originCount : stats) {
                if (originCount.getOrigin().equals(origin)){
                    maxRecords = originCount.getCount();
                }
            }

            ContinuationStream<DsRecordDto, Long> recordStream = storageClient.getRecordsModifiedAfterStream(origin, mTimeFrom, maxRecords);
            recordStream
                    .parallel() // Parallelize stream for performance boost.
                    /*.map(DsDatahandlerFacade::logContent)
                    .filter(PreservicaUtils::isInformationObject)
                    .filter(PreservicaUtils::needsChildrenIds)*/
                    .map(record -> PreservicaUtils.fetchManifestation(record, manifestationPlugin))
                    .forEach(record -> PreservicaUtils.safeRecordPost(storageClient, record, counter, currentTime));
        } catch (IOException e) {
            log.warn("Sleeping 20 seconds. Caught IOException: ", e);
            sleep(20000);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }

        log.info("FINISHED MANIFESTATION PLUGIN");
        LoggingUtils.writeToFile("FINISHED MANIFESTATION PLUGIN", "src/main/resources/manifestationTimes.txt");
        return processedRecords;
    }

    private static DsRecordDto logContent(DsRecordDto record) {
        log.debug("Streaming record with id: '{}'", record.getId());
        return record;
    }

    private static DsRecordDto counter(DsRecordDto record, AtomicInteger count) {
        count.incrementAndGet();
        log.debug("Count is '{}'", count);
        return record;
    }
}
