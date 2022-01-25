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
        
       String baseUrl="http://www5.kb.dk/cop/oai/";
       //String metadataPrefix="mods";        
       //String verb="ListRecords";
//       String set="oai:kb.dk:images:billed:2010:okt:billeder";
  
       String set="oai:kb.dk:manus:vmanus:2011:dec:ha";
       OaiHarvestClient client = new OaiHarvestClient(baseUrl, set);
       
       OaiResponse r1 = client.next();
       OaiResponse r2= client.next();
       System.out.println(r2.getRecords().size());
       OaiResponse r3= client.next();
       System.out.println(r3.getRecords().size());
       OaiResponse r4= client.next();
       System.out.println(r4.getRecords().size());
       OaiResponse r5= client.next();
       System.out.println(r5.getRecords().size());
       OaiResponse r6= client.next();
       System.out.println(r6.getRecords().size());
       OaiResponse r7= client.next();
       System.out.println(r7.getRecords().size());

       
       
        }
   


}
