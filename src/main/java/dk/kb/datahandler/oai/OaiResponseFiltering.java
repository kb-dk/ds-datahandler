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
     * @param response      OAI-PMH response containing records.
     * @param dsAPI         api for storage.
     * @param datasource    where the harvested OAI-PMH record is extracted from.
     *                      This value is used as origin in DS-storage
     */
    public static void addToStorageWithoutFiltering(OaiResponse response, DsStorageApi dsAPI,
                                                    String datasource, AtomicInteger totalRecordsLoaded) throws ApiException {

        for (OaiRecord  oaiRecord : response.getRecords()) {
            totalRecordsLoaded.getAndAdd(1);
            String storageId=datasource+":"+oaiRecord.getId();
            if (oaiRecord.isDeleted()) { //mark for delete
                dsAPI.markRecordForDelete(storageId);
            } else { //Create or update
                addOrUpdateRecord(oaiRecord, storageId, null, datasource, dsAPI); //No parent
            }
        }
    }

    /**
     * Filter records from OAI-PMH before they are added to ds-storage.
     * This method checks for references to parent records in the record in hand.
     * @param response  OAI-PMH response containing pvica records.
     * @param dsAPI     api for storage.
     */
    public static void addToStorageWithPvicaFiltering(OaiResponse response, DsStorageApi dsAPI,
                                                      String origin, AtomicInteger totalRecordsLoaded) throws ApiException {
        for (OaiRecord  oaiRecord : response.getRecords()) {
            totalRecordsLoaded.getAndAdd(1);

            /*
            // This filtering has been rolled back, as it's not possible to correct the origins for all preservica reocrd types.
            String xmlContent = oaiRecord.getMetadata();
            String correctOrigin = getCorrectPvicaOrigin(xmlContent);
             */

            String storageId=origin+":"+oaiRecord.getId();

            if (oaiRecord.isDeleted()) { //mark for delete
                dsAPI.markRecordForDelete(storageId);
            } else { //Create or update
                String parent=getPvicaParent(oaiRecord.getMetadata(), origin);
                addOrUpdateRecord(oaiRecord, storageId, parent, origin, dsAPI);
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
     * For Pvica posts all 3 values can be defined.
     * For OAI records from other collections such is images, the default will be RecordTypeDto.DELIVERABLEUNIT.
     * 
     * 
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
        else {
            dsRecord.setRecordType(RecordTypeDto.DELIVERABLEUNIT);
        }
    }


    /**
     * If input XML is a preservica manifestation, then check for relations to a DeliverableUnit.
     * @param xml to check for parent record.
     * @param origin used to create correct ID for parent record.
     * @return the ID with origin prefixed for found parent record.
     */
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
          return origin+":oai:du:"+parent;
          
        }
        return null;
    }

    /*
     * THIS METHOD HAS BEEN COMMENTED OUT AS WE CANT SPLIT PRESERVICA CONTENT INTO MULTIPLE ORIGINS AS OF NOW.
     * Extract origin from Preservica XML by looking for the tag {@code formatMediaType} and then define origin based
     * on the value present.
     * @param xmlContent a preservica XML record delivered through a OAI harvest.
     * @return a value used as origin in ds-storage for preservica records.
    public static String getCorrectPvicaOrigin(String xmlContent) {
        if (xmlContent.contains("<formatMediaType>Sound</formatMediaType>")){
            return "ds.radio";
        } else if (xmlContent.contains("<formatMediaType>Moving Image</formatMediaType>")){
            return "ds.tv";
        } else {
            // Not quite sure what we should do in the case where nothing gets matched.
            return "";
        }
    }*/
    
    
}
