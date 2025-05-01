package dk.kb.datahandler.job;

import static java.lang.Thread.sleep;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import dk.kb.datahandler.model.v1.DsDatahandlerJobDto;
import dk.kb.datahandler.model.v1.OaiTargetDto;
import dk.kb.util.webservice.exception.InvalidArgumentServiceException;


/*
 * Access to all methods working with the maps needs to be syncronized to prevent concurrent modification error. 
 * 
 */
public class JobCache {

    private static HashMap<Long, DsDatahandlerJobDto> runningJobsMap = new HashMap<Long, DsDatahandlerJobDto>();
    private static TreeMap<Long, DsDatahandlerJobDto> completedJobsMap = new TreeMap<Long, DsDatahandlerJobDto>(); //ordered
    private static Logger log = LoggerFactory.getLogger(JobCache.class);
    private JobCache() { // No need for constructor       
    }

    enum  STATUS {
        RUNNING,
        COMPLETED        
      }

    /**
     * Add a new job to running job list.
     * If a job with same name is already running will throw exception.
     * 
     * @param DsDatahandlerJobDto job information about the job
     * @throws InvalidArgumentServiceException If a job with same name is already in RUNNING status. 
     * 
     */
    public static synchronized void addNewJob(DsDatahandlerJobDto job) throws InvalidArgumentServiceException{             
        validateNotAlreadyRunning(job.getName());        
        job.setStatus(STATUS.RUNNING.toString());
        job.setStartedTime(formatSystemMillis(System.currentTimeMillis()));
        runningJobsMap.put(job.getId(), job);                
    }

    
    
    /*
     * Change status and move from running map to completed map
     */
    public static synchronized void finishJob(DsDatahandlerJobDto job, int numberOfRecords, boolean error) {
        job.setCompletedTime(JobCache.formatSystemMillis(System.currentTimeMillis()));
        runningJobsMap.remove(job.getId());        
        if (error) {
            job.setError(true);            
        }        
        job.setStatus(STATUS.COMPLETED.toString());
        job.setNumberOfRecords(numberOfRecords);
        job.setCompletedTime(formatSystemMillis(System.currentTimeMillis()));        
        log.info("Setting completed for job:"+job.getName() +" time:"+job.getCompletedTime());
                 
        completedJobsMap.put(job.getId(), job);        
        
    }

    public static synchronized List<DsDatahandlerJobDto> getRunningJobsMostRecentFirst(){    
        ArrayList<DsDatahandlerJobDto>  runningJobs =new  ArrayList<DsDatahandlerJobDto>(runningJobsMap.values());               

        //Sort with most recent first.
        Collections.sort(runningJobs, Comparator.comparing(DsDatahandlerJobDto::getId, Comparator.reverseOrder()));        
        return runningJobs;   
    }

    public static synchronized List<DsDatahandlerJobDto> getCompletedJobsMostRecentFirst(){    
        ArrayList<DsDatahandlerJobDto>  completedJobs =new  ArrayList<DsDatahandlerJobDto>(completedJobsMap.values());                           

        //Sort with most recent first.
        Collections.sort(completedJobs, Comparator.comparing(DsDatahandlerJobDto::getId,  Comparator.reverseOrder()));
        return completedJobs;        
    }

    public static synchronized boolean isJobRunningForTarget(String targetName) {
        Collection<DsDatahandlerJobDto> running = runningJobsMap.values();
        for (DsDatahandlerJobDto job : running) {            
            if (job.getName().equals(targetName)) {
              return true;
            }               
        }        
        return false;
    }

    public static String formatSystemMillis(long millis) {        
        LocalDateTime myDateObj=Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDateTime();                 
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss",Locale.getDefault());
        String formattedDate = myDateObj.format(dateFormat);
        return formattedDate;   

    }

    private static synchronized void validateNotAlreadyRunning(String jobName) {
        boolean alreadyRunning= JobCache.isJobRunningForTarget(jobName);        
        if (alreadyRunning) {
            throw new InvalidArgumentServiceException("There is already a job running with name:"+jobName);
        }
    }
    

