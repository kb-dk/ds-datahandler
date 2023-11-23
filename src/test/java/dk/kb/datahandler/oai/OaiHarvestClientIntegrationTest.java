package dk.kb.datahandler.oai;

import org.junit.jupiter.api.Test;

import dk.kb.datahandler.facade.DsDatahandlerFacade;
import dk.kb.datahandler.model.v1.OaiTargetDto;


/*
 * This is an integration test that must be run manually.
 * 
 */
public class OaiHarvestClientIntegrationTest {


    public static  void main(String[] args) throws Exception {
   
        //example: http://www5.kb.dk/cop/oai/?metadataPrefix=mods&set=oai:kb.dk:images:billed:2010:okt:billeder&verb=ListRecords

        // When using resumption token you must NOT include metadatPrefix+set again
        //http://www5.kb.dk/cop/oai/?verb=ListRecords&resumptionToken=KB!1000!mods!0001-01-01!9999-12-31!oai:kb.dk:images:billed:2010:okt:billeder
  
        
        OaiTargetDto oaiTarget = new OaiTargetDto();
        
        
       oaiTarget.setUrl("http://www5.kb.dk/cop/oai/");
       oaiTarget.setMetadataprefix("mods");               
       oaiTarget.setSet("oai:kb.dk:images:billed:2010:okt:billeder");
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
        
        
//       String set="oai:kb.dk:images:billed:2014:jun:hca";
       OaiTargetJob job = DsDatahandlerFacade.createNewJob(oaiTarget);        
       
       OaiHarvestClient client = new OaiHarvestClient(job,from);
       
       OaiResponse r1 = client.next();
       System.out.println("records:"+r1.getRecords().size());
       System.out.println("token:"+r1.getResumptionToken());
       
   /*    
       OaiResponse r2= client.next();
       System.out.println(r2.getRecords().size());
       
       OaiResponse r3= client.next();
       System.out.println(r3.getRecords().size());
              */
     
        }
   


}
