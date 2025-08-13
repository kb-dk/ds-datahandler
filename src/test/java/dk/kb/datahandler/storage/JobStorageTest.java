package dk.kb.datahandler.storage;

import dk.kb.datahandler.config.ServiceConfig;
import dk.kb.datahandler.model.v1.CategoryDto;
import dk.kb.datahandler.model.v1.JobDto;
import dk.kb.datahandler.model.v1.JobStatusDto;
import dk.kb.datahandler.model.v1.TypeDto;
import dk.kb.datahandler.util.H2DbUtil;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.sql.SQLException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class JobStorageTest {
    protected static final String TEST_CLASSES_PATH = new File(Thread.currentThread().getContextClassLoader().getResource("logback-test.xml").getPath()).getParentFile().getAbsolutePath();
    protected static final String DB_URL = "jdbc:h2:" + TEST_CLASSES_PATH + "/ds_datahandler;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE";
    private static final String DRIVER = "org.h2.Driver";
    private static final String USERNAME = "";
    private static final String PASSWORD = "";
    private static JobStorageForUnitTests storage = null;

    @BeforeAll
    public static void beforeClass() throws Exception {
        ServiceConfig.initialize("ds-datahandler-unittest.yaml");
        H2DbUtil.createEmptyH2DBFromDDL(DB_URL, DRIVER, USERNAME, PASSWORD);
        JobStorage.initialize(DRIVER, DB_URL, USERNAME, PASSWORD);
        storage = new JobStorageForUnitTests();
    }

    @BeforeEach
    public void beforeTest() throws SQLException {
        storage.clearTables();
    }

    @Test
    public void testCreateJob() throws SQLException {
        JobDto jobDto = genetrateJobDto();
        UUID jobId = storage.createJob(jobDto);

        List<JobDto> listJobDtoFromDb = storage.getJobs(jobDto.getCategory(), jobDto.getJobStatus());

        assertNotNull(listJobDtoFromDb);
        assertEquals(1, listJobDtoFromDb.size());

        JobDto jobDtoFromDb = listJobDtoFromDb.get(0);

        assertNotNull(jobDtoFromDb);
        assertEquals(jobId, jobDtoFromDb.getId());
        assertEquals(jobDto.getType(), jobDtoFromDb.getType());
        assertEquals(jobDto.getCategory(), jobDtoFromDb.getCategory());
        assertEquals(jobDto.getSource(), jobDtoFromDb.getSource());
        assertEquals(jobDto.getCreatedBy(), jobDtoFromDb.getCreatedBy());
        assertEquals(jobDto.getJobStatus(), jobDtoFromDb.getJobStatus());
        assertNull(jobDtoFromDb.getErrorCorrelationId());
        assertNull(jobDtoFromDb.getMessage());

        assertNotNull(jobDtoFromDb.getModifiedTimeFrom());
        // Assert that result is 'close enough'
        assertTrue(Duration.between(jobDto.getModifiedTimeFrom(), jobDtoFromDb.getModifiedTimeFrom()).toSeconds() <= 0);

        assertNotNull(jobDtoFromDb.getStartTime());
        // Assert that result is 'close enough'
        assertTrue(Duration.between(jobDto.getStartTime(), jobDtoFromDb.getStartTime()).toSeconds() <= 0);

        assertNull(jobDtoFromDb.getEndTime());
        assertNull(jobDtoFromDb.getNumberOfRecords());
        assertNull(jobDtoFromDb.getRestartValue());
    }

    @Test
    public void testUpdateJob() throws SQLException {
        JobDto jobDto = genetrateJobDto();
        UUID jobId = storage.createJob(jobDto);

        List<JobDto> listJobDtoFromDb = storage.getJobs(jobDto.getCategory(), jobDto.getJobStatus());

        assertNotNull(listJobDtoFromDb);
        assertEquals(1, listJobDtoFromDb.size());

        JobDto jobDtoFromDb = listJobDtoFromDb.get(0);

        assertNotNull(jobDtoFromDb);

        // Update job to be completed
        jobDtoFromDb.setJobStatus(JobStatusDto.COMPLETED);
        jobDtoFromDb.setEndTime(OffsetDateTime.now(ZoneOffset.UTC));
        jobDtoFromDb.setNumberOfRecords(777777);
        jobDtoFromDb.setRestartValue(OffsetDateTime.now(ZoneOffset.UTC));
        jobDtoFromDb.setMessage("The job has ended");

        int numberOfUpdatedRows = storage.updateJob(jobDtoFromDb);
        assertEquals(1, numberOfUpdatedRows);

        List<JobDto> listUpdatedJobDtoFromDb = storage.getJobs(jobDtoFromDb.getCategory(), jobDtoFromDb.getJobStatus());

        assertNotNull(listUpdatedJobDtoFromDb);
        assertEquals(1, listUpdatedJobDtoFromDb.size());
        JobDto updatedJobDtoFromDb = listUpdatedJobDtoFromDb.get(0);

        assertNotNull(updatedJobDtoFromDb);
        assertEquals(jobId, updatedJobDtoFromDb.getId());
        assertEquals(jobDto.getType(), updatedJobDtoFromDb.getType());
        assertEquals(jobDto.getCategory(), updatedJobDtoFromDb.getCategory());
        assertEquals(jobDto.getSource(), updatedJobDtoFromDb.getSource());
        assertEquals(jobDto.getCreatedBy(), updatedJobDtoFromDb.getCreatedBy());
        assertEquals(jobDtoFromDb.getJobStatus(), updatedJobDtoFromDb.getJobStatus());
        assertEquals(jobDto.getErrorCorrelationId(), updatedJobDtoFromDb.getErrorCorrelationId());
        assertEquals(jobDtoFromDb.getMessage(), updatedJobDtoFromDb.getMessage());

        assertNotNull(updatedJobDtoFromDb.getModifiedTimeFrom());
        // Assert that result is 'close enough'
        assertTrue(Duration.between(jobDto.getModifiedTimeFrom(), updatedJobDtoFromDb.getModifiedTimeFrom()).toSeconds() <= 0);

        assertNotNull(updatedJobDtoFromDb.getStartTime());
        // Assert that result is 'close enough'
        assertTrue(Duration.between(jobDto.getStartTime(), updatedJobDtoFromDb.getStartTime()).toSeconds() <= 0);

        assertNotNull(updatedJobDtoFromDb.getEndTime());
        // Assert that result is 'close enough'
        assertTrue(Duration.between(jobDtoFromDb.getEndTime(), updatedJobDtoFromDb.getEndTime()).toSeconds() <= 0);

        assertEquals(jobDtoFromDb.getNumberOfRecords(), updatedJobDtoFromDb.getNumberOfRecords());

        assertNotNull(updatedJobDtoFromDb.getRestartValue());
        // Assert that result is 'close enough'
        assertTrue(Duration.between(jobDtoFromDb.getRestartValue(), updatedJobDtoFromDb.getRestartValue()).toSeconds() <= 0);
    }

    @Test
    public void testHasJobRunning() throws SQLException {
        JobDto jobDto = genetrateJobDto();

        // No OAI_HARVEST job should be running
        assertFalse(storage.hasRunningJob(CategoryDto.OAI_HARVEST, "test.source"));

        jobDto.setCategory(CategoryDto.OAI_HARVEST);
        jobDto.setSource("test.source");
        jobDto.setJobStatus(JobStatusDto.RUNNING);
        storage.createJob(jobDto);

        // Now there should be a OIA_HARVEST job running
        assertTrue(storage.hasRunningJob(CategoryDto.OAI_HARVEST, "test.source"));
    }

    @Test
    public void testGetJobs() throws SQLException {
        JobDto jobDto = genetrateJobDto();
        // OAI JOBS
        storage.createJob(jobDto);
        jobDto.setSource("test.source2");
        storage.createJob(jobDto);
        jobDto.setJobStatus(JobStatusDto.COMPLETED);
        storage.createJob(jobDto);

        // SOLR JOBS
        jobDto.setCategory(CategoryDto.SOLR_INDEX);
        storage.createJob(jobDto);
        jobDto.setJobStatus(JobStatusDto.RUNNING);
        storage.createJob(jobDto);

        assertEquals(5, storage.getJobs(null, null).size());
        assertEquals(3, storage.getJobs(CategoryDto.OAI_HARVEST, null).size());
        assertEquals(2, storage.getJobs(CategoryDto.SOLR_INDEX, null).size());
        assertEquals(2, storage.getJobs(null, JobStatusDto.COMPLETED).size());
        assertEquals(1, storage.getJobs(CategoryDto.OAI_HARVEST, JobStatusDto.COMPLETED).size());
    }

    @NotNull
    private static JobDto genetrateJobDto() {
        JobDto jobDto = new JobDto();

        jobDto.setType(TypeDto.DELTA);
        jobDto.setCategory(CategoryDto.OAI_HARVEST);
        jobDto.setCreatedBy("Unit test");
        jobDto.setJobStatus(JobStatusDto.RUNNING);
        jobDto.setStartTime(OffsetDateTime.now(ZoneOffset.UTC));
        jobDto.setModifiedTimeFrom(OffsetDateTime.now(ZoneOffset.UTC));

        return jobDto;
    }
}
