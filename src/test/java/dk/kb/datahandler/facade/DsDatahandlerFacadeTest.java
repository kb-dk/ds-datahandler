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
import java.time.Instant;

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
    void testOnly1JobWithSameName() {
        
        OaiTargetDto oaiTarget = ServiceConfig.getOaiTargets().get("test.target");
        
        // OK
        JobDto jobDto = new JobDto();
        jobDto.setType(TypeDto.DELTA);
        jobDto.category(CategoryDto.OAI_HARVEST);
        jobDto.setSource(oaiTarget.getName());
        jobDto.setCreatedBy("Unit test");
        jobDto.setJobStatus(JobStatusDto.RUNNING);
        jobDto.setStartTime(Instant.now());
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
