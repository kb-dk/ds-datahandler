package dk.kb.datahandler.oai;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dk.kb.datahandler.util.PreservicaOaiRecordHandler;
import dk.kb.util.webservice.exception.InternalServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.storage.model.v1.RecordTypeDto;
import dk.kb.storage.util.DsStorageClient;
import dk.kb.util.webservice.exception.ServiceException;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Filtering and delivery of Preservica OAI records from Preservica 7. Generates {@code datasource} prefixed IDs,
 * derives type from {@link DsRecordDto#getId()} and resolves {@code parent} from content.
 */
public class OaiResponseFilterPreservicaSeven extends OaiResponseFilter{
    private static final Logger log = LoggerFactory.getLogger(OaiResponseFilterPreservicaSeven.class);
    static final SAXParserFactory factory = SAXParserFactory.newInstance();

    protected int emptyMetadataRecords = 0;

    protected int transCodingNotDoneRecords = 0;

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
    public void addToStorage(OaiResponse response) throws ServiceException {
        SAXParser saxParser = getSaxParser();

        for (OaiRecord oaiRecord: response.getRecords()) {
            try {
                PreservicaOaiRecordHandler handler = new PreservicaOaiRecordHandler();

                InputStream inputXml = new ByteArrayInputStream(oaiRecord.getMetadata().getBytes(StandardCharsets.UTF_8));
                saxParser.parse(inputXml, handler);

                String recordId = oaiRecord.getId();
                // Preservica StructuralObjects are ignored as they are only used as folders in the GUI.
                if (recordId.contains("oai:so")){
                    log.debug("Skipped Structural object with id: '{}'", recordId);
                    continue;
                }

                // InformationObjects from preservcia 6/7 need to have the PBCore metadata tag.
                if (!informationObjectContainsPbcoreBoolean(handler, recordId)){
                    continue;
                }

                if (!transcodingStatusIsDoneBoolean(handler, recordId)){
                    continue;
                }

                String origin = getOrigin(oaiRecord, datasource, handler);

                addToStorage(oaiRecord, origin, handler.fileReference);
                processed++;
            } catch (ServiceException e){
                log.warn("DsStorage threw an exception when adding OAI record from Preservica 7 to storage.");
                throw e;
            } catch (IOException | SAXException e) {
                throw new InternalServiceException("An error occurred when parsing XML with SAX:", e);
            }
        }
    }

    /**
     * Checks if the transcoding status is complete for the given XML string.
     *
     * <p>This method uses a regex pattern to determine if the transcoding status is marked as done in the XML.
     * If the status is not found, it increments the counters for processed records and those with
     * transcoding not done, logs a debug message, and, for every 1000 records filtered, logs an info message.
     * The method returns false if the transcoding status is not complete; otherwise, it returns true.</p>
     *
     * @param recordHandler The XML string containing the transcoding status to be checked.
     * @param recordId The unique identifier for the record being checked.
     * @return {@code true} if the transcoding status is complete; {@code false} if the status is not done.
     */
    boolean transcodingStatusIsDoneBoolean(PreservicaOaiRecordHandler recordHandler, String recordId) {
        if (!recordHandler.recordIsTranscoded){
            processed++;
            transCodingNotDoneRecords++;
            log.debug("OAI-PMH record '{}' transcoding status not done. Record skipped", recordId);
            if (transCodingNotDoneRecords % 1000 == 0) {
                log.info("'{}' records transcoding status not done filtered away. '{}' records have been processed.",
                        transCodingNotDoneRecords, processed);
            }
            return false;
        }

        return true;
    }


    /**
     * Checks if the given XML string contains PBCore metadata and that the record is an InformationObject.
     *
     * @param recordHandler containing a boolean describing if the record in hand contains PBCore metadata.
     * @param recordId The unique identifier for the record, which indicates if the record is an InformationObject.
     * @return {@code true} if the XML contains PBCore metadata or if the record ID does not indicate
     *         an OAI-PMH record; {@code false} if the record ID indicates an OAI-PMH record
     *         and PBCore metadata is not found.
     */
    boolean informationObjectContainsPbcoreBoolean(PreservicaOaiRecordHandler recordHandler, String recordId) {
        if (recordId.contains("oai:io") && !recordHandler.recordHasMetadata){
            processed++;
            emptyMetadataRecords ++;
            log.warn("OAI-PMH record '{}' does not contain PBCore metadata and is therefore not added to storage. " +
                            "'{}' empty records have been found and '{}' records have been processed in total.",
                    recordId, emptyMetadataRecords, processed);
            return false;
        }
        return true;
    }

    /**
     * Get DS origin from OaiRecordHandler which have streamed the content from the incoming OAI record.
     */
    @Override
    public String getOrigin(OaiRecord oaiRecord, String datasource, DefaultHandler recordHandler) {
        PreservicaOaiRecordHandler preservicaRecordHandler = (PreservicaOaiRecordHandler) recordHandler;

        switch (preservicaRecordHandler.getRecordType()) {
            case TV:
                return "ds.tv";
            case RADIO:
                return "ds.radio";
            case UNKNOWN:
                return "";
            default:
                throw new InternalServiceException("Unknown record type: " + preservicaRecordHandler.getRecordType());
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

    static SAXParser getSaxParser() {
        try {
            return factory.newSAXParser();
        } catch (ParserConfigurationException | SAXException e) {
            throw new InternalServiceException("An error occurred when constructing SAXParser: ", e);
        }
    }
}
