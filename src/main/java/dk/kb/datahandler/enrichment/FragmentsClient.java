package dk.kb.datahandler.enrichment;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.kb.datahandler.config.ServiceConfig;
import org.apache.http.client.utils.URIBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class FragmentsClient {

    private static FragmentsClient instance;

    private String baseUrl;

    public static synchronized FragmentsClient getInstance() {
        if (instance == null) {
            instance = new FragmentsClient(ServiceConfig.getConfig().getString("fragmentService.baseUrl"));
        }
        return instance;
    }

    public FragmentsClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public List<Fragment> fetchMetadataFragments(String id) throws IOException, URISyntaxException {
        // Fetch enrichment data from the webservice
        HttpURLConnection connection = getConnection(id);
        if (connection.getResponseCode() != 200){
            //do some error handling
            return null;
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
        String inputLine;
        StringBuilder response = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        ObjectMapper objectMapper = new ObjectMapper();
        List<Fragment> fragments = objectMapper.readValue(response.toString(), new TypeReference<List<Fragment>>(){});
        return fragments;
    }


    private HttpURLConnection getConnection(String id) throws URISyntaxException, MalformedURLException {
        URL url = new URIBuilder(baseUrl)
                .setPathSegments("fragments",id)
                .build().toURL();
        HttpURLConnection connection;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("accept", "application/json");
            return connection;
        } catch (ProtocolException e) {
            return null;
        } catch (IOException e) {
            return null;
        }
    }


}
