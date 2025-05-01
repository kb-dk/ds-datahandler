package dk.kb.datahandler.kaltura;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.solr.common.SolrDocumentList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kaltura.client.enums.MediaType;

import dk.kb.datahandler.config.ServiceConfig;

import dk.kb.util.Resolver;

public class KalturaDeltaUploadIntegrationTest {

    
    private static final Logger log = LoggerFactory.getLogger(KalturaDeltaUploadIntegrationTest.class);

    
    
    @BeforeAll
    static void setup() {
        try {
       ServiceConfig.initialize("ds-datahandler-integration-test.yaml");
        }
        catch(Exception e) {
            log.error("error loading integration-test yaml");
        }
    }
    
    @Test
    void fetchSolrDocumentTest() throws Exception {
        try {
        SolrDocumentList records = KalturaDeltaUploadJob.fetchSolrRecords(0, 10);
        assertTrue(records.size() >= 0);
        }
        catch(Exception e) {
            fail("Solr call failed");
        }        
    }
    

    @Test
    void uploadStreamTest() throws Exception {
        try {

       //It is inteded this is a local file. After upload it must be delete in the KMC            
       String filePath="/home/teg/Music/Boz_-_BBB_20191202.mp3";            
            
       String title="test title audio";
       String referenceId="12345TEGAudio";
       String description="test description audtion";
       String tag="delta-2025-04-25";
       MediaType mediaType=MediaType.AUDIO;
       int flavourParamId= KalturaUtil.getFlavourParamId(mediaType);
       
       //Incomment if you want to run test.
       String entryId=KalturaDeltaUploadJob.uploadStream(title, referenceId, description, filePath, tag, description, mediaType, flavourParamId);
       
       }
        catch(Exception e) {
            e.printStackTrace();
            fail("Upload failed");
        }        
    }

    
    
    
}
