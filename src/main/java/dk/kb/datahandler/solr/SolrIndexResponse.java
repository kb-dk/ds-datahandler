package dk.kb.datahandler.solr;

import dk.kb.datahandler.model.v1.IndexTypeDto;

/**
 * An object used to create a response for the API method:
 * {@link dk.kb.datahandler.api.v1.impl.DsDatahandlerApiServiceImpl#indexSolr(String, Long, IndexTypeDto)}.
 * <p>
 * This object is used to deliver information on the completed indexing done through the aforementioned method.
 * The object contains the following information:
 * <ul>
 *     <li>{@link SolrIndexResponse#combinedQTime}: The combined QTime value from all indexed {@link SolrResponseHeader}</li>
 *     <li>{@link SolrIndexResponse#allDocumentsIndexed}: The combined number of documents indexed through the
 *     {@link dk.kb.datahandler.api.v1.impl.DsDatahandlerApiServiceImpl#indexSolr(String, Long, IndexTypeDto)} call.</li>
 *     <li>{@link SolrIndexResponse#lastSolrResponseHeader}: The last solr response header added to the object. </li>
 * </ul>
 *
 */
public class SolrIndexResponse {
    public SolrIndexResponse() {
    }

    private Long combinedQTime = 0L;
    private Long allDocumentsIndexed;
    private SolrResponseHeader lastSolrResponseHeader;

    public SolrResponseHeader getLastSolrResponseHeader() {
        return lastSolrResponseHeader;
    }

    public void setLastSolrResponseHeader(SolrResponseHeader lastSolrResponseHeader) {
        this.lastSolrResponseHeader = lastSolrResponseHeader;
    }

    public Long getCombinedQTime() {
        return combinedQTime;
    }

    public void setCombinedQTime(Long combinedQTime) {
        this.combinedQTime = combinedQTime;
    }

    public Long getAllDocumentsIndexed() {
        return allDocumentsIndexed;
    }

    public void setAllDocumentsIndexed(Long allDocumentsIndexed) {
        this.allDocumentsIndexed = allDocumentsIndexed;
    }

    public void incrementCombinedQTime(Long combinedQTime){
        this.combinedQTime += combinedQTime;
    }

    public void incrementAllDocumentsIndexed(Long documentsIndexed){
        this.allDocumentsIndexed += documentsIndexed;
    }

    @Override
    public String toString() {
        return "SolrIndexResponse{" +
                "combinedQTime=" + combinedQTime +
                ", allDocumentsIndexed=" + allDocumentsIndexed +
                ", lastResponseHeader=" + "SolrResponseHeader{rf=" +
                                            lastSolrResponseHeader.getRf() +
                                        ", status=" + lastSolrResponseHeader.getStatus() +
                                        ", qTime=" + lastSolrResponseHeader.getqTime() +
                                        "}" +
                '}';
    }
}
