package dk.kb.datahandler.oai.plugins;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.kb.datahandler.oai.OaiRecord;
import dk.kb.datahandler.preservica.client.DsPreservicaClient;
import dk.kb.datahandler.util.PreservicaUtils;
import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.storage.model.v1.RecordTypeDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 *
 */
public class PreservicaManifestationPlugin  implements Plugin {
    public static DsRecordDto createdRecord = new DsRecordDto();

    private static final Logger log = LoggerFactory.getLogger(PreservicaManifestationPlugin.class);
    private final String filenameField = "cmis:contentStreamFileName";
    private DsPreservicaClient client;

    /**
     *
     */
    @Override
    public void apply(OaiRecord oaiRecord) {
        try {
            String filename = getManifestationFileName(oaiRecord.getId());
            oaiRecord.setManifestationId(filename);
            log.info("Filename is: '{}'", filename);
        } catch (URISyntaxException | IOException e) {
            log.warn("Manifestation could not be extracted. PreservicaManifestationPlugin threw the following exception: ", e);
        }

    }

    /**
     * Apply the plugin to a DsRecord.
     * If the record is a DeliverableUnit (A record containing metadata) the ID of the manifestation, which the record
     * is about will be resolved from the backing Preservica instance. The resolved child record can be accessed in the
     * {@link #createdRecord}.
     * @param dsRecord DeliverableUnit to fetch manifestation from.
     */
    @Override
    public void apply(DsRecordDto dsRecord) {
        if (dsRecord.getRecordType() != RecordTypeDto.DELIVERABLEUNIT){
            log.warn("Manifestation extraction plugin has been used on a record which cant have manifestations.");
            return;
        }

        // Resets the output record.
        resetInternalRecord();
        try {
            // Get the clean Preservica InformationObject ID.
            String preservicaID = PreservicaUtils.getPreservicaIoId(dsRecord);

            // Extract filename from Preservica and create a prefixed version for DS.
            String filename = getManifestationFileName(preservicaID);
            // Ensure that child records are added to the origin of the processed record.
            StringJoiner joiner = new StringJoiner(":");
            String prefixedFilename = joiner.add(dsRecord.getOrigin()).add(filename).toString();

            // Update the record that is to be returned.
            if (!filename.isEmpty()){
                updateInternalRecord(dsRecord, filename, prefixedFilename);
            }
        } catch (URISyntaxException | IOException e) {
            log.error("Manifestation could not be extracted. PreservicaManifestationPlugin threw the following exception: ", e);
        }

    }

    /**
     * Initialize the Preservica Manifestation Plugin. This constructor extracts all needed endpoints from config files
     * and gets the first accessToken from the backing preservica installation.
     * Furthermore, it starts a timer, which updates the accesToken every 14th minute, by exchanging a refreshToken.
     */
    public PreservicaManifestationPlugin() throws IOException {
        client = DsPreservicaClient.getPreservicaClient();
        createdRecord.setRecordType(RecordTypeDto.MANIFESTATION);
    }

    /**
     * Resolve filename for a presentation copy given a Preservica 7 InformationObject ID. The filename is extracted
     * from a bigger JSON response by string manipulation.
     * @param id Preservica 7 InformationObject ID.
     * @return the filename for the newest presentation copy for the given InformationObject.
     */
    private String getManifestationFileName(String id) throws URISyntaxException, IOException {
        InputStream objectDetails = client.getPreservicaObjectDetails(id);

        /*if (connection.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND){
            log.error("Object Details API responded with HTTP 404 for id: '{}'", id);
            return "";
        }*/

        BufferedReader in = new BufferedReader(new InputStreamReader(objectDetails, StandardCharsets.UTF_8));
        // Create ObjectMapper instance with buffering enabled
        ObjectMapper jsonMapper = new ObjectMapper()
                .enable(JsonParser.Feature.AUTO_CLOSE_SOURCE)
                .enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);

        // Parse JSON array
        JsonNode rootNode = jsonMapper.readTree(in);
        // Iterate over each JSON object in the array
        Iterator<JsonNode> rootIterator = rootNode.elements();
        JsonNode properties = null;
        while (rootIterator.hasNext()) {
            JsonNode node = rootIterator.next();
            properties = node.get("properties");
        }

        if (properties == null){
            throw new IOException("Expected to receive properties from call to Preservica Object Details endpoint for record with id: '" + id + "'.");
        }

        String filename = streamJsonNodes(properties)
                .filter(this::filterByPropName)
                .map(this::getStringValue)
                .collect(Collectors.joining());

        // Close the reader
        in.close();

        // TERACOM files are not presentation copies and should not be returned.
        if (filename.endsWith(".ts")){
            return "";
        }

        return filename;
    }

    /**
     * Reset {@link #createdRecord}.
     */
    private void resetInternalRecord() {
        createdRecord.setParentId("");
        createdRecord.setOrigin("");
        createdRecord.setParent(null);
        createdRecord.setData("");
        createdRecord.setId("");
    }

    /**
     * Update {@link #createdRecord}
     * @param parent a {@link DsRecordDto} which the updated internal record is a presentation manifestation for.
     * @param filename the name of the presentation manifestation file.
     * @param prefixedFilename filename prefixed with origin from parent record.
     */
    private static void updateInternalRecord(DsRecordDto parent, String filename, String prefixedFilename) {
        createdRecord.setParentId(parent.getId());
        createdRecord.setOrigin(parent.getOrigin());
        createdRecord.setParent(parent);
        createdRecord.setData(filename);
        createdRecord.setId(prefixedFilename);
    }

    /**
     * Convert an InputStream to a String. Here it is used to deliver a JSON object, which the filename is then later
     * extracted from.
     * @param inputStream to convert to a string.
     * @return the content of the input stream as a string.
     */
    public static String convertStreamToString(InputStream inputStream) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
        }
        return stringBuilder.toString();
    }

    /**
     * Stops the timer in the backing Preservica client, making the client usable for a maximum of 14 more minuts.
     */
    public void stopClient(){
        client.endTimer();
    }

    private String getStringValue(JsonNode prop) {
        String filename = prop.get("value").asText();
        if (filename.endsWith(".ts")){
            return "";
        } else {
            return filename;
        }
    }

    private boolean filterByPropName(JsonNode prop) {
        return prop.get("name").asText().equals(filenameField);
    }

    private static Stream<JsonNode> streamJsonNodes(JsonNode jsonNode) {
        Iterable<JsonNode> iterable = jsonNode::elements;
        return StreamSupport.stream(iterable.spliterator(), false);
    }

}
