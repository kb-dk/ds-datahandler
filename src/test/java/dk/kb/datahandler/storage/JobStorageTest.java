package dk.kb.datahandler.storage;

import dk.kb.datahandler.config.ServiceConfig;
import dk.kb.datahandler.model.v1.CategoryDto;
import dk.kb.datahandler.model.v1.JobDto;
import dk.kb.datahandler.model.v1.JobStatusDto;
import dk.kb.datahandler.model.v1.TypeDto;
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
        JobDto jobDto = genetrateJobDto();
        UUID jobId = storage.createJob(jobDto);

        JobDto jobDtoFromDb = storage.getJob(jobId);

        Assertions.assertNotNull(jobDtoFromDb);
        Assertions.assertEquals(jobId, jobDtoFromDb.getId());
        Assertions.assertEquals(jobDto.getType(), jobDtoFromDb.getType());
        Assertions.assertEquals(jobDto.getCategory(), jobDtoFromDb.getCategory());
        Assertions.assertEquals(jobDto.getSource(), jobDtoFromDb.getSource());
        Assertions.assertEquals(jobDto.getCreatedBy(), jobDtoFromDb.getCreatedBy());
        Assertions.assertEquals(jobDto.getJobStatus(), jobDtoFromDb.getJobStatus());
        Assertions.assertNull(jobDtoFromDb.getErrorCorrelationId());
        Assertions.assertNull(jobDtoFromDb.getMessage());
        Assertions.assertEquals(jobDto.getmTimeFrom(), jobDtoFromDb.getmTimeFrom());
        Assertions.assertNotNull(jobDtoFromDb.getStartTime());
        // Assert that result is 'close enough'
        Assertions.assertTrue(Duration.between(jobDto.getStartTime(), jobDtoFromDb.getStartTime()).toSeconds() <= 0);
        Assertions.assertNull(jobDtoFromDb.getEndTime());
        Assertions.assertNull(jobDtoFromDb.getNumberOfRecords());
        Assertions.assertNull(jobDtoFromDb.getRestartValue());
    }

    @Test
    public void testUpdateJob() throws SQLException {
        JobDto jobDto = genetrateJobDto();
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
        JobDto updatedJobDtoFromDb = storage.getJob(jobDtoFromDb.getId());
        Assertions.assertNotNull(updatedJobDtoFromDb);
        Assertions.assertEquals(jobId, updatedJobDtoFromDb.getId());
        Assertions.assertEquals(jobDto.getType(), updatedJobDtoFromDb.getType());
        Assertions.assertEquals(jobDto.getCategory(), updatedJobDtoFromDb.getCategory());
        Assertions.assertEquals(jobDto.getSource(), updatedJobDtoFromDb.getSource());
        Assertions.assertEquals(jobDto.getCreatedBy(), updatedJobDtoFromDb.getCreatedBy());
        Assertions.assertEquals(jobDtoFromDb.getJobStatus(), updatedJobDtoFromDb.getJobStatus());
        Assertions.assertEquals(jobDto.getErrorCorrelationId(), updatedJobDtoFromDb.getErrorCorrelationId());
        Assertions.assertEquals(jobDtoFromDb.getMessage(), updatedJobDtoFromDb.getMessage());
        Assertions.assertEquals(jobDto.getmTimeFrom(), updatedJobDtoFromDb.getmTimeFrom());
        Assertions.assertNotNull(updatedJobDtoFromDb.getStartTime());
        // Assert that result is 'close enough'
        Assertions.assertTrue(Duration.between(jobDto.getStartTime(), updatedJobDtoFromDb.getStartTime()).toSeconds() <= 0);
        Assertions.assertNotNull(updatedJobDtoFromDb.getEndTime());
        // Assert that result is 'close enough'
        Assertions.assertTrue(Duration.between(jobDtoFromDb.getEndTime(), updatedJobDtoFromDb.getEndTime()).toSeconds() <= 0);
        Assertions.assertEquals(jobDtoFromDb.getNumberOfRecords(), updatedJobDtoFromDb.getNumberOfRecords());
        Assertions.assertEquals(jobDtoFromDb.getRestartValue(), updatedJobDtoFromDb.getRestartValue());
    }

    @Test
    public void testHasJobRunning() throws SQLException {
        JobDto jobDto = genetrateJobDto();
        Assertions.assertFalse(storage.hasRunningJob(CategoryDto.OAI_HARVEST, "test.source"));
        jobDto.setCategory(CategoryDto.OAI_HARVEST);
        jobDto.setSource("test.source");
        jobDto.setJobStatus(JobStatusDto.RUNNING);
        storage.createJob(jobDto);
        Assertions.assertTrue(storage.hasRunningJob(CategoryDto.OAI_HARVEST, "test.source"));
    }

    @NotNull
    private static JobDto genetrateJobDto() {
        JobDto jobDto = new JobDto();
        jobDto.setType(TypeDto.DELTA);
        jobDto.setCategory(CategoryDto.OAI_HARVEST);
        jobDto.setCreatedBy("Unit test");
        jobDto.setJobStatus(JobStatusDto.RUNNING);
        jobDto.setStartTime(OffsetDateTime.now());
        jobDto.setmTimeFrom(1234567890);
        return jobDto;
    }
}
