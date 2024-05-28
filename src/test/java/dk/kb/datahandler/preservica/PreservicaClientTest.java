package dk.kb.datahandler.preservica;

import dk.kb.datahandler.config.ServiceConfig;

import dk.kb.datahandler.preservica.client.DsPreservicaClient;
import dk.kb.datahandler.util.PreservicaUtils;
import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.storage.model.v1.RecordTypeDto;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("integration")
public class PreservicaClientTest {

    @BeforeAll
    public static void setup() throws IOException {
        // TODO: Change local to integration-test from aegis
        ServiceConfig.initialize("conf/ds-datahandler-behaviour.yaml" ,"ds-datahandler-integration-test.yaml");
        DsPreservicaClient.init(ServiceConfig.getPreservicaUrl(), ServiceConfig.getPreservicaUser(),
                ServiceConfig.getPreservicaPassword(), ServiceConfig.getPreservicaKeepAliveSeconds());
    }

    @Test
    public void testCreateManifestationFromDsRecord() throws IOException {
        PreservicaManifestationExtractor manifestationPlugin = new PreservicaManifestationExtractor();
        DsRecordDto record = getTestRecord();
        record = manifestationPlugin.apply(record);

        assertEquals("9e081b87-66f4-4797-9cff-d2ce226ab300.mp4", record.getReferenceId());
    }

    @Tag("integration")
    @Test
    public void testGetAccessRepresentationForIO() throws IOException, URISyntaxException, XMLStreamException {
        InputStream stream = DsPreservicaClient.getInstance().getAccessRepresentationForIO("ee36a7b5-de87-4e45-96d8-018b513a5e2e");

        String contentObject = PreservicaUtils.parseRepresentationResponseForContentObject(stream);
        // Newest ContentObject at 28th of May 2024. This can change in the future.
        assertEquals("2de653bc-e182-40c4-90c3-60c8b9a546c4", contentObject);
    }

    @Tag("integration")
    @Test
    public void testGetFileRefForContentObject() throws IOException, URISyntaxException, XMLStreamException {
        // Newest ContentObject at 28th of May 2024. This can change in the future.
        InputStream stream = DsPreservicaClient.getInstance().getFileRefForContentObject("2de653bc-e182-40c4-90c3-60c8b9a546c4");
        String fileRef = PreservicaUtils.parseIdentifierResponseForFileRef(stream);
        assertEquals("ce4a81eb-ab15-474f-bed5-0debc2fde97a", fileRef);
    }

    @Tag("integration")
    @Test
    public void testGetFileRefFromIO() throws IOException {
        // Newest ContentObject at 28th of May 2024. This can change in the future.
        String fileRef = DsPreservicaClient.getInstance().getFileRefFromInformationObject("ee36a7b5-de87-4e45-96d8-018b513a5e2e");
        assertEquals("ce4a81eb-ab15-474f-bed5-0debc2fde97a", fileRef);
    }

    @Tag("integration")
    @Test
    public void testNoFileRefFromIO() throws IOException {
        // Newest ContentObject at 28th of May 2024. This can change in the future.
        String fileRef = DsPreservicaClient.getInstance().getFileRefFromInformationObject("abee9c4f-dacd-4518-b68b-773c8506ac7d");
        assertEquals("", fileRef);
    }

    @Tag("slow")
    @Tag("integration")
    @Test
    public void testRefresh() throws IOException, InterruptedException {
        PreservicaManifestationExtractor manifestationPlugin = new PreservicaManifestationExtractor();
        DsRecordDto record = getTestRecord();
        record = manifestationPlugin.apply(record);

        assertEquals("9e081b87-66f4-4797-9cff-d2ce226ab300.mp4", record.getReferenceId());

        record.setReferenceId("");
        sleep(960000);

        record = manifestationPlugin.apply(record);
        assertEquals("9e081b87-66f4-4797-9cff-d2ce226ab300.mp4", record.getReferenceId());
    }

    private static DsRecordDto getTestRecord() {
        DsRecordDto record = new DsRecordDto();
        record.setRecordType(RecordTypeDto.DELIVERABLEUNIT);
        record.setId("ds.tv:oai:io:9e081b87-66f4-4797-9cff-d2ce226ab300");
        return record;
    }
}
