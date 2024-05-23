package dk.kb.datahandler.preservica;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 *
 */
public class PreservicaManifestationExtractor {
    private static final Logger log = LoggerFactory.getLogger(PreservicaManifestationExtractor.class);
    private final String filenameField = "cmis:contentStreamFileName";
    private DsPreservicaClient client;

    /**
     * Apply the extractor to a DsRecord.
     * If the record is a DeliverableUnit (A record containing metadata) the ID of the manifestation, which the record
     * is about will be resolved from the backing Preservica instance and added as a referenceId in the record it has been applied to.
     * @param dsRecord DeliverableUnit to fetch manifestation from.
     */
    public DsRecordDto apply(DsRecordDto dsRecord) {
        if (dsRecord.getRecordType() != RecordTypeDto.DELIVERABLEUNIT){
            log.warn("Manifestation extraction plugin has been used on a record which cant have manifestations.");
            return null;
        }
        try {
            // Get the clean Preservica InformationObject ID.
            String preservicaID = PreservicaUtils.getPreservicaIoId(dsRecord);
            // Extract filename from Preservica and create a prefixed version for DS.
            String filename = getManifestationFileName(preservicaID);

            // Update the record that is to be returned.
            if (!filename.isEmpty()){
                dsRecord.setReferenceId(filename);
            }
        } catch (URISyntaxException | IOException e) {
            log.error("Manifestation could not be extracted. PreservicaManifestationExtractor threw the following exception: ", e);
        }

        return dsRecord;

    }

    /**
     * Initialize the Preservica Manifestation Plugin. This constructor extracts all needed endpoints from config files
     * and gets the first accessToken from the backing preservica installation.
     * Furthermore, it starts a timer, which updates the accesToken every 14th minute, by exchanging a refreshToken.
     */
    public PreservicaManifestationExtractor() throws IOException {
        client = DsPreservicaClient.getPreservicaClient();
    }

    /**
     * Resolve filename for a presentation copy given a Preservica 7 InformationObject ID. The filename is extracted
     * from a bigger JSON response by string manipulation.
     * @param id Preservica 7 InformationObject ID.
     * @return the filename for the newest presentation copy for the given InformationObject.
     */
    private String getManifestationFileName(String id) throws URISyntaxException, IOException {
        client.getClientInstance();
        InputStream objectDetails = client.getPreservicaObjectDetails(id);

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
