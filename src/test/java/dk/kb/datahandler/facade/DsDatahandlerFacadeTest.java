package dk.kb.datahandler.facade;



import static org.junit.jupiter.api.Assertions.fail;

import dk.kb.datahandler.model.v1.*;
import dk.kb.datahandler.storage.BasicStorage;
import dk.kb.datahandler.storage.JobStorage;
import org.junit.jupiter.api.Test;

public class DsDatahandlerFacadeTest {

    @Test
    void testOnly1JobWithSameName() throws Exception {
        
        OaiTargetDto oaiTarget = new OaiTargetDto()
                .url("http://www5.kb.dk/cop/oai/")
                .metadataprefix("mods")
                .set("oai:kb.dk:images:billed:2010:okt:billeder")
                .username(null).password(null)
                .datasource("test")
                .name("THERE_CAN_ONLY_BE_ONE");
        
        // OK
        JobDto jobDto = new JobDto();
        jobDto.setType(TypeDto.DELTA);
        jobDto.category(CategoryDto.OAI_HARVEST);
        jobDto.setSource("THERE_CAN_ONLY_BE_ONE");
        jobDto.setJobStatus(JobStatusDto.RUNNING);
        BasicStorage.performStorageAction("Create job for OAITest", JobStorage::new, (JobStorage storage) -> {
            storage.createJob(jobDto);
            return null;
        });

        // Try add another
        try {
            DsDatahandlerFacade.oaiIngestFull(oaiTarget.getName(),"unit test");
            fail("Two jobs with same name was started");
        } catch (Exception e) {
            // ignore
        }

    }

}
