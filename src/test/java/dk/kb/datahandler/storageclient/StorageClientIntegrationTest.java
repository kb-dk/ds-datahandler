package dk.kb.datahandler.storageclient;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import dk.kb.storage.invoker.v1.ApiException;
import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.storage.util.DsStorageClient;


/**
 *  Integration test, will not be run by automatic build flow.
 */
public class StorageClientIntegrationTest{

    private static final Logger log = LoggerFactory.getLogger(StorageClientIntegrationTest.class);
        
    @Tag("integration")   
    @Test
    public void testDsStorage() throws ApiException {
           	
    	String backEndUrl="http://devel11:10001/ds-storage/v1/";    
        DsStorageClient client = new DsStorageClient(backEndUrl);                 
        String id = "kb.image.luftfo.luftfoto:oai:kb.dk:images:luftfo:2011:maj:luftfoto:object187744";
        DsRecordDto record = client.getRecord(id);        
        log.info("Loaded record from storage with id:"+record.getId());
        assertEquals(id, "kb.image.luftfo.luftfoto:oai:kb.dk:images:luftfo:2011:maj:luftfoto:object187744");               
    }
    
}
