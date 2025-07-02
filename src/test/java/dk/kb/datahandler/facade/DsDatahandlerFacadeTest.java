package dk.kb.datahandler.facade;



import static org.junit.jupiter.api.Assertions.fail;

import dk.kb.datahandler.model.v1.JobDto;
import org.junit.jupiter.api.Test;

import dk.kb.datahandler.model.v1.OaiTargetDto;
import dk.kb.datahandler.job.JobCache;

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
        JobDto jobDto = JobCache.createNewOaiJob(oaiTarget, null);

        // Try add another
        try {
            DsDatahandlerFacade.oaiIngestFull(oaiTarget.getName());
            fail("Two jobs with same name was started");
        } catch (Exception e) {
            // ignore
        }

    }

}
