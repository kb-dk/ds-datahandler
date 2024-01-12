package dk.kb.datahandler.solr;

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
}
