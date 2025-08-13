package dk.kb.datahandler.facade;

import dk.kb.datahandler.config.ServiceConfig;
import dk.kb.datahandler.model.v1.*;
import dk.kb.datahandler.storage.BasicStorage;
import dk.kb.datahandler.storage.JobStorage;
import dk.kb.datahandler.storage.JobStorageForUnitTests;
import dk.kb.datahandler.util.H2DbUtil;
import dk.kb.util.webservice.exception.InvalidArgumentServiceException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DsDatahandlerFacadeTest {
    protected static final String TEST_CLASSES_PATH = new File(Thread.currentThread().getContextClassLoader().getResource("logback-test.xml").getPath()).getParentFile().getAbsolutePath();
    protected static final String DB_URL = "jdbc:h2:" + TEST_CLASSES_PATH + "/h2/ds_datahandler;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE";
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
    public void beforeEach() throws Exception {
        storage.clearTables();
    }

    @Test
    void testGetJobs() {

        OaiTargetDto oaiTarget = ServiceConfig.getOaiTargets().get("test.target");

        JobDto jobDto = new JobDto();
        jobDto.setType(TypeDto.DELTA);
        jobDto.category(CategoryDto.OAI_HARVEST);
        jobDto.setSource(oaiTarget.getName());
        jobDto.setCreatedBy("Unit test");
        jobDto.setJobStatus(JobStatusDto.RUNNING);
        jobDto.setStartTime(OffsetDateTime.now(ZoneOffset.UTC));

        BasicStorage.performStorageAction("Create job for OAITest", JobStorage::new, (JobStorage storage) -> {
            storage.createJob(jobDto);
            return null;
        });

        // List of all jobs saved in database
        List<JobDto> actualJobDtoList = DsDatahandlerFacade.getJobs(null, null);

        assertEquals(1, actualJobDtoList.size());

        JobDto returnedJobDto = actualJobDtoList.get(0);

        assertNotNull(returnedJobDto.getId());
        assertEquals(jobDto.getType(), returnedJobDto.getType());
        assertEquals(jobDto.getCategory(), returnedJobDto.getCategory());
        assertEquals(jobDto.getSource(), returnedJobDto.getSource());
        assertEquals(jobDto.getCreatedBy(), returnedJobDto.getCreatedBy());
        assertEquals(jobDto.getJobStatus(), returnedJobDto.getJobStatus());
        assertNull(returnedJobDto.getErrorCorrelationId());
        assertNull(returnedJobDto.getMessage());
        assertNull(returnedJobDto.getModifiedTimeFrom());

        assertNotNull(returnedJobDto.getStartTime());
        // Assert that result is 'close enough'
        assertTrue(Duration.between(jobDto.getStartTime(), returnedJobDto.getStartTime()).toSeconds() <= 0);

        assertNull(returnedJobDto.getEndTime());
        assertNull(returnedJobDto.getNumberOfRecords());
        assertNull(returnedJobDto.getRestartValue());
    }

    /**
     * Can only have one job with the same name running at the same time even if one is a delta job and the other is a full job
     */
    @Test
    void testOnly1JobWithSameName() {
        
        OaiTargetDto oaiTarget = ServiceConfig.getOaiTargets().get("test.target");
        
        // OK
        JobDto jobDto = new JobDto();
        jobDto.setType(TypeDto.DELTA);
        jobDto.category(CategoryDto.OAI_HARVEST);
        jobDto.setSource(oaiTarget.getName());
        jobDto.setCreatedBy("Unit test");
        jobDto.setJobStatus(JobStatusDto.RUNNING);
        jobDto.setStartTime(OffsetDateTime.now(ZoneOffset.UTC));

        BasicStorage.performStorageAction("Create job for OAITest", JobStorage::new, (JobStorage storage) -> {
            storage.createJob(jobDto);
            return null;
        });

        // Try add another
        InvalidArgumentServiceException exception = Assertions.assertThrows(InvalidArgumentServiceException.class,
                () -> DsDatahandlerFacade.oaiIngestFull(oaiTarget.getName(), "Unit test")
        );

        Assertions.assertEquals("There is already an OAI Harvest job running", exception.getMessage());
    }
}
