package dk.kb.datahandler.facade;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import dk.kb.datahandler.enrichment.DataEnricher;
import dk.kb.datahandler.oai.OaiResponseFilterDrArchive;
import dk.kb.datahandler.oai.OaiResponseFilterPreservicaSeven;
import dk.kb.datahandler.preservica.PreservicaManifestationExtractor;
import dk.kb.datahandler.preservica.client.DsPreservicaClient;
import dk.kb.datahandler.util.PreservicaUtils;
import dk.kb.storage.invoker.v1.ApiException;
import dk.kb.storage.model.v1.DsRecordMinimalDto;
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
import dk.kb.datahandler.oai.OaiHarvestClient;
import dk.kb.datahandler.oai.OaiJobCache;
import dk.kb.datahandler.oai.OaiRecord;
import dk.kb.datahandler.oai.OaiResponse;
import dk.kb.datahandler.oai.OaiResponseFilter;
import dk.kb.datahandler.oai.OaiTargetJob;
import dk.kb.datahandler.util.HarvestTimeUtil;
import dk.kb.datahandler.util.SolrUtils;
import dk.kb.kaltura.client.DsKalturaClient;
import dk.kb.storage.client.v1.DsStorageApi;
import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.storage.model.v1.MappingDto;
import dk.kb.storage.model.v1.OriginCountDto;
import dk.kb.storage.util.DsStorageClient;
import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.webservice.exception.InvalidArgumentServiceException;

import static java.lang.Thread.sleep;


public class DsDatahandlerFacade {
    private static final Logger log = LoggerFactory.getLogger(DsDatahandlerFacade.class);

    private static DsStorageClient storageClient;

    /**
     * <p>
     * Will load all referenceId defined in storage and call Kaltura to map them to KalturaId.
     * The results will be saved in ds-storage in the mapping table.
     * When all mappings has been updated, the records will be enriched with the kalturaId.
     * If the job fails before all mappingss are loaded, the records will not be enriched from the mapping table.
     * </p>
     * 
     * @param origin Only update mappings from this origin
     * @param mTimeFrom Only update mappings for records with mTime after mTimeFrom 
     * 
     * @return Number of mappings updated.
     * 
     * 
     */
    public static long fetchKalturaIdsAndUpdateRecords(String origin,Long mTimeFrom) throws Exception{
        if (mTimeFrom== null) {
            mTimeFrom=0L;
        }

        long start=System.currentTimeMillis();
        int batchSize=100; //No need to take this as input parameter and make method more complicated. 
        //This is a good value and can also be used as batchSize against Kaltura.

        DsStorageClient dsAPI = getDsStorageApiClient();
        DsKalturaClient kalturaClient = getKalturaClient();

        long updated=0;
        List<DsRecordMinimalDto> records= new ArrayList<DsRecordMinimalDto>();
        while(true) {       
            records = dsAPI.getMinimalRecords(origin, batchSize,mTimeFrom);
            if (records.size()==0) { //no more records
                break;
            }
            mTimeFrom=records.get(records.size()-1).getmTime(); //update mTime to mTime from last record.
            log.debug("Getting DsRecordReference from storage for origin={},batchSize={},mTimeFrom={}",origin,batchSize,mTimeFrom);
            
            ArrayList<String> referenceIdsList= new ArrayList<String>();
            for (DsRecordMinimalDto record: records) {
                if (record.getReferenceId() != null) { //This should not be null in production after preservica enrichment. But for stage/prod most will be null
                    referenceIdsList.add(record.getReferenceId());
                }
            }
            if (referenceIdsList.size()==0) { // Should not happen in production.
                continue;
            }

            log.debug("Calling Kaltura to resolve kalturaId for referenceIds. Size:"+referenceIdsList.size());
            Map<String, String> kalturaIds = kalturaClient.getKulturaIds(referenceIdsList);

            if (kalturaIds.size() != referenceIdsList.size()) {
                log.warn("Not all referenceId was found at Kaltura"); //Should not happen
            }
            updated+=kalturaIds.size();      
            
            for (String referenceId: kalturaIds.keySet()) {                            
                //Can be optimized with method that takes multiple. But this workflow is called rarely.             
                MappingDto mappingDto= new MappingDto();
                mappingDto.setReferenceId(referenceId);
                mappingDto.setKalturaId(kalturaIds.get(referenceId));
                dsAPI.mappingPost(mappingDto);
            }
        }        

        //Mapping table is now updated. Enrich all records that does not have KalturaId
        dsAPI.updateKalturaIdForRecords();//Will update all records with kalturaid using the mapping tab
        log.info("Updated kalturaId mapping table and enriched records. Number of mapppings updated:"+updated +" millis:"+(System.currentTimeMillis()-start));
        return updated;
    }


