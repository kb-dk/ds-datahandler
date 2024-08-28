package dk.kb.datahandler.enrichment;

import dk.kb.storage.model.v1.DsRecordDto;

public class DataEnricher {

    public static DsRecordDto apply(DsRecordDto record) {
        String recordData = record.getData();
        String enrichmentData = EnrichmentClient.getInstance().fetchEnrichmentData(record);
        String mergeData = mergeData(recordData,enrichmentData);
        record.setData(mergeData);
        return record;
    }

    private static String mergeData(String recordData, String enrichmentData) {
        // TODO
        // Parse recordData
        // if recordata has no enrichemet data add enrichmentData
        // otherwise updata enrichment data
        return recordData;
    }

}
