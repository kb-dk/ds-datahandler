package dk.kb.datahandler.kaltura;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.apache.solr.common.SolrDocumentList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kaltura.client.enums.MediaType;

import dk.kb.datahandler.config.ServiceConfig;
import dk.kb.util.Resolver;

@Tag("integration")
public class KalturaDeltaUploadIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(KalturaDeltaUploadIntegrationTest.class);

    @BeforeAll
    static void setup() {
        try {
            ServiceConfig.initialize("conf/ds-datahandler-behaviour.yaml", "ds-datahandler-integration-test.yaml");
        } catch (Exception e) {
            log.error("error loading behaviour+integration test yaml");
        }
    }

    @Test
    void fetchSolrDocumentTest() throws Exception {
        try {
            SolrDocumentList records = KalturaDeltaUploadJob.fetchSolrRecords(0, 10);
            assertTrue(records.size() >= 0);
        } catch (Exception e) {
            fail("Solr call failed");
        }
    }

    @Test
    void uploadStreamTest() throws Exception {
        try {
            /* This will upload a small mp3 music file(no copyright on the mp3)
             * The integration test will delete the file in the KMC after upload.
             * If you want to upload a stream to see it in  the KMC change the file name and out comment the delete line  
             */            
            String filePath= Resolver.resolveURL("audio/NoisyPillars.mp3").getFile();
           //String filePath = "/home/xxx/test.mp3"; // If you want to use your down
            
            String title = "Integration unit test";
            String referenceId = "IntegrationTest";
            String description = "Small MP3 music file for integration unit test.";
            String tag = "delta-INTEGRATION-test"; //Do not use date, this way they will be easy to find
            MediaType mediaType = MediaType.AUDIO;
            int flavourParamId = KalturaUtil.getFlavourParamId(mediaType);
            String kalturaEntryId;
            kalturaEntryId=KalturaDeltaUploadJob.uploadStream(title, referenceId,description, filePath, tag, mediaType, flavourParamId);
            assertNotNull(kalturaEntryId); // validate we have a kaltura entryID
            Thread.sleep(5000L); //sleep 5 seconds before deleting
            KalturaDeltaUploadJob.deleteStream(kalturaEntryId); //If it fails we can not do anything. Delete it manuel in the KMC.                       
        } catch (Exception e) {
            e.printStackTrace();
            fail("Upload failed");
        }
    }

}
