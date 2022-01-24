package dk.kb.datahandler.oai;

import org.junit.jupiter.api.Test;


/*
 * This is an integration test that must be run manually.
 * 
 */
public class OaiHarvestClientTest {


    public void main(String[] args) throws Exception {
   
        //example: http://www5.kb.dk/cop/oai/?metadataPrefix=mods&set=oai:kb.dk:images:billed:2010:okt:billeder&verb=ListRecords
       //
       String baseUrl="http://www5.kb.dk/cop/oai/";
       //String metadataPrefix="mods";        
       //String verb="ListRecords";
       String set="oai:kb.dk:images:billed:2010:okt:billeder";
        
       OaiHarvestClient client = new OaiHarvestClient(baseUrl, set);
       
       OaiResponse r1 = client.next();
       System.out.println(r1.getRecords().size());
       OaiResponse r2= client.next();
       System.out.println(r2.getRecords().size());
       
       
        }
   


}