    /**
     * Generates a {@link DsDatahandlerJobDto from a {@link OaiTargetDto}.
     * <p>
     * The job will have a unique timestamp used as ID.  
     *   
     */
    public static synchronized DsDatahandlerJobDto createNewOaiJob(OaiTargetDto dto, String from) {                  

       long id=JobCache.getNextId();

        DsDatahandlerJobDto  job = new DsDatahandlerJobDto();
        job.setId(id);
        job.setName("OAI:"+dto.getName()); //name is key in job cache. Only start one OAI from each target.
        job.setType("OAI");
        job.setFrom(from);
        //register job
        JobCache.addNewJob(job);
        
        return job;                
    }
    
    /**
     * Generates a {@link DsDatahandlerJobDto from a the origin for solr indexing}.
     * <p>
     * The job will have a unique timestamp used as ID.  
     *   
     */
    public static synchronized DsDatahandlerJobDto createIndexSolrJob(String origin, Long mTime) {                  

       long id=JobCache.getNextId();

        DsDatahandlerJobDto  job = new DsDatahandlerJobDto();
        job.setId(id);
        job.setName("SOLR_INDEX:"+origin); //name is key in job cache. Only one job with this name can be started
        job.setType("SOLR_INDEX");
        job.setFrom(parseMTime(mTime));
        //register job
        JobCache.addNewJob(job);
                
        return job;                
    }
    
    

    /**
     * Generates a {@link DsDatahandlerJobDto from a the origin for manifestation enrichment}.
     * <p>
     * The job will have a unique timestamp used as ID.  
     *   
     */
    public static synchronized DsDatahandlerJobDto createPreservicaManifestationJob(String origin, Long mTime) {                  
       long id=JobCache.getNextId();

        DsDatahandlerJobDto  job = new DsDatahandlerJobDto();
        job.setId(id);
        job.setName("PRESERVICA_MANIFESTATION:"+origin); //name is key in job cache. Only one job with this name can be started.
        job.setType("PRESERVICA_MANIFESTATION");
        job.setFrom(parseMTime(mTime));
        
        //register job
        JobCache.addNewJob(job);
                
        return job;                
    }
    
    
    /**
     * Generates a {@link DsDatahandlerJobDto from a the origin for solr indexing}.
     * <p>
     * The job will have a unique timestamp used as ID.  
     *   
     */
    public static synchronized DsDatahandlerJobDto createKalturaEnrichmentJob(String origin, Long mTime) {                  

       long id=JobCache.getNextId();

        DsDatahandlerJobDto  job = new DsDatahandlerJobDto();
        job.setId(id);
        job.setName("KALTURA:"+origin); //name is key in job cache. Only one job with this name can be started.
        job.setType("KALTURA_ENTRY_ID");
        job.setFrom(parseMTime(mTime));
        
        //register job
        JobCache.addNewJob(job);
                
        return job;                
    }
    
    
    /**
     * Generates a {@link DsDatahandlerJobDto for KalturaDeltaUpload}.
     * <p>
     * The job will have a unique timestamp used as ID.  
     *   
     */
    public static synchronized DsDatahandlerJobDto createKalturaDeltaUploadJob(Long mTime) {                  

       long id=JobCache.getNextId();

        DsDatahandlerJobDto  job = new DsDatahandlerJobDto();
        job.setId(id);
        job.setName("KALTURA DELTAUPLOAD FROM:"+mTime); //name is key in job cache. Only one job with this name can be started.
        job.setType("KALTURA_DELTA_UPLOAD");
        job.setFrom(parseMTime(mTime));
        
        //register job
        JobCache.addNewJob(job);
                
        return job;                
    }
    
    
    
    private static String parseMTime(Long mTime) {
        return (mTime==null) ? "0":""+mTime;                 
    }
    
    /**
     * Syncronized method to make sure all ID's are different.
     * 
     * @return system.currentTime in millis.
     */
    
    public static synchronized  long getNextId() {
        long id = System.currentTimeMillis();
        try {
            sleep(1); // So next ID is different.
        }
        catch(Exception e) {
            //can not happen, nothing will interrupt.
        }

        return id;
        
        
    }
}

