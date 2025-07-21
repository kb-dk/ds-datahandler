package dk.kb.datahandler.solr;

import dk.kb.datahandler.config.ServiceConfig;
import dk.kb.util.yaml.YAML;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static dk.kb.datahandler.util.SolrUtils.buildSuggestIndex;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SuggestIntegrationTest {
    @Test
    @Tag("integration")
    @Tag("slow")
    public void suggestTest() throws SolrServerException, IOException {
        // This test might be slow depending on the size of the index which the suggest index is build from
        YAML config = YAML.resolveLayeredConfigs("ds-datahandler-integration-test.yaml");
        ServiceConfig.setSolrWriteCollectionUrl(config.getString("solr.update.url"));

        QueryResponse response = buildSuggestIndex();
        // No methods in QueryResponse to extract the command-part.
        String responseString = response.toString();
        assertTrue(responseString.contains("command=build"));
    }
}
