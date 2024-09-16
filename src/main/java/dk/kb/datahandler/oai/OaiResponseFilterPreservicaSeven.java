package dk.kb.datahandler.oai;

import dk.kb.storage.invoker.v1.ApiException;
import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.storage.model.v1.RecordTypeDto;
import dk.kb.storage.util.DsStorageClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Filtering and delivery of Preservica OAI records from Preservica 7. Generates {@code datasource} prefixed IDs,
 * derives type from {@link DsRecordDto#getId()} and resolves {@code parent} from content.
 */
public class OaiResponseFilterPreservicaSeven extends OaiResponseFilter{
    private static final Logger log = LoggerFactory.getLogger(OaiResponseFilterPreservicaSeven.class);

    /**
     * Pattern for determining if a InformationObject from Preservica 7
     * contains metadata about a radio resource.
     * The regex handles the fact that namespace prefixes are arbitrarily defined.
     */
    protected static final Pattern RADIO_PATTERN = Pattern.compile(
            ">Sound</(?:\\w+:)?formatMediaType", Pattern.CASE_INSENSITIVE);

    /**
     * Pattern for determining if a InformationObject from Preservica 7
     * contains metadata about a television resource.
     * The regex handles the fact that namespace prefixes are arbitrarily defined.
     */
    protected static final Pattern TV_PATTERN = Pattern.compile(
            ">Moving\\sImage</(?:\\w+:)?formatMediaType", Pattern.CASE_INSENSITIVE);

    /**
     * Pattern used to check that records does in fact contain PBCore metadata.
     */
    static final Pattern METADATA_PATTERN = Pattern.compile(
            "<Metadata\\s+schemaUri=\"http://www\\.pbcore\\.org/PBCore/PBCoreNamespace\\.html\">");

    protected int emptyMetadataRecords = 0;

    /**
     * @param datasource source for records. Default implementation uses this for {@code origin}.
     * @param storage    destination for records.
     */
    public OaiResponseFilterPreservicaSeven(String datasource, DsStorageClient storage) {
        super(datasource, storage);
    }


    /**
     * Add records from Preservica OAI-PMH harvest to ds-storage. Records goes through a filtering where StructuralObjects
     * from Preservica are filtered away and not added to ds-storage. Furthermore, types are resolved based on IDs.
     * @param response      OAI-PMH response containing preservica records.
     */
    @Override
    public void addToStorage(OaiResponse response) throws ApiException {
        for (OaiRecord oaiRecord: response.getRecords()) {
            String xml = oaiRecord.getMetadata();
            String recordId = oaiRecord.getId();
            // Preservica StructuralObjects are ignored as they are only used as folders in the GUI.
            if (recordId.contains("oai:so")){
                //log.debug("Skipped Structural object with id: '{}'", recordId);
                continue;
            }
            // InformationObjects from preservcia 6/7 need to have the PBCore metadata tag.
            Matcher metadataMatcher = METADATA_PATTERN.matcher(xml);
            if ((recordId.contains("oai:io")) && !metadataMatcher.find()) {
                processed++;
                emptyMetadataRecords ++;
                log.warn("OAI-PMH record '{}' does not contain PBCore metadata and is therefore not added to storage. " +
                                "'{}' empty records have been found and '{}' records have been processed in total.",
                        recordId, emptyMetadataRecords, processed);
                continue;
            }

            try {
                addToStorage(oaiRecord);
                processed++;
            } catch (ApiException e){
                log.warn("DsStorage threw an exception when adding OAI record from Preservica 7 to storage.");
                throw e;
            }
        }
    }

    @Override
    public String getOrigin(OaiRecord oaiRecord, String datasource) {
        String xml = oaiRecord.getMetadata();

        Matcher radioDeliverableunitMatcher = RADIO_PATTERN.matcher(xml);
        Matcher tvDeliverableUnitMatcher = TV_PATTERN.matcher(xml);

        if (radioDeliverableunitMatcher.find()){
            return "ds.radio";
        } else if (tvDeliverableUnitMatcher.find()){
            return "ds.tv";
        } else {
            log.warn("No specific origin has been extracted for preservica record '{}'.",
                    oaiRecord.getId());
            // Not quite sure what we should do in the case where nothing gets matched.
            return "";
        }
    }


    /**
     * Determine the type of record in hand. For preservica 7 all records injected are most likely InformationObjects
     * which is a metadata record, mapping to our DELIVERABLEUNIT record type.
     * @param dsRecord to specify recordType for.
     * @param storageId used to define the type of record.
     */
    @Override
    public RecordTypeDto getRecordType(DsRecordDto dsRecord, String storageId) {
        if (storageId.contains("oai:io")){
            return RecordTypeDto.DELIVERABLEUNIT;
        }

        log.debug("Unable to derive record type for id '{}' from datasource '{}'. Falling back to '{}'", storageId, datasource, RecordTypeDto.DELIVERABLEUNIT);
        return RecordTypeDto.DELIVERABLEUNIT;
    }
}
