package dk.kb.datahandler.facade;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import dk.kb.datahandler.oai.OaiResponseFilterDrArchive;
import dk.kb.datahandler.oai.OaiResponseFilterPreservicaSeven;
import dk.kb.storage.model.v1.DsRecordMinimalDto;

import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import dk.kb.datahandler.config.ServiceConfig;
import dk.kb.datahandler.model.v1.DsDatahandlerJobDto;
import dk.kb.datahandler.model.v1.OaiTargetDto;

import dk.kb.datahandler.oai.OaiHarvestClient;
import dk.kb.datahandler.job.JobCache;
import dk.kb.datahandler.kaltura.KalturaDeltaUploadJob;
import dk.kb.datahandler.oai.OaiRecord;
import dk.kb.datahandler.oai.OaiResponse;
import dk.kb.datahandler.oai.OaiResponseFilter;
import dk.kb.datahandler.util.HarvestTimeUtil;
import dk.kb.datahandler.util.SolrUtils;
import dk.kb.kaltura.client.DsKalturaClient;
import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.storage.model.v1.MappingDto;
import dk.kb.storage.util.DsStorageClient;
import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.webservice.exception.InvalidArgumentServiceException;
import dk.kb.util.webservice.exception.ServiceException;

public class DsDatahandlerFacade {
    private static final Logger log = LoggerFactory.getLogger(DsDatahandlerFacade.class);

    private static DsStorageClient storageClient;

    /**
     * Deprecated. The use of the mapping table was a temporary solution for "skygge prod"
     * 
     * <p>
     * Will load all referenceId defined in storage and call Kaltura to map them to KalturaId.
     * The results will be saved in ds-storage in the mapping table.
     * When all mappings has been updated, the records will be enriched with the kalturaId.
     * If the job fails before all mappingss are loaded, the records will not be enriched from the mapping table.
     * </p>
     * @param origin Only update mappings from this origin
     * @param mTimeFrom Only update mappings for records with mTime after mTimeFrom
     * @return Number of mappings updated.
     * @throws IOException
     * @throws ServiceException
     */
    @Deprecated 
    public static long fetchKalturaIdsAndUpdateRecords(String origin, Long mTimeFrom) throws IOException, ServiceException {
        if (mTimeFrom == null) {
            mTimeFrom = 0L;
        }

        long start = System.currentTimeMillis();
        long timer = start;
        int batchSize = 100; //No need to take this as input parameter and make method more complicated.
        //This is a good value and can also be used as batchSize against Kaltura.

        log.info("Starting resolving of Kaltura entry ID's for origin: '{}'. Resolving in batches of '{}' records.",
                origin, batchSize);

        DsStorageClient dsAPI = getDsStorageApiClient();
        DsKalturaClient kalturaClient = getKalturaClient();

        long processed = 0;
        long updated = 0;
        long recordsWithoutReferenceId = 0;
        List<DsRecordMinimalDto> records;
        DsDatahandlerJobDto job = JobCache.createKalturaEnrichmentJob(origin, mTimeFrom);
        try {
            while(true) {
                if (processed % 500 == 0) {
                    log.info("Processed '{}' records in total. Updated '{}' mappings in total. Processing the last 500 records took '{}' milliseconds.",
                            processed, processed, System.currentTimeMillis() - timer);
                    timer = System.currentTimeMillis();
                }

                records = dsAPI.getMinimalRecords(origin, batchSize, mTimeFrom);
                if (records.isEmpty()) { //no more records
                    break;
                }

                mTimeFrom = records.get(records.size()-1).getmTime(); //update mTime to mTime from last record.
                log.debug("Getting DsRecordMinimal from storage for origin={}, batchSize={}, mTimeFrom={}", origin, batchSize, mTimeFrom);

                ArrayList<String> referenceIdsList= new ArrayList<String>();
                for (DsRecordMinimalDto record: records) {
                    if (record.getReferenceId() != null) { //This should not be null in production after preservica enrichment. But for stage/prod most will be null
                        referenceIdsList.add(record.getReferenceId());
                    } else {
                        // Updating variable for logging.
                        recordsWithoutReferenceId ++;
                    }
                    // Counting all processed records.
                    processed ++;
                }

                // If none of the records currently in hand have a reference ID then continue to next iteration of loop.
                if (referenceIdsList.isEmpty()) { // Should not happen in production.
                    continue;
                }

                log.debug("Calling Kaltura to resolve kalturaId for referenceIds. Calling with a batch of '{}' records.", referenceIdsList.size());
                Map<String, String> kalturaIds = kalturaClient.getKalturaIds(referenceIdsList);

                if (kalturaIds.size() != referenceIdsList.size()) {
                    log.warn("Not all referenceIds were found at Kaltura"); //Should not happen
                }
                updated += kalturaIds.size();

                log.debug("Updating '{}' mappings in the DS-Storage mapping table, which handles relations between referenceIds and kalturaIds.", kalturaIds.size());
                for (String referenceId: kalturaIds.keySet()) {                            
                    //Can be optimized with method that takes multiple. But this workflow is called rarely.             
                    MappingDto mappingDto= new MappingDto();
                    mappingDto.setReferenceId(referenceId);
                    mappingDto.setKalturaId(kalturaIds.get(referenceId));
                    dsAPI.mappingPost(mappingDto);
                }
            }        

            //Mapping table is now updated. Enrich all records that does not have KalturaId
            log.debug("Updating kaltura ID for ALL records from mapping table.");
            dsAPI.updateKalturaIdForRecords(); //Will update all records with kalturaid using the mapping tab
            log.info("Updated kalturaId mapping table and enriched records. Number of records updated is: '{}'. Number of processed records is: '{}'. Number of records without " +
                    "reference ID processed: '{}' The full request lasted '{}' milliseconds.",
                    updated, processed, recordsWithoutReferenceId, (System.currentTimeMillis() - start));

            JobCache.finishJob(job ,(int) updated, false); //no error
            return updated;
        }
        catch (Exception e) {
            JobCache.finishJob(job ,(int) updated, true);  //error            
            throw new InternalServiceException("Error updating kalturaIds",e);         
        }
    }

