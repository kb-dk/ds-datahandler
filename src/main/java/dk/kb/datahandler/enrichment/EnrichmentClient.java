package dk.kb.datahandler.enrichment;

import dk.kb.storage.model.v1.DsRecordDto;

public class EnrichmentClient {

    private static EnrichmentClient instance;

    public static synchronized  EnrichmentClient getInstance() {
        if (instance == null) {
            instance = new EnrichmentClient();
        }
        return instance;
    }

    public String fetchEnrichmentData(DsRecordDto record) {
        // Fetch enrichment data from the webservice
        return "";
    }



}
