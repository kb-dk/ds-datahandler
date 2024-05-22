package dk.kb.datahandler.preservica;

import dk.kb.datahandler.config.ServiceConfig;

import dk.kb.datahandler.oai.plugins.Plugin;
import dk.kb.datahandler.oai.plugins.PreservicaManifestationPlugin;
import dk.kb.storage.invoker.v1.ApiException;
import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.storage.model.v1.RecordTypeDto;
import dk.kb.storage.storage.DsStorage;
import dk.kb.storage.util.DsStorageClient;
import org.apache.jute.Record;
import org.eclipse.jetty.util.IO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("integration")
public class PreservicaClientTest {

    @BeforeAll
    public static void setup() throws IOException {
        // TODO: Change local to integration-test from aegis
        ServiceConfig.initialize("conf/ds-datahandler-behaviour.yaml" ,"conf/ds-datahandler-local.yaml");
    }

    @Test
    public void testCreateManifestationFromDsRecord() throws IOException {
        Plugin manifestationPlugin = new PreservicaManifestationPlugin();

        // When using the devel instance of preservica 7 this test should not fail. Just testing throughput here.
        DsRecordDto record = new DsRecordDto();
        record.setRecordType(RecordTypeDto.DELIVERABLEUNIT);
        record.setId("ds.tv.devel7:oai:io:aeeb00c9-afd8-4940-8160-b6027c33df94");
        manifestationPlugin.apply(record);

        DsRecordDto childRecord = PreservicaManifestationPlugin.createdRecord;

        assertEquals(record.getId(), childRecord.getParentId());
        assertEquals(RecordTypeDto.MANIFESTATION, childRecord.getRecordType());
        assertEquals("aeeb00c9-afd8-4940-8160-b6027c33df94.mp4", childRecord.getData());
    }
}
