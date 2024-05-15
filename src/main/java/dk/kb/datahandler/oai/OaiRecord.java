package dk.kb.datahandler.oai;

/**
 * Class representing a record extracted from an OAI-PMH call.
 * The object contains the standard fields: {@code id}, {@code metadata}, {@code dateStamp} and {@code deleted}.
 * Furthermore, a non-required and non-OAI attribute named {@code manifestationId} is included. This is used by the
 * {@link dk.kb.datahandler.oai.plugins.PreservicaManifestationPlugin} for extracting manifestations.
 */
public class OaiRecord {
    private String id;
    private String metadata;
    private String dateStamp;
    private boolean deleted=false;
    private String manifestationId;



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

    public String getManifestationId() {
        return manifestationId;
    }

    public void setManifestationId(String manifestationId) {
        this.manifestationId = manifestationId;
    }
}
