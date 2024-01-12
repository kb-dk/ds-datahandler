package dk.kb.datahandler.solr;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SolrIndexResponseTest {
    @Test
    void testStringToObjectMapping() throws JsonProcessingException {
        String solrResponse = "{  \"responseHeader\":{    \"rf\":1,    \"status\":0,    \"QTime\":1348}}";
        SolrResponseHeader responseHeader = new SolrResponseHeader(solrResponse);

        SolrIndexResponse solrIndexResponse = new SolrIndexResponse();
        solrIndexResponse.setLastResponseHeader(responseHeader);;

        assertEquals(solrIndexResponse.getLastResponseHeader().getqTime(), 1348);

    }

    @Test
    void testJsonStringToHashMap() throws JsonProcessingException {
        String solrResponse = "{  \"responseHeader\":{    \"rf\":1,    \"status\":0,    \"QTime\":1348}}";

        HashMap<String,Long> currentResponseHeader =
                (HashMap<String, Long>) new ObjectMapper().readValue(solrResponse, HashMap.class).get("responseHeader");

        System.out.println(currentResponseHeader);
    }
}
