package dk.kb.datahandler.solr;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;

/**
 * Original response header from solr. Received when performing a POST of documents to be indexed into the backing solr.
 */
public class SolrResponseHeader {
    private Integer rf;
    private Integer status;
    private Long qTime;

    /**
     * Construct a {@code SolrResponseHeader} from a string representation of the response in the format:
     * <pre>
     * {"responseHeader": {
     *   "rf":1,
     *   "status":0,
     *   "QTime":1348}
     *   }
     *</pre>
     * @param solrResponseHeader The string representation of a solr response header which the {@code SolrResponseHeader}
     *                           is created from.
     */
    public SolrResponseHeader(String solrResponseHeader) throws JsonProcessingException {
        HashMap<String,Integer> responseHeader =
                (HashMap<String, Integer>) new ObjectMapper().readValue(solrResponseHeader, HashMap.class).get("responseHeader");

        this.rf = responseHeader.get("rf");
        this.status = responseHeader.get("status");
        this.qTime = Long.valueOf(responseHeader.get("QTime"));
    }

    public Integer getRf() {
        return rf;
    }

    public void setRf(Integer rf) {
        this.rf = rf;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Long getqTime() {
        return qTime;
    }

    public void setqTime(Long qTime) {
        this.qTime = qTime;
    }
}
