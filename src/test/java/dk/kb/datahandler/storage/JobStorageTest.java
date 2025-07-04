package dk.kb.datahandler.storage;

import dk.kb.datahandler.config.ServiceConfig;
import dk.kb.datahandler.model.v1.JobDto;
import dk.kb.datahandler.model.v1.JobStatusDto;
import dk.kb.datahandler.model.v1.JobTypeDto;
import dk.kb.datahandler.util.H2DbUtil;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.sql.SQLException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

public class JobStorageTest {
    protected static final String TEST_CLASSES_PATH = new File(Thread.currentThread().getContextClassLoader().getResource("logback-test.xml").getPath()).getParentFile().getAbsolutePath();
    protected static final String DB_URL = "jdbc:h2:"+TEST_CLASSES_PATH+"/h2/ds_datahandler;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE";
    private static final String DRIVER = "org.h2.Driver";
    private static final String USERNAME = "";
    private static final String PASSWORD = "";
    private static JobStorage storage = null;

    @BeforeAll
    public static void beforeClass() throws Exception {
        ServiceConfig.initialize("conf/ds-datahandler*.yaml");
        H2DbUtil.createEmptyH2DBFromDDL(DB_URL, DRIVER, USERNAME, PASSWORD);
        JobStorage.initialize(DRIVER, DB_URL, USERNAME, PASSWORD);
        storage = new JobStorage();
    }

    @Test
    public void testCreateJob() throws SQLException {
        JobDto jobDto = genetrateTestJobDto();
        UUID jobId = storage.createJob(jobDto);

        JobDto jobDtoFromDb = storage.getJob(jobId);

        Assertions.assertNotNull(jobDtoFromDb);
        Assertions.assertEquals(jobId, jobDtoFromDb.getId());
        Assertions.assertEquals(jobDto.getJobName(), jobDtoFromDb.getJobName());
        Assertions.assertEquals(jobDto.getJobStatus(), jobDtoFromDb.getJobStatus());
        Assertions.assertEquals(jobDto.getJobType(), jobDtoFromDb.getJobType());
        Assertions.assertEquals(jobDto.getCreatedBy(), jobDtoFromDb.getCreatedBy());
        Assertions.assertNotNull(jobDtoFromDb.getStartTime());
        // Assert that result is 'close enough'
        Assertions.assertTrue(Duration.between(jobDto.getStartTime(), jobDtoFromDb.getStartTime()).toSeconds() <= 0);
        Assertions.assertNull(jobDtoFromDb.getEndTime());
        Assertions.assertEquals(jobDto.getErrorCorrelationId(), jobDtoFromDb.getErrorCorrelationId());
        Assertions.assertEquals(jobDto.getMessage(), jobDtoFromDb.getMessage());
        Assertions.assertEquals(jobDto.getmTimeFrom(), jobDtoFromDb.getmTimeFrom());
        Assertions.assertNull(jobDtoFromDb.getNumberOfRecords());
        Assertions.assertEquals(jobDto.getNumberOfRecords(), jobDtoFromDb.getNumberOfRecords());
        Assertions.assertNull(jobDtoFromDb.getRestartValue());
        Assertions.assertEquals(jobDto.getRestartValue(), jobDtoFromDb.getRestartValue());
    }

    @Test
    public void testUpdateJob() throws SQLException {
        JobDto jobDto = genetrateTestJobDto();
        UUID jobId = storage.createJob(jobDto);

        JobDto jobDtoFromDb = storage.getJob(jobId);
        Assertions.assertNotNull(jobDtoFromDb);
        jobDtoFromDb.setJobStatus(JobStatusDto.COMPLETED);
        jobDtoFromDb.setEndTime(OffsetDateTime.now());
        jobDtoFromDb.setNumberOfRecords(777777);
        jobDtoFromDb.setRestartValue(888888);
        jobDtoFromDb.setMessage("The job has ended");

        int numberOfUpdatedRows = storage.updateJob(jobDtoFromDb);

        Assertions.assertEquals(1, numberOfUpdatedRows);
        JobDto jobDtoFromDb2 = storage.getJob(jobDtoFromDb.getId());
        Assertions.assertNotNull(jobDtoFromDb2);
        Assertions.assertEquals(jobId, jobDtoFromDb2.getId());
        Assertions.assertEquals(jobDtoFromDb.getJobName(), jobDtoFromDb2.getJobName());
        Assertions.assertEquals(jobDtoFromDb.getJobStatus(), jobDtoFromDb2.getJobStatus());
        Assertions.assertEquals(jobDtoFromDb.getJobType(), jobDtoFromDb2.getJobType());
        Assertions.assertEquals(jobDtoFromDb.getCreatedBy(), jobDtoFromDb2.getCreatedBy());
        Assertions.assertNotNull(jobDtoFromDb.getStartTime());
        // Assert that result is 'close enough'
        Assertions.assertTrue(Duration.between(jobDtoFromDb.getStartTime(), jobDtoFromDb.getStartTime()).toSeconds() <= 0);
        Assertions.assertNotNull(jobDtoFromDb.getEndTime());
        // Assert that result is 'close enough'
        Assertions.assertTrue(Duration.between(jobDtoFromDb.getEndTime(), jobDtoFromDb.getEndTime()).toSeconds() <= 0);

        Assertions.assertEquals(jobDtoFromDb.getErrorCorrelationId(), jobDtoFromDb2.getErrorCorrelationId());
        Assertions.assertEquals(jobDtoFromDb.getMessage(), jobDtoFromDb2.getMessage());
        Assertions.assertEquals(jobDtoFromDb.getmTimeFrom(), jobDtoFromDb2.getmTimeFrom());
        Assertions.assertEquals(jobDtoFromDb.getNumberOfRecords(), jobDtoFromDb2.getNumberOfRecords());
        Assertions.assertEquals(jobDtoFromDb.getRestartValue(), jobDtoFromDb2.getRestartValue());
    }

    @Test
    public void testHasJobRunning() throws SQLException {
        JobDto jobDto = genetrateTestJobDto();
        Assertions.assertFalse(storage.hasRunningJob(JobTypeDto.KALTURA_UPLOAD));
        jobDto.setJobType(JobTypeDto.KALTURA_UPLOAD);
        jobDto.setJobStatus(JobStatusDto.RUNNING);
        storage.createJob(jobDto);
        Assertions.assertTrue(storage.hasRunningJob(JobTypeDto.KALTURA_UPLOAD));
    }

    @NotNull
    private static JobDto genetrateTestJobDto() {
        JobDto jobDto = new JobDto();
        jobDto.setJobName("full " + JobTypeDto.KALTURA_UPLOAD.getValue());
        jobDto.setJobStatus(JobStatusDto.CREATED);
        jobDto.setJobType(JobTypeDto.KALTURA_UPLOAD);
        jobDto.setCreatedBy("Unit test");
        jobDto.setStartTime(OffsetDateTime.now());
        jobDto.setErrorCorrelationId(UUID.randomUUID());
        jobDto.setMessage("This is a message");
        jobDto.setmTimeFrom(1234567890);
        return jobDto;
    }
}
