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

import dk.kb.datahandler.model.v1.JobDto;
import dk.kb.datahandler.model.v1.OaiTargetDto;
import dk.kb.util.webservice.exception.InvalidArgumentServiceException;

/*
 * Access to all methods working with the maps needs to be syncronized to prevent concurrent modification error. 
 * 
 */
public class JobCache {

    private static HashMap<Long, JobDto> runningJobsMap = new HashMap<Long, JobDto>();
    private static TreeMap<Long, JobDto> completedJobsMap = new TreeMap<Long, JobDto>(); // ordered
    private static Logger log = LoggerFactory.getLogger(JobCache.class);

    private JobCache() { // No need for constructor
    }

    enum STATUS {
        RUNNING, COMPLETED
    }

    /**
     * Add a new job to running job list. If a job with same name is already running
     * will throw exception.
     * 
     * @param JobDto jobDto information about the job
     * @throws InvalidArgumentServiceException If a job with same name is already in
     *                                         RUNNING status.
     * 
     */
    public static synchronized void addNewJob(JobDto jobDto) throws InvalidArgumentServiceException {
//        validateNotAlreadyRunning(jobDto.getName());
      //  jobDto.setStatus(STATUS.RUNNING.toString());
//        jobDto.setStartedTime(formatSystemMillis(System.currentTimeMillis()));
      //  runningJobsMap.put(jobDto.getId(), jobDto);
    }

    /*
     * Change status and move from running map to completed map
     */
    public static synchronized void finishJob(JobDto jobDto, int numberOfRecords, boolean error) {
   //     jobDto.setCompletedTime(JobCache.formatSystemMillis(System.currentTimeMillis()));
        runningJobsMap.remove(jobDto.getId());
        if (error) {
  //          jobDto.setError(true);
        }
  //      jobDto.setStatus(STATUS.COMPLETED.toString());
        jobDto.setNumberOfRecords(numberOfRecords);
  //      jobDto.setCompletedTime(formatSystemMillis(System.currentTimeMillis()));
  //      log.info("Setting completed for jobDto:" + jobDto.getName() + " time:" + jobDto.getCompletedTime());

   //     completedJobsMap.put(jobDto.getId(), jobDto);

    }

    public static synchronized List<JobDto> getRunningJobsMostRecentFirst() {
        ArrayList<JobDto> runningJobs = new ArrayList<JobDto>(runningJobsMap.values());

        // Sort with most recent first.
        Collections.sort(runningJobs, Comparator.comparing(JobDto::getId, Comparator.reverseOrder()));
        return runningJobs;
    }

    public static synchronized List<JobDto> getCompletedJobsMostRecentFirst() {
        ArrayList<JobDto> completedJobs = new ArrayList<JobDto>(completedJobsMap.values());

        // Sort with most recent first.
        Collections.sort(completedJobs, Comparator.comparing(JobDto::getId, Comparator.reverseOrder()));
        return completedJobs;
    }

    public static synchronized boolean isJobRunningForTarget(String targetName) {
        Collection<JobDto> running = runningJobsMap.values();
        for (JobDto jobDto : running) {
//            if (jobDto.getName().equals(targetName)) {
//                return true;
//            }
        }
        return false;
    }

    public static String formatSystemMillis(long millis) {
        LocalDateTime myDateObj = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDateTime();
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss", Locale.getDefault());
        String formattedDate = myDateObj.format(dateFormat);
        return formattedDate;

    }

    private static synchronized void validateNotAlreadyRunning(String jobName) {
        boolean alreadyRunning = JobCache.isJobRunningForTarget(jobName);
        if (alreadyRunning) {
            throw new InvalidArgumentServiceException("There is already a job running with name:" + jobName);
        }
    }

    /**
     * Generates a {@link JobDto from a {@link OaiTargetDto}.
     * <p>
     * The job will have a unique timestamp used as ID.
     * 
     */
    public static synchronized JobDto createNewOaiJob(OaiTargetDto dto, String from) {

        long id = JobCache.getNextId();

        JobDto jobDto = new JobDto();
 //       jobDto.setId(id);
 //       jobDto.setName("OAI:" + dto.getName()); // name is key in job cache. Only start one OAI from each target.
 //       jobDto.setType("OAI");
 //       jobDto.setFrom(from);
        // register job
        JobCache.addNewJob(jobDto);

        return jobDto;
    }

    /**
     * Generates a {@link JobDto from a the origin for solr indexing}.
     * <p>
     * The job will have a unique timestamp used as ID.
     * 
     */
    public static synchronized JobDto createIndexSolrJob(String origin, Long mTime) {

        long id = JobCache.getNextId();

        JobDto jobDto = new JobDto();
 //       jobDto.setId(id);
 //       jobDto.setName("SOLR_INDEX:" + origin); // name is key in job cache. Only one job with this name can be started
 //       jobDto.setType("SOLR_INDEX");
 //       jobDto.setFrom(parseMTime(mTime));
        // register job
        JobCache.addNewJob(jobDto);

        return jobDto;
    }

    /**
     * Generates a {@link JobDto from a the origin for manifestation
     * enrichment}.
     * <p>
     * The job will have a unique timestamp used as ID.
     * 
     */
    public static synchronized JobDto createPreservicaManifestationJob(String origin, Long mTime) {
        long id = JobCache.getNextId();

        JobDto jobDto = new JobDto();
//        jobDto.setId(id);
 //       jobDto.setName("PRESERVICA_MANIFESTATION:" + origin); // name is key in job cache. Only one job with this name can be started.
//        jobDto.setType("PRESERVICA_MANIFESTATION");
//        jobDto.setFrom(parseMTime(mTime));

        // register job
        JobCache.addNewJob(jobDto);

        return jobDto;
    }

    /**
     * Generates a {@link JobDto from a the origin for solr indexing}.
     * <p>
     * The job will have a unique timestamp used as ID.
     * 
     */
    public static synchronized JobDto createKalturaEnrichmentJob(String origin, Long mTime) {

        long id = JobCache.getNextId();

        JobDto jobDto = new JobDto();
//        jobDto.setId(id);
//        jobDto.setName("KALTURA:" + origin); // name is key in job cache. Only one job with this name can be started.
//        jobDto.setType("KALTURA_ENTRY_ID");
//        jobDto.setFrom(parseMTime(mTime));

        // register job
        JobCache.addNewJob(jobDto);

        return jobDto;
    }

    /**
     * Generates a {@link JobDto for KalturaDeltaUpload}.
     * <p>
     * The job will have a unique timestamp used as ID.
     * 
     */
    public static synchronized JobDto createKalturaDeltaUploadJob(Long mTime) {

        long id = JobCache.getNextId();

        JobDto jobDto = new JobDto();
//        jobDto.setId(id);
//        jobDto.setName("KALTURA DELTAUPLOAD"); // name is key in job cache. Only one job with this name can be started.
//        jobDto.setType("KALTURA_DELTA_UPLOAD");
//        jobDto.setFrom(parseMTime(mTime));

        // register job
        JobCache.addNewJob(jobDto);

        return jobDto;
    }

    private static String parseMTime(Long mTime) {
        return (mTime == null) ? "0" : "" + mTime;
    }

    /**
     * Syncronized method to make sure all ID's are different.
     * 
     * @return system.currentTime in millis.
     */

    public static synchronized long getNextId() {
        long id = System.currentTimeMillis();
        try {
            sleep(1); // So next ID is different.
        } catch (Exception e) {
            // can not happen, nothing will interrupt.
        }

        return id;

    }
}
