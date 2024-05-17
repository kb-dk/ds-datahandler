package dk.kb.datahandler.preservica;

import dk.kb.datahandler.config.ServiceConfig;

import dk.kb.datahandler.oai.plugins.Plugin;
import dk.kb.datahandler.oai.plugins.PreservicaManifestationPlugin;
import dk.kb.storage.invoker.v1.ApiException;
import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.storage.model.v1.RecordTypeDto;
import dk.kb.storage.storage.DsStorage;
import dk.kb.storage.util.DsStorageClient;
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
    public void testGetManifestationFromDsRecord() throws IOException {
        Plugin manifestationPlugin = new PreservicaManifestationPlugin();

        DsRecordDto record = new DsRecordDto();
        // TV-AVISEN - correctly shown in swagger, result not present here.
        //record.setId("ds.tv:oai:io:321873bf-8eaa-433d-8a58-813c321202e0");
        // Some radio program, also correctly shown in swagger, result not present here.
        //record.setId("ds.tv:oai:io:5239146d-57ec-4221-8630-2a830de4759d");
        record.setId("ds.tv.devel7:oai:io:aeeb00c9-afd8-4940-8160-b6027c33df94");
        assertNull(record.getChildrenIds());

        manifestationPlugin.apply(record);

        // EXAMPLE response
        // {"success":true,"version":1,"value":{"id":"sdb:IO|aeeb00c9-afd8-4940-8160-b6027c33df94","name":"dr1_2018-07-09-20.00-20.45","properties":[{"name":"cmis:objectId","value":"sdb:IO|aeeb00c9-afd8-4940-8160-b6027c33df94"},{"name":"cmis:name","value":"dr1_2018-07-09-20.00-20.45"},{"name":"cmis:description","value":"Kyst til kyst III – Limfjorden Vest"},{"name":"cmis:objectTypeId","value":"cmis:document"},{"name":"sdbcmis:parentId","value":"sdb:SO|c4faa5a5-b34f-42b1-beeb-6dd18f47eefb"},{"name":"sdbcmis:parentName","value":"09"},{"name":"cmis:createdBy","value":"thl"},{"name":"cmis:creationDate","value":"1532077526"},{"name":"cmis:lastModifiedBy","value":"thl"},{"name":"cmis:lastModificationDate","value":"1533121454"},{"name":"cmis:isImmutable","value":"true"},{"name":"cmis:isLatestVersion","value":"true"},{"name":"cmis:isMajorVersion","value":"true"},{"name":"cmis:isLatestMajorVersion","value":"true"},{"name":"cmis:versionLabel","value":"dr1_2018-07-09-20.00-20.45"},{"name":"cmis:versionSeriesId","value":"aeeb00c9-afd8-4940-8160-b6027c33df94"},{"name":"cmis:isVersionSeriesCheckedOut","value":"false"},{"name":"cmis:checkinComment","value":""},{"name":"cmis:contentStreamLength","value":"164067232"},{"name":"cmis:contentStreamMimeType","value":"video/mp4"},{"name":"cmis:contentStreamFileName","value":"aeeb00c9-afd8-4940-8160-b6027c33df94.mp4"},{"name":"sdbcmis:downloadable","value":"true"}],"metadata":{"title":"dr1_2018-07-09-20.00-20.45","description":"Kyst til kyst III – Limfjorden Vest","groupOrItem":[]},"links":[{"rel":"render","href":"https://kuana-devel2.kb.dk/Render/render/external?entity=IO&entityRef=aeeb00c9-afd8-4940-8160-b6027c33df94","type":"Play"}]}}
        System.out.println(record.getChildrenIds());
        assertEquals("aeeb00c9-afd8-4940-8160-b6027c33df94.mp4", record.getChildrenIds().get(0));
    }

}
