package dk.kb.datahandler.oai.plugins;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.kb.datahandler.config.ServiceConfig;
import dk.kb.datahandler.oai.OaiRecord;
import dk.kb.datahandler.preservica.AccessResponseObject;
import dk.kb.datahandler.preservica.client.DsPreservicaClient;
import dk.kb.datahandler.preservica.jobs.JobsBase;
import dk.kb.datahandler.util.PreservicaUtils;
import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.util.yaml.YAML;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 *
 */
public class PreservicaManifestationPlugin  implements Plugin {

    private static final Logger log = LoggerFactory.getLogger(PreservicaManifestationPlugin.class);
    private final String filenameField = "\"cmis:contentStreamFileName\",\"value\":\"";
    private DsPreservicaClient client;

    private HttpURLConnection connection;

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

    @Override
    public void apply(DsRecordDto dsRecord) {
        //log.debug("Applying plugin to record with ID: '{}'", dsRecord.getId());

        try {
            String preservicaID = PreservicaUtils.getPreservicaIoId(dsRecord);
            //log.debug("Preservica ID has been resolved to: '{}'", preservicaID);

            String filename = getManifestationFileName(preservicaID);
            String prefixedFilename = dsRecord.getOrigin() + ":" + filename;

            if (!filename.isEmpty() ){
                List<String> singletonFilename = Collections.singletonList(prefixedFilename);
                dsRecord.setChildrenIds(singletonFilename);
            }
        } catch (URISyntaxException | IOException e) {
            log.warn("Manifestation could not be extracted. PreservicaManifestationPlugin threw the following exception: ", e);
        }

    }

    /**
     * Initialize the Preservica Manifestation Plugin. This constructor extracts all needed endpoints from config files
     * and gets the first accessToken from the backing preservica installation.
     * Furthermore, it starts a timer, which updates the accesToken every 14th minute, by exchanging a refreshToken.
     */
    public PreservicaManifestationPlugin() throws IOException {
        client = JobsBase.getPreservicaClient();
    }

    private String getManifestationFileName(String id) throws URISyntaxException, IOException {
        connection = client.getPreservicaObjectDetails(id);

        if (connection.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND){
            log.warn("Object Details API responded with HTTP 404 for id: '{}'", id);
        }

        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            String filename;
            String objectDetails = convertStreamToString(connection.getInputStream());

            /*boolean containsFilename = objectDetails.contains(filenameField);
            if (containsFilename){
                log.info("ObjectDetail contains filename: '{}'", containsFilename);
            }*/

            int indexOfContentStreamStart = objectDetails.indexOf(filenameField);
            int lengthOfContentStreamPrefix = filenameField.length();
            int FilenameIndexStart = indexOfContentStreamStart + lengthOfContentStreamPrefix;

            String semiParsedObject = objectDetails.substring(FilenameIndexStart);
            int lastIndexOfFileName = semiParsedObject.indexOf("\"");

            filename = semiParsedObject.substring(0, lastIndexOfFileName);

            // TERACOM files are not presentation copies.
            if (filename.endsWith(".ts")){
                filename = "";
            }


            /*BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));


            // IMPLEMENT PARSING OF RESPONSE
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
                    .collect(Collectors.joining());*/

            /*if (filename.isEmpty()){
                log.debug("No filename was extracted for InformationObject: '{}'", id);
            }*/

            if (!filename.isEmpty()){
                log.debug("File with filename: '{}' has been extracted for record: '{}'",filename, id);
            }

            // Close the reader
            //in.close();

            return filename;
        } else {
            throw new IOException("Expected to receive HTTP 200 from call to Preservica Object Details endpoint " +
                    "for record with id: '" + id + "', but got: '" + connection.getResponseCode()+ "' instead.");
        }
    }

    private String getStringValue(JsonNode prop) {
        return prop.get("value").asText();
    }

    private boolean filterByPropName(JsonNode prop) {
        return prop.get("name").asText().equals("cmis:contentStreamFileName");
    }

    // Method to stream JsonNodes from a JsonNode
    private static Stream<JsonNode> streamJsonNodes(JsonNode jsonNode) {
        Iterable<JsonNode> iterable = jsonNode::elements;
        return StreamSupport.stream(iterable.spliterator(), false);
    }


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
}
