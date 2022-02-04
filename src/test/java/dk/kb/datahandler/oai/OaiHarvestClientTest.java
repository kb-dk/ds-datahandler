package dk.kb.datahandler.oai;

import org.junit.jupiter.api.Test;


/*
 * This is an integration test that must be run manually.
 * 
 */
public class OaiHarvestClientTest {


    public static  void main(String[] args) throws Exception {
   
        //example: http://www5.kb.dk/cop/oai/?metadataPrefix=mods&set=oai:kb.dk:images:billed:2010:okt:billeder&verb=ListRecords

        // When using resumption token you must NOT include metadatPrefix+set again
        //http://www5.kb.dk/cop/oai/?verb=ListRecords&resumptionToken=KB!1000!mods!0001-01-01!9999-12-31!oai:kb.dk:images:billed:2010:okt:billeder
        /*
       String baseUrl="http://www5.kb.dk/cop/oai/";
       String metadataPrefix="mods";               
       String set="oai:kb.dk:images:billed:2010:okt:billeder";
       String user = null;
       String password=null;
       */
        
        String baseUrl="https://pvica-devel2.statsbiblioteket.dk/OAI-PMH/";
        String metadataPrefix="XIP_full_schema";               
        String set=null;
        String user = "oai-pmh-devel";
        String password="XXXX";


        
        
//       String set="oai:kb.dk:images:billed:2014:jun:hca";
       OaiHarvestClient client = new OaiHarvestClient(baseUrl, set,metadataPrefix,null, user,password);
       
       OaiResponse r1 = client.next();
       System.out.println("records:"+r1.getRecords().size());
       System.out.println("token:"+r1.getResumptionToken());
       
       
       OaiResponse r2= client.next();
       System.out.println(r2.getRecords().size());
       
       OaiResponse r3= client.next();
       System.out.println(r3.getRecords().size());
              
     
        }
   


}
