package dk.kb.datahandler.enrichment;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

/*
 *  Client for accessing metadata fragments for enrichment (from webservices)
 */
public class FragmentsClient {
    private static final Logger log = LoggerFactory.getLogger(FragmentsClient.class);

    private static FragmentsClient instance;

    private final String baseUrl;
    private final int maxRetries;

    public static synchronized FragmentsClient getInstance(String baseUrl) {
        if (instance == null || !baseUrl.equals(instance.getBaseUrl())) {
            instance = new FragmentsClient(baseUrl,5);
        }
        return instance;
    }

    public FragmentsClient(String baseUrl,int maxRetries) {
        this.maxRetries = maxRetries;
        this.baseUrl = baseUrl;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Fetches metadata fragments for a information object
     *
     * @param id the id of the object
     * @return A list of metadata fragments
     * @throws URISyntaxException
     */
    public List<Fragment> fetchMetadataFragments(String id) throws  URISyntaxException, IOException {
        int attempt = 0;
        while (attempt < maxRetries) {
            try {
                HttpURLConnection connection = getConnection(id);

                int status = connection.getResponseCode();
                if (status != 200) {
                    throw new IOException("Failed to fetch metadata fragments, HTTP response code: " + status);
                }

                try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String response = readResponse(in);
                    ObjectMapper objectMapper = new ObjectMapper();
                    return objectMapper.readValue(response, new TypeReference<List<Fragment>>(){});
                }
            } catch (IOException e) {
                log.warn("Fragments client connection failed "+e.getMessage());
                attempt++;
            }
        }
        throw new IOException("Failed to fetch fragments for id:"+id+" after "+maxRetries+" retries");
    }

    protected HttpURLConnection getConnection(String id) throws URISyntaxException, IOException {
        URL url = new URIBuilder(baseUrl)
                .setPathSegments("fragments", id)
                .build().toURL();

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("accept", "application/json");
        return connection;
    }

    private String readResponse(BufferedReader in) throws IOException {
        StringBuilder response = new StringBuilder();
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        return response.toString();
    }
}
