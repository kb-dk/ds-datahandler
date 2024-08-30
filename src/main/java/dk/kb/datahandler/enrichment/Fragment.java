package dk.kb.datahandler.enrichment;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Fragment {

    @JsonProperty("metadata_fragment")
    private String metadataFragment;

    public Fragment(@JsonProperty("metadata_fragment") String metadataFragment) {
        this.metadataFragment = metadataFragment;
    }

    public String getMetadataFragment() {
        return metadataFragment;
    }

    public void setMetadataFragment(String metadataFragment) {
        this.metadataFragment = metadataFragment;
    }
}
