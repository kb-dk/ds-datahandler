package dk.kb.datahandler.oai.plugins;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.kb.datahandler.config.ServiceConfig;
import dk.kb.datahandler.oai.OaiRecord;
import dk.kb.datahandler.preservica.AccessResponseObject;
import dk.kb.util.yaml.YAML;
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
public class PreservicaManifestationPlugin  implements Plugin{

    private final String baseUrl;
    private final List<String> accessEndpoint = List.of("api", "accesstoken", "login");
    private final List<String> accessRefreshEndpoint = List.of("api", "accesstoken", "refresh");
    private final List<String> objectDetailsEndpoint = List.of("api", "content", "object-details");
    private final String user;
    private final String password;

    private String accessToken;
    private String refreshToken;

    private static final Logger log = LoggerFactory.getLogger(PreservicaManifestationPlugin.class);

    /**
     *
     */
    @Override
    public void apply(OaiRecord oaiRecord) {
        log.info("Preservica Manifestation Plugin has been applied.");
        log.info("Access Token is: '{}'", accessToken);

        try {
            String filename = getManifestationFileName(oaiRecord.getId());
            oaiRecord.setManifestationId(filename);
            log.info("Filename is: '{}'", filename);
        } catch (URISyntaxException | IOException e) {
            log.warn("Manifestation could not be extracted. PreservicaManifestationPlugin threw the following exception: ", e);
        }

    }

    /**
     * Initialize the Preservica Manifestation Plugin. This constructor extracts all needed endpoints from config files
     * and gets the first accessToken from the backing preservica installation.
     * Furthermore, it starts a timer, which updates the accesToken every 14th minute, by exchanging a refreshToken.
     */
    public PreservicaManifestationPlugin(){
        YAML preservicaConfig = ServiceConfig.getConfig().getSubMap("preservica");
        this.baseUrl = preservicaConfig.getString("baseUrl");
        this.user = preservicaConfig.getString("user");
        this.password = preservicaConfig.getString("password");

        // Schedule a task to refresh the token every 14 minutes. As the first accessToken can be used the delay is also 14 minutes.
        Timer timer = new Timer();
        timer.schedule(new RefreshTokenTask(), 14 * 60 * 1000, 14 * 60 * 1000);

        // Call method to get first accesToken and refreshToken.
        getInitialAccess();
    }

    private String getManifestationFileName(String id) throws URISyntaxException, IOException {
        HttpURLConnection connection = getPreservicaObjectDetails(id);

        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));

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
                    .collect(Collectors.joining());

            // Close the reader
            in.close();
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

    private HttpURLConnection getPreservicaObjectDetails(String id) throws URISyntaxException, IOException {
        URL url = new URIBuilder(baseUrl)
                .setPathSegments(objectDetailsEndpoint)
                // This ID needs to be prefixed with the string: sdb:IO|
                .addParameter("id", "sdb:IO|"+id)
                .build()
                .toURL();

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("GET");
        connection.setRequestProperty("accept", "application/json");
        connection.setRequestProperty("Preservica-Access-Token", accessToken);

        return connection;
    }

    /**
     * Get initial accessToken from Preservica Access API. This method gets the accessToken by using user credentials.
     * This should only be used for getting the initial accessToken. Subsequent refreshes should use the refreshToken.
     */
    private void getInitialAccess() {
        try {
            // Create URLConnection for access endpoint
            HttpURLConnection connection = getPreservicaAccessConnection();
            AccessResponseObject responseObject = getAccessResponseObject(connection);

            // Set accessToken and refreshToken from responseObject
            this.accessToken = responseObject.getToken();
            this.refreshToken = responseObject.getRefreshToken();
            // Close connection
            connection.disconnect();
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create a POST {@link HttpURLConnection} to the backing Preservica Access API, posting username and password.
     * @return an open connection to the Preservica Access API.
     */
    private HttpURLConnection getPreservicaAccessConnection() throws IOException, URISyntaxException {
        URL url = new URIBuilder(baseUrl)
                .setPathSegments(accessEndpoint)
                .build()
                .toURL();

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        connection.setRequestProperty("accept", "application/json");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        // Set POST data
        String postData = "username=" + user +
                            "&password=" + password +
                            "&cookie=false" +
                            "&includeUserDetails=false";
        connection.setDoOutput(true);
        try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
            byte[] postDataBytes = postData.getBytes(StandardCharsets.UTF_8);
            wr.write(postDataBytes);
            wr.flush();
        }
        return connection;
    }

    /**
     * Create a POST {@link HttpURLConnection} to the backing Preservica Access API, sending refresh token as a query
     * parameter and setting the current access token as value for the header: {@code Preservica-Access-Token}.
     * @return an open connection, where a new accessToken can be extracted from. This new token should be valid for fifteen minutes.
     */
    public HttpURLConnection refreshPreservicaAccessConnection() throws IOException, URISyntaxException {
        URL url = new URIBuilder(baseUrl)
                .setPathSegments(accessRefreshEndpoint)
                .addParameter("refreshToken", refreshToken)
                .build()
                .toURL();

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        connection.setRequestProperty("accept", "application/json");
        connection.setRequestProperty("Content-Type", "undefined");
        connection.setRequestProperty("Preservica-Access-Token", accessToken);

        // Set POST data
        String postData = "cookie=false";
        connection.setDoOutput(true);
        try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
            byte[] postDataBytes = postData.getBytes(StandardCharsets.UTF_8);
            wr.write(postDataBytes);
            wr.flush();
        }
        return connection;
    }

    /**
     * Convert JSON response from a {@link HttpURLConnection} to a {@link AccessResponseObject} JAVA-object. This method
     * expects JSON in the following format:
     * <pre>
     *{
     *   "success": true,
     *   "token": "664c455f-59f2-4c83-9d7a-ddfe5e4b363d",
     *   "refresh-token": "3bb58740-db8e-4332-bcff-7a7435f5686e",
     *   "validFor": 15,
     *   "user": "manager"
     * }
     * </pre>
     * @param connection which delivers the response JSON. Most likely created by either
     *                   {@link #getPreservicaAccessConnection()} or {@link #refreshPreservicaAccessConnection()}.
     * @return an {@link AccessResponseObject} containing accessToken and refreshToken.
     */
    private static AccessResponseObject getAccessResponseObject(HttpURLConnection connection) throws IOException {
        // Check response code and log a warning if not 200
        if (connection.getResponseCode() != 200){
            log.warn("Response Code was '{}' with the following message: '{}'", connection.getResponseCode(), connection.getResponseMessage());
        }

        // Read response
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
        String inputLine;
        StringBuilder response = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        // Create AccessResponseObject from response
        ObjectMapper objectMapper = new ObjectMapper();
        AccessResponseObject responseObject = objectMapper.readValue(response.toString(), AccessResponseObject.class);
        return responseObject;
    }


    /**
     * Inner class representing the task to refresh the token
     */
    class RefreshTokenTask extends TimerTask {
        @Override
        public void run() {
            // Create URL for access endpoint
            try {
                HttpURLConnection connection = refreshPreservicaAccessConnection();
                AccessResponseObject responseObject = getAccessResponseObject(connection);

                accessToken = responseObject.getToken();
                refreshToken = responseObject.getRefreshToken();

                log.info("New accessToken is: '{}'", accessToken);
                connection.disconnect();
            } catch (IOException | URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
