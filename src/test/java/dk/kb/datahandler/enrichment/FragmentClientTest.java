package dk.kb.datahandler.enrichment;

import dk.kb.storage.model.v1.DsRecordDto;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;

public class FragmentClientTest {

    @Test
    public void testFetchFragments() throws IOException, URISyntaxException {
        FragmentsClient client = new FragmentsClient("http://nifi-rtv-stage-node01.kb.dk:9411");
        DsRecordDto record = new DsRecordDto();
        record.setId("b8a1a107-59a8-4f74-a6e4-c0026b828d66");
        client.fetchMetadataFragments(record);
    }

}
