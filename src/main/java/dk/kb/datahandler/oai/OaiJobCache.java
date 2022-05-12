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

import dk.kb.datahandler.model.v1.OaiJobDto;
import dk.kb.datahandler.oai.OaiTargetJob.STATUS;

public class OaiJobCache {

    private static HashMap<Long, OaiTargetJob> runningJobsMap = new HashMap<Long, OaiTargetJob>();
    private static TreeMap<Long, OaiTargetJob> completedJobsMap = new TreeMap<Long, OaiTargetJob>(); //ordered
    private static Logger log = LoggerFactory.getLogger(OaiJobCache.class);
    private OaiJobCache() { // No need for constructor       
    }

    public static synchronized void addNewJob(OaiTargetJob job) {     
        job.setStatus(STATUS.RUNNING);
        runningJobsMap.put(job.getId(), job);                
    }

    /*
     * Change status and move from running map to completed map
     */
    public static synchronized void finishJob(OaiTargetJob job, int numberOfRecords, boolean error) {

        runningJobsMap.remove(job.getId());        
        if (error) {
            job.setError(true);            
        }        
        job.setStatus(STATUS.COMPLETED);
        job.setRecordsHarvested(numberOfRecords);
        job.setCompletedTime(System.currentTimeMillis());        
        log.info("Setting completed for job:"+job.getDto().getName() +" time:"+job.getCompletedTime());
    
        //We do not want more than 1000 completed jobs 
        if (completedJobsMap.size() > 1000) {
            completedJobsMap.remove(completedJobsMap.firstKey()); //Remove oldest
        }
        
        completedJobsMap.put(job.getId(), job);        
        
    }

    public static List<OaiJobDto> getRunningJobsMostRecentFirst(){    
        ArrayList<OaiTargetJob>  runningJobs =new  ArrayList<OaiTargetJob>(runningJobsMap.values());        
        List<OaiTargetJob> sorted =runningJobs.stream()
                .sorted(Comparator.comparing(OaiTargetJob::getId).reversed())
                .collect(Collectors.toList());                

        return convertToDto(sorted);        
    }

    public static List<OaiJobDto> getCompletedJobsMostRecentFirst(){    
        ArrayList<OaiTargetJob>  completedJobs =new  ArrayList<OaiTargetJob>(completedJobsMap.values());      
        List<OaiTargetJob> sorted =completedJobs.stream()
                .sorted(Comparator.comparing(OaiTargetJob::getId).reversed())
                .collect(Collectors.toList());                
        return convertToDto(sorted);        
    }

    //Convert to the smaller OaiJobDto with much fewer attributes. (and no urls/user/passwords etc. for the target)
    public static List<OaiJobDto> convertToDto(List<OaiTargetJob> jobs){

        List<OaiJobDto> dtoList = new  ArrayList<OaiJobDto>();
        for (OaiTargetJob targetJob : jobs) {         
            OaiJobDto jobDto = new OaiJobDto();        
            jobDto.setId(targetJob.getId());
            jobDto.setName(targetJob.getDto().getName());
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
        Collection<OaiTargetJob> running = runningJobsMap.values();
        for (OaiTargetJob job : running) {            
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

