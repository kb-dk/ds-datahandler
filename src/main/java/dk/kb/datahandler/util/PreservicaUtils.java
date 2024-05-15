package dk.kb.datahandler.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.kb.datahandler.oai.plugins.Plugin;
import dk.kb.datahandler.oai.plugins.PreservicaManifestationPlugin;
import dk.kb.datahandler.preservica.AccessResponseObject;
import dk.kb.storage.invoker.v1.ApiException;
import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.storage.util.DsStorageClient;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class PreservicaUtils {

    private static final Logger log = LoggerFactory.getLogger(PreservicaUtils.class);

    /**
     * Validate that the given record is a Preservica 7 InformationObject
     * @param dsRecord
     * @return
     */
    public static boolean isInformationObject(DsRecordDto dsRecord) {
        String id = dsRecord.getId();
        // Check if the id contains ':oai:io:' as this is what characterises InformationObjects from Preservica 7.
        return id != null && id.contains(":oai:io:");
    }

    public static DsRecordDto fetchManifestation(DsRecordDto record) {
        Plugin manifestationPlugin = new PreservicaManifestationPlugin();
        manifestationPlugin.apply(record);
        log.info("Record was updated with childID: '{}'", record.getChildren());
        return record;
    }

    public static DsRecordDto safeRecordPost(DsStorageClient storageClient, DsRecordDto record) {
        try {
            storageClient.recordPost(record);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
        return record;
    }
}
