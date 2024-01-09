package dk.kb.datahandler.facade;



import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

import dk.kb.datahandler.model.v1.OaiTargetDto;
import dk.kb.datahandler.oai.OaiJobCache;
import dk.kb.datahandler.oai.OaiTargetJob;

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

    @Test
    void testSolrQuery() throws Exception {
        Long result = DsDatahandlerFacade.getLatestMTimeForOrigin("ds.radio");
        System.out.println(result);
    }

}
