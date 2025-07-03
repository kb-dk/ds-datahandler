package dk.kb.datahandler.storage;

import dk.kb.datahandler.config.ServiceConfig;
import dk.kb.datahandler.model.v1.JobDto;
import dk.kb.datahandler.model.v1.JobStatusDto;
import dk.kb.datahandler.model.v1.JobTypeDto;
import dk.kb.datahandler.util.H2DbUtil;
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
    private static JobStorage storage = null ;

    @BeforeAll
    public static void beforeClass() throws Exception {
        ServiceConfig.initialize("conf/ds-datahandler*.yaml");
        H2DbUtil.createEmptyH2DBFromDDL(DB_URL,DRIVER,USERNAME,PASSWORD);
        JobStorage.initialize(DRIVER,DB_URL,USERNAME,PASSWORD);
        storage = new JobStorage();
    }

    @Test
    public void testCreateJob() throws SQLException {
        JobDto jobDto = new JobDto();
        jobDto.setJobName("jobName");
        jobDto.setJobStatus(JobStatusDto.CREATED);
        jobDto.setJobType(JobTypeDto.KALTURA_UPLOAD);
        jobDto.setCreatedBy("Unit test");
        jobDto.setStartTime(OffsetDateTime.now());
        jobDto.setErrorCorrelationId(UUID.randomUUID());
        jobDto.setMessage("This is a message");
        jobDto.setmTimeFrom(1234567890);
        jobDto.setNumberOfRecords(4200);
        jobDto.setRestartValue(12345);
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
        Assertions.assertEquals(jobDto.getErrorCorrelationId(), jobDtoFromDb.getErrorCorrelationId());
        Assertions.assertEquals(jobDto.getErrorCorrelationId(), jobDtoFromDb.getErrorCorrelationId());
        Assertions.assertEquals(jobDto.getMessage(), jobDtoFromDb.getMessage());
        Assertions.assertEquals(jobDto.getmTimeFrom(), jobDtoFromDb.getmTimeFrom());
        Assertions.assertEquals(jobDto.getNumberOfRecords(), jobDtoFromDb.getNumberOfRecords());
        Assertions.assertEquals(jobDto.getRestartValue(), jobDtoFromDb.getRestartValue());
    }

}