    /**
     * Ingest records directly into ds-storage from a zip-file containing multiple files that each is a xml-file with a single record
     *  
     * @param  origin The origin for collection documents. The origin must be defined in ds-storage. 
     * @param is InputStream. Must be a zip-file containing single files that each is an XML record.
     * @return List of strings containing the records that failed parsing.
     */    
    public static ArrayList<String> ingestFromZipfile(String origin, InputStream is) {
        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(is));

        DsStorageApi dsAPI = getDsStorageApiClient();
        ArrayList<String> errorRecords= new ArrayList<>();

        ZipEntry entry;
        String fileName = "";

        try {

            while ((entry = zis.getNextEntry()) != null) {

                fileName = entry.getName();

                String recordString = IOUtils.toString(zis, StandardCharsets.UTF_8);
                Document record = OaiHarvestClient.sanitizeXml(recordString, null);

                //There are several 'mods:identifier' identifier tags, but the first always contains the URI.
                Node item = record.getElementsByTagName("mods:identifier").item(0);
                String identifier = item.getTextContent();

                //Example: urn:uuid:096c9090-717f-11e0-82d7-002185371280
                identifier = identifier.replaceFirst("urn:uuid:", ""); // Clear this first part from the ID

                String recordId = origin + ":" + identifier;
                log.info("Ingesting record filename from zip: '{}' and id: '{}'", fileName, recordId);
                DsRecordDto dsRecord = new DsRecordDto();
                dsRecord.setId(recordId);
                dsRecord.setOrigin(origin);
                dsRecord.setData(recordString);
                dsAPI.recordPost(dsRecord);
            }
        } catch (IOException e) {
            errorRecords.add(fileName);
            log.error("Error parsing xml record for file: '{}'", fileName, e);
        } catch (ApiException e){
            errorRecords.add(fileName);
            log.error("Error posting record with filename: '{}' to DsStorage.", fileName, e);
        }

