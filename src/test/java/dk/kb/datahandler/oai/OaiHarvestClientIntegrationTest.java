package dk.kb.datahandler.oai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import dk.kb.datahandler.facade.DsDatahandlerFacade;
import dk.kb.datahandler.model.v1.OaiTargetDto;


/**
 * This is an integration test that will call an external OAI service.
 * The unittest is marked with the integrationTest tag
 * 
 */
public class OaiHarvestClientIntegrationTest {


    @Tag("integration")
    @Test
    void testHarvest1000OaiRecords() throws Exception {

        //example: http://www5.kb.dk/cop/oai/?metadataPrefix=mods&set=oai:kb.dk:images:billed:2010:okt:billeder&verb=ListRecords
        //When using resumption token you must NOT include metadatPrefix+set again
        //http://www5.kb.dk/cop/oai/?verb=ListRecords&resumptionToken=KB!1000!mods!0001-01-01!9999-12-31!oai:kb.dk:images:billed:2010:okt:billeder
        
        String set="oai:kb.dk:images:billed:2010:okt:billeder";
        //String set="oai:kb.dk:images:billed:2014:jun:hca";
        
        OaiTargetDto oaiTarget = new OaiTargetDto();       
        oaiTarget.setUrl("http://www5.kb.dk/cop/oai/"); //Public KB service
        oaiTarget.setMetadataprefix("mods");               
        oaiTarget.setSet(set);
        oaiTarget.setUsername(null);
        oaiTarget.setPassword(null);;
        oaiTarget.setDatasource("test");
        String from=null;

        /*
        String baseUrl="https://pvica-devel2.statsbiblioteket.dk/OAI-PMH/";
        String metadataPrefix="XIP_full_schema";               
        String set=null;
        String user = "oai-pmh-devel";
        String password="XXXX"; //find it yourself
        String from ="2021-01-01";
         */

        OaiTargetJob job = DsDatahandlerFacade.createNewJob(oaiTarget);        

        OaiHarvestClient client = new OaiHarvestClient(job,from,null);
        OaiResponse r1 = client.next();
        assertEquals(1000, r1.getRecords().size());
        assertNotNull(r1.getResumptionToken());

        //Fetch next 1000        
        OaiResponse r2= client.next();
        assertEquals(1000, r2.getRecords().size());
    }

}
