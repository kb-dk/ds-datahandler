package dk.kb.datahandler.oai;

/**
 * Class representing a record extracted from an OAI-PMH call.
 * The object contains the standard fields: {@code id}, {@code metadata}, {@code dateStamp} and {@code deleted}.
 */
public class OaiRecord {
    private String id;
    private String metadata;
    private String dateStamp;
    private boolean deleted=false;

    public OaiRecord () {        
    }


    public String getId() {
        return id;
    }


    public void setId(String id) {
        this.id = id;
    }


    public String getMetadata() {
        return metadata;
    }


    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }


    public boolean isDeleted() {
        return deleted;
    }


    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }


    public String getDateStamp() {
        return dateStamp;
    }


    public void setDateStamp(String dateStamp) {
        this.dateStamp = dateStamp;
    }


    @Override
    public String toString() {
        return "OaiRecord [id=" + id + ", metadata=" + metadata + ", dateStamp=" + dateStamp + ", deleted=" + deleted + "]";
    }
    
    
}
