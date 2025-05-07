package dk.kb.datahandler.kaltura;

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
            /*
             * WARNING! For this test to success you need to change both filePath to a local
             * file and referenceId to a new value not in the KMC. Afterwards you must to
             * delete the file in the KMC which you can find using the referenceId.
             */
            String filePath = "/home/teg/Music/Boz_-_BBB_20191202.mp3"; // Will fail if file does not exist.

            String title = "test title audio";
            String referenceId = "12345TEGAudio";
            String description = "test description audio";
            String tag = "delta-2025-04-25";
            MediaType mediaType = MediaType.AUDIO;
            int flavourParamId = KalturaUtil.getFlavourParamId(mediaType);

            // Incomment if you want to run test.
            // String entryId=KalturaDeltaUploadJob.uploadStream(title, referenceId,
            // description, filePath, tag, mediaType, flavourParamId);

        } catch (Exception e) {
            e.printStackTrace();
            fail("Upload failed");
        }
    }

}