        IOUtils.closeQuietly(zis);        
        return errorRecords;         
    }

    /**  
     *  Will start an index flow of records from ds-storage into solr.
     * <p>
     *  1) Call ds-present that will extract records from ds-storage and xslt transform them into solr-add documents json.
     *  2) Send the input stream with json documents directly to solr, so it is not kept in memory.
     *  
     * @param origin Origin must be defined on the ds-present server.
     * @param mTimeFrom Will only index records with a last modification time (mTime) after this value. 
     * @exception InternalServiceException Will throw exception is the dsPresentCollectionName is not known, or if server communication fails.
     */    
    public static String indexSolrFull(String origin, Long mTimeFrom) {

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
    public static String indexSolrDelta(String origin) throws IOException, SolrServerException {
        Long lastStorageMTime = SolrUtils.getLatestMTimeForOrigin(origin);

        return SolrUtils.indexOrigin(origin, lastStorageMTime);
    }

    /**
     * Starts a full OAI harvest job for the target.
     * The job will harvest records from the OAI server and ingest them into DS-storage  
     * If OAI strategy for the target is dayOnly, the harvest process will be split into days instead of a single job. 
     *  
     * @param oaiTargetName the location of the image, relative to the url argument
     * @return Number of harvested records.
     */        
    public static Integer oaiIngestFull(String oaiTargetName) throws Exception {
        OaiTargetDto oaiTargetDto = ServiceConfig.getOaiTargets().get(oaiTargetName);       


        String from= HarvestTimeUtil.generateFrom(oaiTargetDto, null); // from == null, use default start day for OAI target instead
        Integer totalHarvested = oaiIngestJobScheduler(oaiTargetName,from);
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
     * @return Number of harvested records.
     */
    public static Integer oaiIngestDelta(String oaiTargetName) throws Exception {
        OaiTargetDto oaiTargetDto = ServiceConfig.getOaiTargets().get(oaiTargetName);       
        String lastHarvestTime = HarvestTimeUtil.loadLastHarvestTime(oaiTargetDto);

        //Will be 1 interval for OAI targets that does not need to split into days
        String from= HarvestTimeUtil.generateFrom(oaiTargetDto, lastHarvestTime);
        Integer totalHarvested = oaiIngestJobScheduler(oaiTargetName, from);
        log.info("Delta ingest of target={} completed with records={}", oaiTargetName, totalHarvested);
        return totalHarvested;    	    	
    }

    /**
     * This method has no specific code for the different OAI targets. Date formats must be set correct for the target when calling this method. <br>
     * The list of date-intervals must be ascending in time<br> 
     * The date intervals will be harvested in same order as in list. After each interval harvested they persistent last harvest time will be updated for that OAI target.
     *  <p/>
     * For each interval this method will start a new OAI job and call {@link #oaiIngestPerform(OaiTargetJob, String, String)}-method}<br>
     *  
     * @param oaiTargetName the name of the configured oai-target
     * @param fromUntilList List of date intervals. When calling this method the date formats must be in format accepted by the target.
     * @return Total number of records harvest from all intervals. Records that are discarded will not be counted.
     *
     */
    protected static Integer oaiIngestJobScheduler(String oaiTargetName, String from) throws Exception {
        int totalNumber=0;

        log.info("Starting jobs from: "+from +" for target:"+oaiTargetName);
                 
            //Test no job is running before starting new for same target
            validateNotAlreadyRunning(oaiTargetName);  //If we want to multithread preservica harvest, this has to be removed      
            OaiTargetDto oaiTargetDto = ServiceConfig.getOaiTargets().get(oaiTargetName);                
            if (oaiTargetDto== null) {
                throw new InvalidArgumentServiceException("No target found in configuration with name: '" + oaiTargetName +
                        "'. See the config method for list of configured targets.");
            }

            OaiTargetJob job = createNewJob(oaiTargetDto);        

            //register job
            OaiJobCache.addNewJob(job);

            try {                       
                int number= oaiIngestPerform(job , from);
                OaiJobCache.finishJob(job, number,false);//No error
                totalNumber+=number;
            }
            catch (IOException | ApiException e) {
                log.error("Oai harvest did not complete successfully for target: '{}'", oaiTargetName);
                job.setCompletedTime(System.currentTimeMillis());
                OaiJobCache.finishJob(job, 0,true);//Error                        
                throw new Exception(e);
            }
        
        return totalNumber;
    }

    /**
     *  Gives a list of both completed and running jobs with status. Jobs still running will be first.
     *  The completed jobs will only contain last 10000 completed jobs
     *  
     * @return List of jobs with status
     */    
    public static List<OaiJobDto> getJobs() {
        List<OaiJobDto> running=OaiJobCache.getRunningJobsMostRecentFirst();
        List<OaiJobDto> completed=OaiJobCache.getCompletedJobsMostRecentFirst();
        List<OaiJobDto> result = new ArrayList<>();
        result.addAll(running);
        result.addAll(completed);
        return result;
    }


    /**
     * This method will be called by the {@link #oaiIngestJobScheduler(String, ArrayList)}-method}<br>
     * The scheduler method will set up the job and responsible for status of the job. <br>
     * The target will be harvest full for this interval using the resumptionToken from the response and call recursively.<br>
     * For each successful response the persistent datestamp for the OAI target will be updated with datestamp from last parsed records.<br>
     *      
     * @param job The configured OAI target
     * @param from Datestamp format that will be accepted for that OAI target
     * @param until Datestamp format that will be accepted for that OAI target
     * @return Number of harvested records for this date interval. Records discarded by filter etc. will not be counted.
     * @throws IOException If anything unexpected happens. OAI target does not respond, invalid xml, XSLT (filtering) failed etc.
     */
    private static Integer oaiIngestPerform(OaiTargetJob job, String from) throws IOException, ApiException {

        //In the OAI spec, the from-parameter can be both yyyy-MM-dd or full UTC timestamp (2021-10-09T09:42:03Z)
        //But COP only supports the short version. So when this is called use short format
        //Preservica seems to only accept full UTC format
        //Dirty but quick solution fix. Best would be if COP could fix it

        OaiTargetDto oaiTargetDto = job.getDto();

        // TODO: Change this to datasource in the OpenAPI specification
        String origin=oaiTargetDto.getDatasource();
        String targetName = oaiTargetDto.getName();

        DsStorageClient dsAPI = getDsStorageApiClient();
        OaiHarvestClient client = new OaiHarvestClient(job,from);
        OaiResponse response = client.next();

        OaiResponseFilter oaiFilter;
        if (oaiTargetDto.getFilter() == null) {
            throw new IllegalStateException("The filter for OaiTargetDto '" + targetName + "' was null");
        }
        switch (oaiTargetDto.getFilter()) {
            case DIRECT:
                oaiFilter = new OaiResponseFilter(origin, dsAPI);
                break;
            case DR:
                oaiFilter = new OaiResponseFilterDrArchive(origin, dsAPI);
                break;
            case PRESERVICA:
                oaiFilter = new OaiResponseFilterPreservicaSeven(origin, dsAPI);
                break;
            default: throw new UnsupportedOperationException(
                    "Unknown filter '" + oaiTargetDto.getFilter() + "' for target '" + targetName + "'");
        }

        while (response.getRecords().size() >0) {

            OaiRecord lastRecord = response.getRecords().get(response.getRecords().size()-1);

            oaiFilter.addToStorage(response);

            log.info("Processed '{}' records from origin: '{}' out of a total of '{}' records.",
                    oaiFilter.getProcessed(), origin, response.getTotalRecords());

            //Update timestamp with timestamp from last OAI record.
            HarvestTimeUtil.updateDatestampForOaiTarget(oaiTargetDto,lastRecord.getDateStamp());

            response = client.next(); //load next (can be empty)
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
            sleep(1); // So next ID is different.
        }
        catch(Exception e) {
            //can not happen, nothing will interrupt.
        }

        OaiTargetJob  job = new OaiTargetJob(id, dto);                
        return job;                
    }


    private static DsKalturaClient getKalturaClient() throws IOException {
        String kalturaUrl= ServiceConfig.getConfig().getString("kaltura.url");
        String adminSecret = ServiceConfig.getConfig().getString("kaltura.adminSecret"); //Must not be shared or exposed.
        Integer partnerId = ServiceConfig.getConfig().getInteger("kaltura.partnerId");  
        String userId = ServiceConfig.getConfig().getString("kaltura.userId");                               
        long sessionKeepAliveSeconds=3600L; //1 hour
        log.info("creating kaltura client for partnerID:"+partnerId);
        DsKalturaClient kalturaClient = new DsKalturaClient(kalturaUrl,userId,partnerId,adminSecret,sessionKeepAliveSeconds);
        return kalturaClient;
    }


    private static DsStorageClient getDsStorageApiClient() {
        if (storageClient != null) {
            return storageClient;
        }

        String dsStorageUrl = ServiceConfig.getDsStorageUrl();
        storageClient = new DsStorageClient(dsStorageUrl);
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

    //TODO remove
    public static void enrichMetadataRecord(String id) {
        DsStorageClient storageClient = new DsStorageClient(ServiceConfig.getDsStorageUrl());

        try {
            DsRecordDto record = storageClient.getRecord(id,false);
            DataEnricher.apply(record);
            storageClient.recordPost(record);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }

    }

    //TODO remove
    public static long enrichMetadataRecords(String origin, Long mTimeFrom) throws IOException, ApiException {
        DsStorageClient storageClient = new DsStorageClient(ServiceConfig.getDsStorageUrl());

        long processedRecords= 0L;
        boolean hasMore = true;

        AtomicInteger counter = new AtomicInteger(0);
        AtomicLong currentTime = new AtomicLong(System.currentTimeMillis());


        try (ContinuationInputStream<Long> dsDocsStream =
                     storageClient.getRecordsModifiedAfterJSON(origin, mTimeFrom, 1000L)){
            log.info("Enriching {} records from DS-storage origin '{}'. '{}' records have been enriched through this request.",
                    dsDocsStream.getRecordCount(), origin, counter.get());

            dsDocsStream.stream(DsRecordDto.class)
                    .map(DataEnricher::apply)
                    .forEach((record) -> {
                                try {
                                    storageClient.recordPost(record);
                                } catch (ApiException e) {
                                    // Error handling
                                }
                            }
                    );

            hasMore = dsDocsStream.hasMore();
            if (hasMore) {
                mTimeFrom = dsDocsStream.getContinuationToken(); //Next batch start from here.
            }
        } catch (IOException e) {
            log.warn("DsStorage threw an exception while streaming records through the DsStorageClient.getRecordsByRecordTypeModifiedAfterLocalTreeJSON() method. " +
                            "The method was called with the following parameters: origin='{}', recordType='{}', mTime='{}', maxRecords={}.",
                    origin, RecordTypeDto.DELIVERABLEUNIT, mTimeFrom, "1000");
            throw e;
        }


        return 0L;
    }

    /**
     * Method to update records in Preservica 7 related origins in backing {@code DsStorage} with children IDs.
     * The method filters incoming records on IDs representing InformationObjects from Preservica 7, then tries to fetch
     * a manifestation for the record and updates the storage record with the manifestation as a childrenID.
     * @param origin to update records in.
     * @param mTimeFrom to update records from.
     * @return a count of records that have been updated.
     */
    public static long updateManifestationForRecords(String origin, Long mTimeFrom) throws IOException {
        DsStorageClient storageClient = new DsStorageClient(ServiceConfig.getDsStorageUrl());
        PreservicaManifestationExtractor manifestationPlugin = new PreservicaManifestationExtractor();

        long processedRecords= 0L;
        long startTime = System.currentTimeMillis();
        long startTimeWithExtraZeros = startTime * 1000;
        boolean hasMore = true;
        AtomicInteger counter = new AtomicInteger(0);
        AtomicLong currentTime = new AtomicLong(System.currentTimeMillis());

        while (hasMore) {
            try (ContinuationInputStream<Long> dsDocsStream =
                    storageClient.getMinimalRecordsModifiedAfterJSON(origin, mTimeFrom, 1000L)){
                log.info("Enriching {} records from DS-storage origin '{}'. '{}' records have been enriched through this request.",
                        dsDocsStream.getRecordCount(), origin, counter.get());

                dsDocsStream.stream(DsRecordMinimalDto.class)
                    .takeWhile(record -> record.getmTime() < startTimeWithExtraZeros)
                    .parallel() // Parallelize stream for performance boost.
                    .map(record -> PreservicaUtils.fetchManifestation(record, manifestationPlugin, counter, currentTime))
                    .filter(PreservicaUtils::validateRecord)
                    .forEach(record -> PreservicaUtils.safeRecordPost(storageClient, record));

                hasMore = dsDocsStream.hasMore();
                if (hasMore) {
                    mTimeFrom = dsDocsStream.getContinuationToken(); //Next batch start from here.
                }
            } catch (IOException e) {
                log.warn("DsStorage threw an exception while streaming records through the DsStorageClient.getRecordsByRecordTypeModifiedAfterLocalTreeJSON() method. " +
                        "The method was called with the following parameters: origin='{}', recordType='{}', mTime='{}', maxRecords={}.",
                        origin, RecordTypeDto.DELIVERABLEUNIT, mTimeFrom, "1000");
                throw e;
            }
        }

        log.info("Updated '{}' records in '{}' milliseconds.", counter.get(), System.currentTimeMillis() - startTime);
        return processedRecords;
    }

}
