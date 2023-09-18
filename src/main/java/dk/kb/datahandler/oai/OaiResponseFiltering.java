package dk.kb.datahandler.oai;

import dk.kb.datahandler.facade.DsDatahandlerFacade;
import dk.kb.storage.client.v1.DsStorageApi;
import dk.kb.storage.invoker.v1.ApiException;
import dk.kb.storage.model.v1.DsRecordDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OaiResponseFiltering {
    private static final Logger log = LoggerFactory.getLogger(DsDatahandlerFacade.class);


    /**
     * Add record from OAI-PMH harvest to ds-storage without applying any filtering.
     * @param response  OAI-PMH response containing records.
     * @param dsAPI     api for storage.
     * @param origin    where the harvested OAI-PMH record is extracted from.
     * @return          the number of records loaded in total.
     * @throws ApiException
     */
    public static int addToStorageWithoutFiltering(OaiResponse response, DsStorageApi dsAPI,
                                                    String origin, int totalRecordsLoaded) throws ApiException {

        for (OaiRecord  oaiRecord : response.getRecords()) {
            totalRecordsLoaded++;
            String storageId=origin+":"+oaiRecord.getId();
            if (oaiRecord.isDeleted()) { //mark for delete
                dsAPI.markRecordForDelete(storageId);
            } else { //Create or update
                addOrUpdateRecord(oaiRecord, storageId, origin, dsAPI);
            }
        }
        log.info("Ingesting '{}' records from origin: '{}' out of a total of '{}' records.",
                totalRecordsLoaded, origin, response.getTotalRecords());
        return totalRecordsLoaded;
    }

    /**
     * Filter records from OAI-PMH before they are added to ds-storage. This method filters preservica records
     * containing xip:Collection files, as these contain meta-metadata, which are not used in the ds-ecosystem.
     * @param response  OAI-PMH response containing pvica records.
     * @param dsAPI     api for storage.
     * @param origin    where the harvested OAI-PMH record is extracted from.
     */
    public static int addToStorageWithPvicaFiltering(OaiResponse response, DsStorageApi dsAPI,
                                                      String origin, int totalRecordsLoaded) throws ApiException {
        int xipCollections = 0;
        for (OaiRecord  oaiRecord : response.getRecords()) {
            totalRecordsLoaded++;
            String storageId=origin+":"+oaiRecord.getId();
            if (oaiRecord.getMetadata().contains("<xip:Collection")){
                // XIP:Collections do not provide any needed metadata. Therefore, they are not added to ds-storage.
                xipCollections ++;
            } else if (oaiRecord.isDeleted()) { //mark for delete
                dsAPI.markRecordForDelete(storageId);
            } else { //Create or update
                addOrUpdateRecord(oaiRecord, storageId, origin, dsAPI);
            }
        }
        log.info("Ingesting '{}' records from origin: '{}' out of a total of '{}' records. " +
                        "'{}' xip:Collections have been skipped",
                totalRecordsLoaded, origin, response.getTotalRecords(), xipCollections);
        return totalRecordsLoaded;
    }

    /**
     * Add or update record in ds-storage.
     * @param oaiRecord record from OAI-PMH to ingest to ds-storage.
     * @param storageId id given to the record in ds-storage.
     * @param origin    The origin, which the OAI record is extracted from,
     * @param dsAPI     The ds storage API, which the oaiRecord is added to.
     */
    public static void addOrUpdateRecord(OaiRecord oaiRecord, String storageId, String origin,
                                         DsStorageApi dsAPI) throws ApiException {
        DsRecordDto dsRecord = new DsRecordDto();
        dsRecord.setId(storageId);
        dsRecord.setOrigin(origin);
        dsRecord.setData(oaiRecord.getMetadata());
        dsAPI.recordPost(dsRecord);
    }

}
