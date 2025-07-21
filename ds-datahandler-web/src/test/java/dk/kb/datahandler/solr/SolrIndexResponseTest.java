package dk.kb.datahandler.solr;

import com.fasterxml.jackson.core.JsonProcessingException;
import dk.kb.datahandler.util.SolrUtils;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SolrIndexResponseTest {
    @Test
    void testStringToObjectMapping() throws JsonProcessingException {
        String solrResponse = "{  \"responseHeader\":{    \"rf\":1,    \"status\":0,    \"QTime\":1348}}";
        SolrResponseHeader responseHeader = new SolrResponseHeader(solrResponse);

        SolrIndexResponse solrIndexResponse = new SolrIndexResponse();
        solrIndexResponse.setLastSolrResponseHeader(responseHeader);;

        assertEquals(solrIndexResponse.getLastSolrResponseHeader().getqTime(), 1348);

    }

    @Test
    void testObjectToString() throws JsonProcessingException {
        List<String> responses = new ArrayList<>();
        responses.add("{  \"responseHeader\":{    \"rf\":1,    \"status\":0,    \"QTime\":1348}}");
        responses.add("{  \"responseHeader\":{    \"rf\":1,    \"status\":0,    \"QTime\":1500}}");
        responses.add("{  \"responseHeader\":{    \"rf\":1,    \"status\":0,    \"QTime\":12}}");

        SolrIndexResponse indexResponse = new SolrIndexResponse();
        Long documents = 1231516L;


        for (String response: responses) {
            SolrUtils.updateFinalResponse(response, indexResponse, documents);
        }

        assertEquals("SolrIndexResponse{combinedQTime=2860, allDocumentsIndexed=1231516, lastResponseHeader=SolrResponseHeader{rf=1, status=0, qTime=12}}",
                    indexResponse.toString());


    }


}
