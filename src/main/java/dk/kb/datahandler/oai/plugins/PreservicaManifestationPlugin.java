package dk.kb.datahandler.oai.plugins;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.kb.datahandler.config.ServiceConfig;
import dk.kb.datahandler.preservica.AccessResponseObject;
import dk.kb.util.yaml.YAML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.TimerTask;

/**
 *
 */
public class PreservicaManifestationPlugin  implements Plugin{

    private String baseUrl;
    private String accessEndpoint;
    private String accessRefreshEndpoint;
    private String objectDetailsEndpoint;
    private String user;
    private String password;

    private String accessToken;
    private String refreshToken;

    private static final Logger log = LoggerFactory.getLogger(PreservicaManifestationPlugin.class);

    /**
     *
     */
    @Override
    public void apply() {
        log.info("Preservica Manifestation Plugin has been applied.");
        log.info("Access Token is: '{}'", accessToken);


    }

    /**
     *
     */
    public PreservicaManifestationPlugin(){
        YAML preservicaConfig = ServiceConfig.getConfig().getSubMap("preservica");
        this.baseUrl = preservicaConfig.getString("baseUrl");
        this.accessEndpoint = preservicaConfig.getString("accessEndpoint");
        this.accessRefreshEndpoint = preservicaConfig.getString("accessRefreshEndpoint");
        this.objectDetailsEndpoint = preservicaConfig.getString("objectDetailsEndpoint");
        this.user = preservicaConfig.getString("user");
        this.password = preservicaConfig.getString("password");

        // Schedule a task to refresh the token every 14 minutes. As the first accessToken can be used the delay is also 14 minutes.
        Timer timer = new Timer();
        timer.schedule(new RefreshTokenTask(), 14 * 60 * 1000, 14 * 60 * 1000);

        // Call method to get first accesToken and refreshToken.
        getInitialAccess();
    }

    /**
     *
     */
    private void getInitialAccess() {
        try {
            // Create URLConnection for access endpoint
            HttpURLConnection connection = getPreservicaAccessConnection();

            // Get response code and log a warning if not 200
            AccessResponseObject responseObject = getAccessResponseObject(connection);
            // Set accessToken and refreshToken from responseObject
            this.accessToken = responseObject.getToken();
            this.refreshToken = responseObject.getRefreshToken();
            // Close connection
            connection.disconnect();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     *
     * @return
     * @throws IOException
     */
    private HttpURLConnection getPreservicaAccessConnection() throws IOException {
        URL url = new URL(baseUrl + accessEndpoint);

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
     *
     * @return
     * @throws IOException
     */
    public HttpURLConnection refreshPreservicaAccessConnection() throws IOException {
        URL url = new URL(baseUrl + accessRefreshEndpoint + "?refreshToken=" + refreshToken);

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
     *
     * @param connection
     * @return
     * @throws IOException
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
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
