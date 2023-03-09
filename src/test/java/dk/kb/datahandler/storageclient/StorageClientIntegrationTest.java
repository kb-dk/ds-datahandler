package dk.kb.datahandler.storageclient;

import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.storage.util.DsStorageClient;


/*
 * This is an integration test that must be run manually.
 *
 */
public class StorageClientIntegrationTest{

    
    public static void main(String[] args) throws Exception {
           	
    	//replace server with a running server with ds-storage installed
    	String backEndUrl="http://server:10001/ds-storage/v1/";
    	
        DsStorageClient client = new DsStorageClient(backEndUrl);            
     
        DsRecordDto record = client.getRecord("kb.image.luftfo.luftfoto:oai:kb.dk:images:luftfo:2011:maj:luftfoto:object187744");        
        System.out.println(record);
        
    }
    
}
