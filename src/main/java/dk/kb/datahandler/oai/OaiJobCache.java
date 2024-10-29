package dk.kb.datahandler.oai;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.datahandler.model.v1.DsDatahandlerJobDto;
import dk.kb.datahandler.oai.DsDatahandlerJob.STATUS;


/*
 * Access to all methods working with the maps needs to be syncronized to prevent concurrent modification error. 
 * 
 */
public class OaiJobCache {

    private static HashMap<Long, DsDatahandlerJob> runningJobsMap = new HashMap<Long, DsDatahandlerJob>();
    private static TreeMap<Long, DsDatahandlerJob> completedJobsMap = new TreeMap<Long, DsDatahandlerJob>(); //ordered
    private static Logger log = LoggerFactory.getLogger(OaiJobCache.class);
    private OaiJobCache() { // No need for constructor       
    }

    public static synchronized void addNewJob(DsDatahandlerJob job) {     
        job.setStatus(STATUS.RUNNING);
        runningJobsMap.put(job.getId(), job);                
    }

    /*
     * Change status and move from running map to completed map
     */
    public static synchronized void finishJob(DsDatahandlerJob job, int numberOfRecords, boolean error) {

        runningJobsMap.remove(job.getId());        
        if (error) {
            job.setError(true);            
        }        
        job.setStatus(STATUS.COMPLETED);
        job.setRecordsHarvested(numberOfRecords);
        job.setCompletedTime(System.currentTimeMillis());        
        log.info("Setting completed for job:"+job.getDto().getName() +" time:"+job.getCompletedTime());
                 
        completedJobsMap.put(job.getId(), job);        
        
    }

    public static synchronized List<DsDatahandlerJobDto> getRunningJobsMostRecentFirst(){    
        ArrayList<DsDatahandlerJob>  runningJobs =new  ArrayList<DsDatahandlerJob>(runningJobsMap.values());        
        List<DsDatahandlerJob> sorted =runningJobs.stream()
                .sorted(Comparator.comparing(DsDatahandlerJob::getId).reversed())
                .collect(Collectors.toList());                

        return convertToDto(sorted);        
    }

    public static synchronized List<DsDatahandlerJobDto> getCompletedJobsMostRecentFirst(){    
        ArrayList<DsDatahandlerJob>  completedJobs =new  ArrayList<DsDatahandlerJob>(completedJobsMap.values());      
        List<DsDatahandlerJob> sorted =completedJobs.stream()
                .sorted(Comparator.comparing(DsDatahandlerJob::getId).reversed())
                .collect(Collectors.toList());                
        return convertToDto(sorted);        
    }

    //Convert to the smaller OaiJobDto with much fewer attributes. (and no urls/user/passwords etc. for the target)
    public static List<DsDatahandlerJobDto> convertToDto(List<DsDatahandlerJob> jobs){

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
        Collection<DsDatahandlerJob> running = runningJobsMap.values();
        for (DsDatahandlerJob job : running) {            
            if (job.getDto().getName().equals(targetName)) {
              return true;
            }               
        }        
        return false;
    }

    private static String formatSystemMillis(long millis) {        
        LocalDateTime myDateObj=Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDateTime();                 
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss",Locale.getDefault());
        String formattedDate = myDateObj.format(dateFormat);
        return formattedDate;   

    }

}

