package dk.kb.datahandler.oai;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.datahandler.model.v1.DsDatahandlerJobDto;
import dk.kb.util.webservice.exception.InvalidArgumentServiceException;


/*
 * Access to all methods working with the maps needs to be syncronized to prevent concurrent modification error. 
 * 
 */
public class OaiJobCache {

    private static HashMap<Long, DsDatahandlerJobDto> runningJobsMap = new HashMap<Long, DsDatahandlerJobDto>();
    private static TreeMap<Long, DsDatahandlerJobDto> completedJobsMap = new TreeMap<Long, DsDatahandlerJobDto>(); //ordered
    private static Logger log = LoggerFactory.getLogger(OaiJobCache.class);
    private OaiJobCache() { // No need for constructor       
    }

    enum STATUS {
        RUNNING,
        COMPLETED        
      }

    /**
     * Add a new job to running job list.
     *  If a job with same name is already running, will throw
     * 
     * @param DsDatahandlerJobDto job information about the job
     * @throws InvalidArgumentServiceException If a job with same name is already in RUNNING status. 
     * 
     */
    public static synchronized void addNewJob(DsDatahandlerJobDto job) throws InvalidArgumentServiceException{             
        validateNotAlreadyRunning(job.getName());        
        job.setStatus(STATUS.RUNNING.toString());
        runningJobsMap.put(job.getId(), job);                
    }

    
    
    /*
     * Change status and move from running map to completed map
     */
    public static synchronized void finishJob(DsDatahandlerJobDto job, int numberOfRecords, boolean error) {

        runningJobsMap.remove(job.getId());        
        if (error) {
            job.setError(true);            
        }        
        job.setStatus(STATUS.COMPLETED.toString());
        job.setNumberOfRecords(numberOfRecords);
        job.setCompletedTime(OaiJobCache.formatSystemMillis(System.currentTimeMillis()));        
        log.info("Setting completed for job:"+job.getName() +" time:"+job.getCompletedTime());
                 
        completedJobsMap.put(job.getId(), job);        
        
    }

    public static synchronized List<DsDatahandlerJobDto> getRunningJobsMostRecentFirst(){    
        ArrayList<DsDatahandlerJobDto>  runningJobs =new  ArrayList<DsDatahandlerJobDto>(runningJobsMap.values());               
         //TODO sort reversed                
        return runningJobs;   
    }

    public static synchronized List<DsDatahandlerJobDto> getCompletedJobsMostRecentFirst(){    
        ArrayList<DsDatahandlerJobDto>  completedJobs =new  ArrayList<DsDatahandlerJobDto>(completedJobsMap.values());                           
        //TODO sort reversed
        return completedJobs;        
    }

    //Convert to the smaller OaiJobDto with much fewer attributes. (and no urls/user/passwords etc. for the target)
    public static List<DsDatahandlerJobDto> XconvertToDto(List<DsDatahandlerJob> jobs){

        List<DsDatahandlerJobDto> dtoList = new  ArrayList<DsDatahandlerJobDto>();
        for (DsDatahandlerJob targetJob : jobs) {         
            DsDatahandlerJobDto jobDto = new DsDatahandlerJobDto();        
            jobDto.setId(targetJob.getId());
            jobDto.setName(targetJob.getDto().getName());
            //jobDto.setType(null);
            jobDto.setNumberOfRecords(targetJob.getRecordsHarvested());
            jobDto.setError(targetJob.isError());
            jobDto.setStatus(targetJob.getStatus().name());
            jobDto.setStartedTime(formatSystemMillis(targetJob.getId()));        
            
            if (targetJob.getCompletedTime() != 0) {
                jobDto.setCompletedTime(formatSystemMillis(targetJob.getCompletedTime()));                     
            }
            
            dtoList.add(jobDto);

        }

        return dtoList;

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
        boolean alreadyRunning= OaiJobCache.isJobRunningForTarget(jobName);        
        if (alreadyRunning) {
            throw new InvalidArgumentServiceException("There is already a job running for target:"+jobName);
        }
    }
    
}

