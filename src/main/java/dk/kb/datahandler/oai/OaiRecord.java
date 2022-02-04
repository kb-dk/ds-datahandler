package dk.kb.datahandler.oai;

public class OaiRecord {
private String id;
private String metadata;
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
    


}
