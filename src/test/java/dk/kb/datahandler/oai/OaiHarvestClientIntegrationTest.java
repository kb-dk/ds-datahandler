package dk.kb.datahandler.oai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import dk.kb.datahandler.config.ServiceConfig;
import dk.kb.util.yaml.YAML;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import dk.kb.datahandler.job.JobCache;
import dk.kb.datahandler.model.v1.DsDatahandlerJobDto;
import dk.kb.datahandler.model.v1.OaiTargetDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * This is an integration test that will call an external OAI service.
 * The unittest is marked with the integrationTest tag
 * 
 */
public class OaiHarvestClientIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(OaiHarvestClientIntegrationTest.class);

    @BeforeAll
    static void setup() {
        try {
            ServiceConfig.initialize("conf/ds-datahandler-behaviour.yaml", "ds-datahandler-integration-test.yaml");
        } catch (IOException e) {
            e.printStackTrace();
            log.error("Integration yaml 'ds-datahandler-integration-test.yaml' file most be present. Call 'kb init'");
            fail();
        }
    }


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
        oaiTarget.setName("Unittest1");
        String from=null;

        /*
        String baseUrl="https://pvica-devel2.statsbiblioteket.dk/OAI-PMH/";
        String metadataPrefix="XIP_full_schema";               
        String set=null;
        String user = "oai-pmh-devel";
        String password="XXXX"; //find it yourself
        String from ="2021-01-01";
         */

        DsDatahandlerJobDto job = JobCache.createNewOaiJob(oaiTarget,from);        

        OaiHarvestClient client = new OaiHarvestClient(job,oaiTarget,from);
        OaiResponse r1 = client.next();
        assertEquals(1000, r1.getRecords().size());
        assertNotNull(r1.getResumptionToken());

        //Fetch next 1000        
        OaiResponse r2= client.next();
        assertEquals(1000, r2.getRecords().size());
    
        JobCache.finishJob(job, 0, false);
    }


    @Tag("integration")
    @Test
    void testPreservicaSevenAuth() throws Exception {

        YAML conf = ServiceConfig.getConfig();

        // name: pvica6.devel
        OaiTargetDto oaiTarget = new OaiTargetDto();
        oaiTarget.setUrl(conf.getString("integration.oaiTargets[1].url")); //Public KB service
        oaiTarget.setName("Unitest2");
        oaiTarget.setMetadataprefix(conf.getString("integration.oaiTargets[1].metadataPrefix"));
        oaiTarget.setUsername(conf.getString("integration.oaiTargets[1].user"));
        oaiTarget.setPassword(conf.getString("integration.oaiTargets[1].password"));;
        oaiTarget.setDatasource(conf.getString("integration.oaiTargets[1].datasource"));
        oaiTarget.setFilter(OaiTargetDto.FilterEnum.PRESERVICA);
        oaiTarget.setDateStampFormat(OaiTargetDto.DateStampFormatEnum.DATETIME);
        DsDatahandlerJobDto job = JobCache.createNewOaiJob(oaiTarget,null);


        OaiHarvestClient client = new OaiHarvestClient(job,oaiTarget,null);
        OaiResponse r1 = client.next();
        assertEquals(200, r1.getRecords().size()); //there is over 200 now. 200 is batch size.
        assertNotNull(r1.getResumptionToken());

        //System.out.println(r1.getRecords().get(0).getMetadata());


        //Fetch next 200
        OaiResponse r2= client.next();
        assertEquals(200, r2.getRecords().size());
        // and next
        OaiResponse r3= client.next();
        assertEquals(200, r3.getRecords().size());
        
        JobCache.finishJob(job, 0, false);
    }

}
