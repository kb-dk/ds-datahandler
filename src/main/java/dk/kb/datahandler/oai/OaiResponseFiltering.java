package dk.kb.datahandler.oai;

import dk.kb.datahandler.facade.DsDatahandlerFacade;
import dk.kb.storage.client.v1.DsStorageApi;
import dk.kb.storage.invoker.v1.ApiException;
import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.storage.model.v1.RecordTypeDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

public class OaiResponseFiltering {

    private static final Logger log = LoggerFactory.getLogger(DsDatahandlerFacade.class);

    /**
     * Add record from OAI-PMH harvest to ds-storage without applying any filtering.
     * @param response  OAI-PMH response containing records.
     * @param dsAPI     api for storage.
     * @param origin    where the harvested OAI-PMH record is extracted from.
     * @return          the number of records loaded in total.
     */
    public static void addToStorageWithoutFiltering(OaiResponse response, DsStorageApi dsAPI,
                                                    String origin, AtomicInteger totalRecordsLoaded) throws ApiException {

        for (OaiRecord  oaiRecord : response.getRecords()) {
            totalRecordsLoaded.getAndAdd(1);
            String storageId=origin+":"+oaiRecord.getId();
            if (oaiRecord.isDeleted()) { //mark for delete
                dsAPI.markRecordForDelete(storageId);
            } else { //Create or update
                addOrUpdateRecord(oaiRecord, storageId, null, origin, dsAPI); //No parent
            }
        }
    }

    /**
     * Add or update record in ds-storage.
     * @param oaiRecord record from OAI-PMH to ingest to ds-storage.
     * @param storageId id given to the record in ds-storage.
     * @param origin    The origin, which the OAI record is extracted from,
     * @param dsAPI     The ds storage API, which the oaiRecord is added to.
     */
    private static void addOrUpdateRecord(OaiRecord oaiRecord, String storageId, String parent, String origin,
                                         DsStorageApi dsAPI) throws ApiException {
        DsRecordDto dsRecord = new DsRecordDto();
        dsRecord.setId(storageId);
        dsRecord.setOrigin(origin);
        dsRecord.setData(oaiRecord.getMetadata());
        dsRecord.setParentId(parent); //Does not matter if null is set
        setRecordType(dsRecord, storageId);
        dsAPI.recordPost(dsRecord);
    }

    /**
     * Define RecordType from id.
     * @param dsRecord to specify recordType for.
     * @param storageId used to define the type of record.
     */
    public static void setRecordType(DsRecordDto dsRecord, String storageId) {
        if (storageId.contains("oai:col")){
            dsRecord.setRecordType(RecordTypeDto.COLLECTION);
        } else if (storageId.contains("oai:du")) {
            dsRecord.setRecordType(RecordTypeDto.DELIVERABLEUNIT);
        } else if (storageId.contains("oai:man")) {
            dsRecord.setRecordType(RecordTypeDto.MANIFESTATION);
        }
    }

    
    //If xip:Manifestation record, return value of field  <ManifestationRef>.
    public static String getPvicaParent(String xml, String origin) {
        
         if (!xml.contains("<xip:Manifestation")) { 
          return null;    
        }
        
        String start="<DeliverableUnitRef>";                    
        String end="</DeliverableUnitRef>";
        int indexStart=xml.indexOf(start);
        int indexEnd=xml.indexOf(end);        
        if (indexStart >= 0 && indexEnd >0){
            String parent=xml.substring(indexStart+start.length() , indexEnd);                   
          if (parent.length() < 30 || parent.length() > 40){
              log.warn("ParentID does not seem to have correct format:+parent");
          }                      
          return parent=origin+":oai:du:"+parent;
          
        }
        return null;
        
    }
    
    
}
