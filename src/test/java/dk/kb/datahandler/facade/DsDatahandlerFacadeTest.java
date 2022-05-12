package dk.kb.datahandler.facade;

import static org.junit.Assert.fail;

import org.junit.jupiter.api.Test;

import dk.kb.datahandler.model.v1.OaiTargetDto;
import dk.kb.datahandler.oai.OaiJobCache;
import dk.kb.datahandler.oai.OaiTargetJob;

public class DsDatahandlerFacadeTest {

    @Test
    void testOnly1JobWithSameName() throws Exception {

        OaiTargetDto oaiTarget = new OaiTargetDto();
        oaiTarget.setUrl("http://www5.kb.dk/cop/oai/");
        oaiTarget.setMetadataprefix("mods");
        oaiTarget.setSet("oai:kb.dk:images:billed:2010:okt:billeder");
        oaiTarget.setUsername(null);
        oaiTarget.setPassword(null);       
        oaiTarget.setRecordBase("test");
        oaiTarget.setName("THERE_CAN_ONLY_BE_ONE");

        // OK
        OaiTargetJob job = DsDatahandlerFacade.createNewJob(oaiTarget);
        OaiJobCache.addNewJob(job);

        // Try add another
        try {
            DsDatahandlerFacade.oaiIngestFull(oaiTarget.getName());
            fail("Two jobs with same name was started");
        } catch (Exception e) {
            // ignore
        }

    }

}
