package dk.kb.datahandler.enrichment;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Java Object representaion of the json response from the metadata fragment webservices
 * Example
 * <pre>
 *     [
 *         {
 *          "metadata_fragment" : "<?xml version=\"1.0\" standalone=\"yes\"?><MetadataContainer schemaUri=\"http://id.kb.dk/schemas/supplementary_tvmeter_metadata\">..</MetadataContainer>"
 *          },
 *          {
 *           "metadata_fragment" : "<?xml version=\"1.0\" standalone=\"yes\"?><MetadataContainer schemaUri=\"http://id.kb.dk/schemas/supplementary_tvmeter_metadata\">..</MetadataContainer>"
 *           },
 *     ]
 * </pre>
 */
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
