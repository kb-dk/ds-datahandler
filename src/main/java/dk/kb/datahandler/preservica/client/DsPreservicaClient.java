package dk.kb.datahandler.preservica.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.kb.datahandler.config.ServiceConfig;
import dk.kb.datahandler.preservica.AccessResponseObject;
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
import java.util.List;
import java.util.TimerTask;

public class DsPreservicaClient {
    private static final List<String> accessEndpoint = List.of("api", "accesstoken", "login");
    private static final List<String> accessRefreshEndpoint = List.of("api", "accesstoken", "refresh");
    private static final List<String> objectDetailsEndpoint = List.of("api", "content", "object-details");
    private String baseUrl;
    private String user;
    private String password;
    private long sessionKeepAliveSeconds;
    private long lastSessionStart = 0;
    DsPreservicaClient client;

    private String accessToken;
    private String refreshToken;
    private static final Logger log = LoggerFactory.getLogger(DsPreservicaClient.class);

    public DsPreservicaClient(String baseUrl, String username, String password, long sessionKeepAliveSeconds){
        if (sessionKeepAliveSeconds <600) { //Enforce some kind of reuse of session since authenticating sessions will accumulate at Kaltura.
            throw new IllegalArgumentException("SessionKeepAliveSeconds must be at least 600 seconds (10 minutes) ");
        }
        this.baseUrl = baseUrl;
        this.user = username;
        this.password = password;
        this.sessionKeepAliveSeconds=sessionKeepAliveSeconds;

        // Call method to get first accesToken and refreshToken.
        getAccess();
    }

    /**
     * Get Preservica Client for use
     * @return the {@link DsPreservicaClient}.
     */
    public static DsPreservicaClient getPreservicaClient() {
        String preservicaUrl = ServiceConfig.getConfig().getString("preservica.baseUrl");
        String user = ServiceConfig.getConfig().getString("preservica.user");
        String password =  ServiceConfig.getConfig().getString("preservica.password");

        DsPreservicaClient client = new DsPreservicaClient(preservicaUrl, user, password, 600);
        return client;
    }

    /**
     * Get initial accessToken from Preservica Access API. This method gets the accessToken by using user credentials.
     * This should only be used for getting the initial accessToken. Subsequent refreshes should use the refreshToken.
     */
    public void getAccess() {
        try {
            // Create URLConnection for access endpoint
            HttpURLConnection connection = getPreservicaAccessConnection();
            AccessResponseObject responseObject = getAccessResponseObject(connection);

            // Set accessToken and refreshToken from responseObject
            accessToken = responseObject.getToken();
            refreshToken = responseObject.getRefreshToken();
            lastSessionStart=System.currentTimeMillis(); //Reset timer
            // Close connection
            connection.disconnect();
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }

    }

    public synchronized DsPreservicaClient getClientInstance() throws IOException{
        try {
            if (System.currentTimeMillis()-lastSessionStart >= sessionKeepAliveSeconds*100) {
                DsPreservicaClient clientInstance = new DsPreservicaClient(baseUrl, user, password, sessionKeepAliveSeconds);
                log.info("Refreshed Preservica client session.");
                client=clientInstance;
                lastSessionStart=System.currentTimeMillis(); //Reset timer
                return client;
            }
            return client; //Reuse existing connection.
        }
        catch (Exception e) {
            log.warn("Connecting to Preservica failed.");
            throw new IOException (e);
        }
    }

    /**
     * Create a POST {@link HttpURLConnection} to the backing Preservica Access API, posting username and password.
     * @return an open connection to the Preservica Access API.
     */
    public HttpURLConnection getPreservicaAccessConnection() throws IOException, URISyntaxException {
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
    public static AccessResponseObject getAccessResponseObject(HttpURLConnection connection) throws IOException {
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
            Thread.currentThread().setName("RefreshTokenTimer");
            // Create URL for access endpoint
            try {
                HttpURLConnection connection = refreshPreservicaAccessConnection();
                AccessResponseObject responseObject = getAccessResponseObject(connection);

                accessToken = responseObject.getToken();
                refreshToken = responseObject.getRefreshToken();

                log.info("Refreshed access token.");
                connection.disconnect();
            } catch (IOException | URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
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

    public InputStream getPreservicaObjectDetails(String id) throws IOException, URISyntaxException {
        StringBuilder idBuilder = new StringBuilder();
        URL url = new URIBuilder(baseUrl)
                .setPathSegments(objectDetailsEndpoint)
                // This ID needs to be prefixed with the string: sdb:IO|
                .addParameter("id", idBuilder.append("sdb:IO|").append(id).toString())
                .build()
                .toURL();

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("GET");
        connection.setRequestProperty("accept", "application/json");
        connection.setRequestProperty("Preservica-Access-Token", accessToken);

        return connection.getInputStream();
    }

}
