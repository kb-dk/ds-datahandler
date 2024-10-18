package dk.kb.datahandler.preservica.client;

import dk.kb.datahandler.config.ServiceConfig;

import dk.kb.datahandler.preservica.PreservicaManifestationExtractor;
import dk.kb.datahandler.preservica.client.DsPreservicaClient;
import dk.kb.datahandler.util.PreservicaUtils;
import dk.kb.storage.model.v1.DsRecordMinimalDto;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.MockedStatic;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("integration")
public class PreservicaClientTest {

    @BeforeAll
    public static void setup() throws IOException {
        ServiceConfig.initialize("conf/ds-datahandler-behaviour.yaml" ,"ds-datahandler-integration-test.yaml");
    }


    @Test
    public void testManifestationExtractionForDoms() throws IOException, InterruptedException {
        // ID of a DOMS record. Has no ContentObject as of 11th of July 2024.
        String informationObjectId = "6ca25068-6dd4-45ed-a0cb-ab808441c078";
        String result = DsPreservicaClient.getInstance().getFileRefFromInformationObjectAsStream(informationObjectId);

        assertEquals(informationObjectId, result);
    }

    @Test
    public void testCreateManifestationFromDsRecord() throws IOException {
        PreservicaManifestationExtractor manifestationPlugin = new PreservicaManifestationExtractor();
        DsRecordMinimalDto record = getTestRecord();
        record = manifestationPlugin.apply(record);

        assertEquals("21f85387-9900-4f2a-ab4f-cf81b2fd1dea", record.getReferenceId());
    }

    @Tag("integration")
    @Test
    public void testGetAccessRepresentationForIO() throws IOException, URISyntaxException, XMLStreamException, InterruptedException {
        InputStream stream = DsPreservicaClient.getInstance().getAccessRepresentationForIO("ee36a7b5-de87-4e45-96d8-018b513a5e2e");

        String contentObject = PreservicaUtils.parseRepresentationResponseForContentObject(stream);
        // Newest ContentObject at 28th of May 2024. This can change in the future.
        assertEquals("2de653bc-e182-40c4-90c3-60c8b9a546c4", contentObject);
    }

    @Tag("integration")
    @Test
    public void testGetFileRefForContentObject() throws IOException, URISyntaxException, XMLStreamException, InterruptedException {
        // Newest ContentObject at 28th of May 2024. This can change in the future.
        InputStream stream = DsPreservicaClient.getInstance().getFileRefForContentObject("2de653bc-e182-40c4-90c3-60c8b9a546c4");
        String fileRef = PreservicaUtils.parseIdentifierResponseForFileRef(stream);
        assertEquals("ce4a81eb-ab15-474f-bed5-0debc2fde97a", fileRef);
    }

    @Tag("integration")
    @Test
    public void testGetFileRefFromIO() throws IOException, InterruptedException {
        // Newest ContentObject at 28th of May 2024. This can change in the future.
        String fileRef = DsPreservicaClient.getInstance().getFileRefFromInformationObjectAsStream("ee36a7b5-de87-4e45-96d8-018b513a5e2e");
        assertEquals("ce4a81eb-ab15-474f-bed5-0debc2fde97a", fileRef);
    }

    @Tag("integration")
    @Test
    public void testNoFileRefFromIO() throws IOException, InterruptedException {
        // Newest ContentObject at 28th of May 2024. This can change in the future.
        String fileRef = DsPreservicaClient.getInstance().getFileRefFromInformationObjectAsStream("abee9c4f-dacd-4518-b68b-773c8506ac7d");
        assertEquals("", fileRef);
    }

    @Tag("slow")
    @Tag("integration")
    @Test
    public void testDOMSCheck() {
        // Test used for profiling connection to preservica and parsing response objects.

        // When I did profiling I set this variable to 10000.
        int amountOfRuns = 1000;
        String id1 = "f37d60da-6b29-4d4f-9b18-50c45e9157a8";
        String id2 = "ab438f89-1baf-441e-91de-568068e8f406";
        String id3 = "689e2fff-e2db-47e8-ba16-6719a8616b38";
        List<String> ids = List.of(id1, id2, id3);

        for (int i = 0; i < amountOfRuns; i++) {
            ids.forEach( id ->
                    {
                        try {
                            String result = PreservicaUtils.validateInformationObjectForDomsData(id);
                            assertEquals(id, result);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
            );
        }

        // Used as breakpoint to start and run the test with the debugger attached to have a look at  object in memory over time
        System.out.println("finished");
    }

    @Tag("slow")
    @Tag("integration")
    @Test
    public void testRefresh() throws IOException, InterruptedException {
        PreservicaManifestationExtractor manifestationPlugin = new PreservicaManifestationExtractor();
        DsRecordMinimalDto record = getTestRecord();
        record = manifestationPlugin.apply(record);

        assertEquals("21f85387-9900-4f2a-ab4f-cf81b2fd1dea", record.getReferenceId());

        record.setReferenceId("");
        sleep(960000);

        record = manifestationPlugin.apply(record);
        assertEquals("21f85387-9900-4f2a-ab4f-cf81b2fd1dea", record.getReferenceId());
    }
    /*@Test
    public void testTimeOutHandling() throws InterruptedException, ExecutionException, IOException, URISyntaxException {
        int threadCount = 50;
        int numberOfRequests = 4000;

        // ID of a DOMS record. Has no ContentObject as of 11th of July 2024.
        String informationObjectId = "6ca25068-6dd4-45ed-a0cb-ab808441c078";
        String secondId = "9e081b87-66f4-4797-9cff-d2ce226ab300";

        DsPreservicaClient client = DsPreservicaClient.getInstance();

        // Initialize the executor service with a fixed thread pool
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        Random random = new Random();


        for (int i = 0; i < threadCount; i++) {
            // Submit tasks to the executor service
            executorService.submit(() -> {
                try {
                    for (int j = 0; j < numberOfRequests; j++) {
                        client.getFileRefFromInformationObjectAsStream(random.nextBoolean() ? informationObjectId : secondId);
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } finally {
                    latch.countDown(); // Decrement the latch count
                }
            });
        }

        // Await until all threads have finished
        latch.await(1, TimeUnit.MINUTES);
        // Shut down the executor service
        executorService.shutdown();
    }*/

    private static DsRecordMinimalDto getTestRecord() {
        DsRecordMinimalDto record = new DsRecordMinimalDto();
        record.setId("ds.tv:oai:io:9e081b87-66f4-4797-9cff-d2ce226ab300");
        return record;
    }
}
