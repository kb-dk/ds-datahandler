package dk.kb.datahandler.facade;

import dk.kb.datahandler.config.ServiceConfig;
import dk.kb.datahandler.model.v1.*;
import dk.kb.datahandler.storage.BasicStorage;
import dk.kb.datahandler.storage.JobStorage;
import dk.kb.datahandler.storage.JobStorageForUnitTests;
import dk.kb.datahandler.util.H2DbUtil;
import dk.kb.storage.util.DsStorageClient;
import dk.kb.util.webservice.exception.InvalidArgumentServiceException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.File;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

public class DsDatahandlerFacadeTest {
    protected static final String TEST_CLASSES_PATH = new File(Thread.currentThread().getContextClassLoader().getResource("logback-test.xml").getPath()).getParentFile().getAbsolutePath();
    protected static final String DB_URL = "jdbc:h2:" + TEST_CLASSES_PATH + "/h2/ds_datahandler;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE";
    private static final String DRIVER = "org.h2.Driver";
    private static final String USERNAME = "";
    private static final String PASSWORD = "";
    private static JobStorageForUnitTests storage = null;

    @BeforeAll
    public static void beforeClass() throws Exception {
        ServiceConfig.initialize("conf/ds-datahandler-behaviour.yaml");
        H2DbUtil.createEmptyH2DBFromDDL(DB_URL, DRIVER, USERNAME, PASSWORD);
        JobStorage.initialize(DRIVER, DB_URL, USERNAME, PASSWORD);

        storage = new JobStorageForUnitTests();
    }

    @BeforeEach
    public void beforeEach() throws Exception {
        storage.clearTables();
    }

    @Test
    public void updateRerunClustersTable_whenNewData_thenReturnHowManyRowsWasInsertedOrUpdated() {
        // Arrange
        String username = "unittest";
        Integer count = 2;
        dk.kb.storage.model.v1.RecordsCountDto recordsCountDto = new dk.kb.storage.model.v1.RecordsCountDto();
        recordsCountDto.setCount(count);

        DsStorageClient dsStorageClient = Mockito.mock(DsStorageClient.class);

        try (MockedStatic<DsDatahandlerFacade> mockedDsDatahandlerFacade = mockStatic(DsDatahandlerFacade.class, Mockito.CALLS_REAL_METHODS)) {
            mockedDsDatahandlerFacade.when(DsDatahandlerFacade::getDsStorageApiClient).thenReturn(dsStorageClient);

            Mockito.when(dsStorageClient.updateRerunClustersTable()).thenReturn(recordsCountDto);

            // Act
            RecordsCountDto returnedRecordsCountDto = DsDatahandlerFacade.updateRerunClustersTable(username);

            // Assert
            assertNotNull(returnedRecordsCountDto);
            assertEquals(count, returnedRecordsCountDto.getCount());

            List<JobDto> actualJobDtoList = DsDatahandlerFacade.getJobs(null, null);
            assertEquals(1, actualJobDtoList.size());

            JobDto returnedJobDto = actualJobDtoList.get(0);

            assertNotNull(returnedJobDto.getId());
            assertEquals(TypeDto.DELTA, returnedJobDto.getType());
            assertEquals(CategoryDto.RERUN_CLUSTERS, returnedJobDto.getCategory());
            assertNull(returnedJobDto.getSource());
            assertEquals(username, returnedJobDto.getCreatedBy());
            assertEquals(JobStatusDto.COMPLETED, returnedJobDto.getJobStatus());
            assertNull(returnedJobDto.getErrorCorrelationId());
            assertNull(returnedJobDto.getMessage());
            assertNull(returnedJobDto.getModifiedTimeFrom());

            assertNotNull(returnedJobDto.getStartTime());
            assertEquals(OffsetDateTime.class, returnedJobDto.getStartTime().getClass());
            assertNotNull(returnedJobDto.getEndTime());
            assertEquals(OffsetDateTime.class, returnedJobDto.getEndTime().getClass());
            assertEquals(count, returnedJobDto.getNumberOfRecords());
            assertNull(returnedJobDto.getRestartValue());
        }
    }

    @Test
    public void getJobs_whenJobExistInTable_thenReturnListOfAllJobs() {
        // Arrange
        OaiTargetDto oaiTarget = ServiceConfig.getOaiTargets().get("ds.radiotv");

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

        // Act
        // List of all jobs saved in database
        List<JobDto> actualJobDtoList = DsDatahandlerFacade.getJobs(null, null);

        // Assert
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
    public void startJob_whenThereIsAlreadyRunningJobWithTheSameName_thenThrowInvalidArgumentServiceException() {
        // Arrange
        String user = "Unit test user";
        JobDto jobDto = new JobDto();

        jobDto.setType(TypeDto.DELTA);
        jobDto.category(CategoryDto.KALTURA_UPLOAD);
        jobDto.setSource(null);
        jobDto.setCreatedBy(user);
        jobDto.setJobStatus(JobStatusDto.RUNNING);
        jobDto.setStartTime(OffsetDateTime.now(ZoneOffset.UTC));

        BasicStorage.performStorageAction("Create job for kaltura upload test", JobStorage::new, (JobStorage storage) -> {
            storage.createJob(jobDto);
            return null;
        });

        // Act/Assert
        InvalidArgumentServiceException exception = Assertions.assertThrows(InvalidArgumentServiceException.class,
                () -> DsDatahandlerFacade.kalturaDeltaUpload(user)
        );

        Assertions.assertEquals("There is already a/an kaltura upload job running", exception.getMessage());
    }
}