    /**
     * Ingest records directly into ds-storage from a zip-file containing multiple files that each is a xml-file with a single record
     *  
     * @param  origin The origin for collection documents. The origin must be defined in ds-storage. 
     * @param is InputStream. Must be a zip-file containing single files that each is an XML record.
     * @return List of strings containing the records that failed parsing.
     * @throws ServiceException
     */    
    public static ArrayList<String> ingestFromZipfile(String origin, InputStream is) throws ServiceException {
        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(is));

        DsStorageClient dsAPI = getDsStorageApiClient();
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
            String msg = "Error parsing xml record for file: " + fileName;
            log.error(msg, e);
            throw new InvalidArgumentServiceException(msg);
        } catch (ServiceException e){
            errorRecords.add(fileName);
            log.error("Error posting record with filename: '{}' to DsStorage.", fileName, e);
            throw e; 
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
     * @exception InternalServiceException Will throw exception is the dsPresentCollectionName is not known, or if server communication fails.
     */    
    public static String indexSolrFull(String origin) throws InternalServiceException {

        DsDatahandlerJobDto job = JobCache.createIndexSolrJob(origin, 0L);

        String response=null;
        try {
          response= SolrUtils.indexOrigin(origin, 0L);
          
        }
        
        catch(Exception e){
            JobCache.finishJob(job, -1, true); //error
            throw e; 
        }        
        
        JobCache.finishJob(job, -1, false); //No error. We do not know how many records processed. But can be parsed from response.
        return response;
    }

    /**
     * Indexes ds-storage records that haven't been indexed in solr through the following workflow:
     * Get latest  ds-storage modification time for records in existing solr index.
     * Then fetch newer records from ds-storage, transform to solr documents in ds-present and index into solr.
     * @param origin to index records from.
     * @throws InternalServiceException
     * @throws SolrServerException
     * @throws IOException
     */
    public static String indexSolrDelta(String origin) throws InternalServiceException, SolrServerException, IOException {
        Long lastStorageMTime = SolrUtils.getLatestMTimeForOrigin(origin);
        String response = null;
        DsDatahandlerJobDto job = JobCache.createIndexSolrJob(origin, lastStorageMTime);
        try {
         response= SolrUtils.indexOrigin(origin, lastStorageMTime);
        }
        catch(Exception e){
            JobCache.finishJob(job, -1, true); //error
            throw e; 
        }                
        JobCache.finishJob(job, -1, false); //No error. We do not know how many records processed. (But can be parsed from response maybe)
        return response;
    }

    /**
     * <p>
     * Start job that uploading streams to kaltura that does not have an kaltura_id from the given mTimeFrom
     * <p>
     * Will only extract records from Solr with access_malfunction:false and production_code_allowed:true 
     * <p>
     * Storage records will be updated with the kalturaid or error message. Errors are
     * <ul>
     * <li>File missing</li>
     * <li>File too short</li>
     * <li>Kaltura API error. Very rare this happens. Have not seen it yet.</li>
     * </ul>
     * <p>
     * It is important to mark the records as failed or a new delta upload job will start processing the same streams with errors every time.
     *
     * <p>
     * A solr delta indexing job will be started if both the job completes succesfully or fails. 
     * 
     * @param mTimeFrom only uploading missing streams for records with mTimeFrom this value or higher 
     * @throws InternalServiceException
     * @throws SolrServerException
     * @throws IOException
     */
    public static void kalturaDeltaUpload(Long mTimeFrom) throws InternalServiceException, SolrServerException, IOException {

        DsDatahandlerJobDto job = JobCache.createKalturaDeltaUploadJob(mTimeFrom); //For job cache
        log.info("Starting kaltura delta upload from mTimeFrom: " + mTimeFrom);
        try {
          
          //upload strems  
          int numberStreamsUploaded=KalturaDeltaUploadJob.uploadStreamsToKaltura(mTimeFrom);
          log.info("Kaltura delta uploaded completed sucessfully. #streams uploaded={}", numberStreamsUploaded);
          
          //Index the records that has mTime modified due to kalturaId was set on record.
          if (numberStreamsUploaded >0) {
             log.info("Starting solr delta index job");
             indexSolrDelta("ds.tv");          
             indexSolrDelta("ds.radio");
          }
        }
        catch(Throwable e){
            log.error("Kaltura delta upload/indexing stopped due to error",e);
            JobCache.finishJob(job, -1, true); //error
            throw e; 
        }                
        JobCache.finishJob(job, -1, false); //No error.
    }
    
    /**
     * Starts a full OAI harvest job for the target.
     * The job will harvest records from the OAI server and ingest them into DS-storage  
     * If OAI strategy for the target is dayOnly, the harvest process will be split into days instead of a single job. 
     *  
     * @param oaiTargetName the location of the image, relative to the url argument
     * @return Number of harvested records.
     */        
    public static Integer oaiIngestFull(String oaiTargetName) {
        OaiTargetDto oaiTargetDto = ServiceConfig.getOaiTargets().get(oaiTargetName);

        String from = HarvestTimeUtil.generateFrom(oaiTargetDto, null); // from == null, use default start day for OAI target instead
        Integer totalHarvested = oaiIngestJobScheduler(oaiTargetName, from);
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
     * @throws InternalServiceException
     */
    protected static Integer oaiIngestJobScheduler(String oaiTargetName, String from) throws InternalServiceException {
        int totalNumber = 0;

        log.info("Starting jobs from: " + from + " for target: " + oaiTargetName);
                       
            OaiTargetDto oaiTargetDto = ServiceConfig.getOaiTargets().get(oaiTargetName);                
            if (oaiTargetDto == null) {
                throw new InvalidArgumentServiceException("No target found in configuration with name: '" + oaiTargetName +
                        "'. See the config method for list of configured targets.");
            }

            DsDatahandlerJobDto job = JobCache.createNewOaiJob(oaiTargetDto,from);        
        
            try {                       
                int number = oaiIngestPerform(job, oaiTargetDto, from);
                JobCache.finishJob(job, number, false); //No error
                totalNumber += number;
            }
            catch (Exception e) {
                log.error("Oai harvest did not complete successfully for target: '{}'", oaiTargetName);                
                JobCache.finishJob(job, totalNumber, true); //Error
                throw new InternalServiceException("Error harvesting oai target: " + oaiTargetName, e);
            }
        
        return totalNumber;
    }

    /**
     *  Gives a list of both completed and running jobs with status. Jobs still running will be first.
     *  The completed jobs will only contain last 10000 completed jobs
     *  
     * @return List of jobs with status
     */    
    public static List<DsDatahandlerJobDto> getJobs() {
        List<DsDatahandlerJobDto> running=JobCache.getRunningJobsMostRecentFirst();
        List<DsDatahandlerJobDto> completed=JobCache.getCompletedJobsMostRecentFirst();
        List<DsDatahandlerJobDto> result = new ArrayList<>();
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
     * @throws ServiceException
     */
    private static Integer oaiIngestPerform(DsDatahandlerJobDto job, OaiTargetDto oaiTargetDto, String from) throws IOException, ServiceException {

        //In the OAI spec, the from-parameter can be both yyyy-MM-dd or full UTC timestamp (2021-10-09T09:42:03Z)
        //But COP only supports the short version. So when this is called use short format
        //Preservica seems to only accept full UTC format
        //Dirty but quick solution fix. Best would be if COP could fix it

        // TODO: Change this to datasource in the OpenAPI specification
        String origin = oaiTargetDto.getDatasource();
        String targetName = oaiTargetDto.getName();

        DsStorageClient dsAPI = getDsStorageApiClient();
        OaiHarvestClient client = new OaiHarvestClient(job, oaiTargetDto, from);
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

        while (response.getRecords().size() > 0) {

            OaiRecord lastRecord = response.getRecords().get(response.getRecords().size()-1);

            oaiFilter.addToStorage(response);

            log.info("Processed '{}' records from origin: '{}' out of a total of '{}' records.",
                    oaiFilter.getProcessed(), origin, response.getTotalRecords());

            //Update timestamp with timestamp from last OAI record.
            HarvestTimeUtil.updateDatestampForOaiTarget(oaiTargetDto,lastRecord.getDateStamp());

            response = client.next(); //load next (can be empty)
        }

        if (response.isError()) {
            throw new InternalServiceException("Error during harvest for target: " + oaiTargetDto.getName() +
                    " after harvesting: " + oaiFilter.getProcessed() + " records");
        }

        log.info("Completed ingesting origin '{}' successfully with {} records", origin, oaiFilter.getProcessed());
        return oaiFilter.getProcessed();
    }

    private static DsKalturaClient getKalturaClient() throws IOException {
        String kalturaUrl= ServiceConfig.getKalturaUrl();
        String adminSecret = ServiceConfig.getKalturaAdminSecret();
        Integer partnerId = ServiceConfig.getKalturaPartnerId();  
        String userId = ServiceConfig.getKalturaUserId();                               
        String token= ServiceConfig.getKalturaToken();
        String tokenId= ServiceConfig.getKalturaTokenId();               
        int sessionDurationSeconds=ServiceConfig.getKalturaSessionDurationSeconds();
        int sessionRefreshThreshold=ServiceConfig.getKalturaSessionRefreshThreshold();
                
        log.info("creating kaltura client for partnerID:"+partnerId);     
        DsKalturaClient kalturaClient = new DsKalturaClient(kalturaUrl, userId, partnerId, token, tokenId, adminSecret, sessionDurationSeconds, sessionRefreshThreshold);
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
}
