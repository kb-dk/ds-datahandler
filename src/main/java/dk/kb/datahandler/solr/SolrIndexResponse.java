package dk.kb.datahandler.solr;

import dk.kb.datahandler.model.v1.IndexTypeDto;

/**
 * An object used to create a response for the API method:
 * {@link dk.kb.datahandler.api.v1.impl.DsDatahandlerApiServiceImpl#indexSolr(String, Long, IndexTypeDto)}.
 * <p>
 * This object is used to deliver information on the completed indexing done through the aforementioned method.
 * The object contains the following information:
 * <ul>
 *     <li>{@link SolrIndexResponse#rf}: The rf value from the latest {@link SolrIndexResponse#lastResponseHeader }</li>
 *     <li>{@link SolrIndexResponse#lastStatus}: The status value from the latest {@link SolrIndexResponse#lastResponseHeader }</li>
 *     <li>{@link SolrIndexResponse#combinedQTime}: The combined QTime value from all indexed {@link SolrResponseHeader}</li>
 *     <li>{@link SolrIndexResponse#allDocumentsIndexed}: The combined number of documents indexed through the
 *     {@link dk.kb.datahandler.api.v1.impl.DsDatahandlerApiServiceImpl#indexSolr(String, Long, IndexTypeDto)} call.</li>
 *     <li>{@link SolrIndexResponse#lastResponseHeader}: The last solr response header added to the object. </li>
 * </ul>
 *
 */
public class SolrIndexResponse {
    public SolrIndexResponse() {
    }

    private Long rf;
    private Long lastStatus;
    private Long combinedQTime;
    private Long allDocumentsIndexed;
    private SolrResponseHeader lastResponseHeader;

    public SolrResponseHeader getLastResponseHeader() {
        return lastResponseHeader;
    }

    public void setLastResponseHeader(SolrResponseHeader lastResponseHeader) {
        this.lastResponseHeader = lastResponseHeader;
    }

    public Long getRf() {
        return rf;
    }

    public void setRf(Long rf) {
        this.rf = rf;
    }

    public Long getLastStatus() {
        return lastStatus;
    }

    public void setLastStatus(Long lastStatus) {
        this.lastStatus = lastStatus;
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
                "rf=" + rf +
                ", lastStatus=" + lastStatus +
                ", combinedQTime=" + combinedQTime +
                ", allDocumentsIndexed=" + allDocumentsIndexed +
                '}';
    }
}
