package dk.kb.datahandler.util;

import dk.kb.datahandler.oai.OaiResponseFilterPreservicaSeven;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class OaiRecordHandler extends DefaultHandler {
    private static final Logger log = LoggerFactory.getLogger(OaiResponseFilterPreservicaSeven.class);


    public boolean recordIsTranscoded = false;
    public boolean recordHasMetadata = false;
    public boolean recordIsDr = false;
    public RecordType recordType = null;

    public enum RecordType { RADIO, TV, UNKNOWN}


    // TODO: Move logic for DR record to a new extending DrArchiveRecordHandler
    private boolean isPublisher = false;
    private boolean isFormatMediaType = false;
    private boolean isTranscodingStatus = false;
    private boolean isMetadata = false;

    // TODO: Could these be one stringbuilder which gets reset after each iteration
    private StringBuilder publisherContent = new StringBuilder();
    private StringBuilder formatMediaTypeContent = new StringBuilder();
    private StringBuilder transcodingStatusContent = new StringBuilder();
    private StringBuilder metadataContent = new StringBuilder();

    private static final String pbCoreSchemaUri = "http://www.pbcore.org/PBCore/PBCoreNamespace.html";


    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        // TODO: Move logic for DR record to a new extending DrArchiveRecordHandler
        if (qName.equalsIgnoreCase("publisher")) {
            isPublisher = true;
            publisherContent.setLength(0); // Reset content
        }

        if (qName.equalsIgnoreCase("formatMediaType")) {
            isFormatMediaType = true;
            formatMediaTypeContent.setLength(0); // Reset content
        }

        if (qName.equalsIgnoreCase("transcodingStatus")) {
            isTranscodingStatus = true;
            transcodingStatusContent.setLength(0); // reset content
        }

        if (qName.equalsIgnoreCase("Metadata")) {
            isMetadata = true;

            // Check if the schemaUri attribute is present
            String schemaUri = attributes.getValue("schemaUri");
            if (schemaUri != null && schemaUri.equals(pbCoreSchemaUri)) {
                recordHasMetadata = true;
                log.info("Matched <Metadata> tag with schemaUri: " + schemaUri);
            } else {
                log.info("<Metadata> tag found, but schemaUri does not match.");
            }
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        // TODO: Move logic for DR record to a new extending DrArchiveRecordHandler
        if (qName.equalsIgnoreCase("publisher")) {
            // Check if the publisher content matches "dr" case-insensitively
            if (publisherContent.toString().toLowerCase().startsWith("dr")) {
                recordIsDr = true;
                log.info("Found publisher starting with 'dr': " + publisherContent);
            }
            isPublisher = false; // Reset the flag
        }

        if (qName.equalsIgnoreCase("formatMediaType")) {
            // Check what type of record we have in hand
            log.info("Found format: " + formatMediaTypeContent);

            switch (formatMediaTypeContent.toString().toLowerCase()){
                case "moving image":
                    recordType = RecordType.TV;
                    break;
                case "audio":
                    recordType = RecordType.RADIO;
                    break;
                default:
                    recordType = RecordType.UNKNOWN;
                    log.error("No recordType could be found for the record. Record cannot be sorted into a DS origin.");
            }

            isFormatMediaType = false; // reset flag
        }

        if (qName.equalsIgnoreCase("transcodingStatus")) {
            // Check what type of record we have in hand
            log.info("Found transcoding status: " + transcodingStatusContent);
            if (transcodingStatusContent.toString().equals("done")) {
                recordIsTranscoded = true;
            }

            isTranscodingStatus = false; // reset flag
        }

        if (qName.equalsIgnoreCase("Metadata")) {
            isMetadata = false;
        }

    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        // TODO: Move logic for DR record to a new extending DrArchiveRecordHandler
        if (isPublisher) {
            publisherContent.append(ch, start, length); // Collect publisher content
        }

        if (isFormatMediaType) {
            formatMediaTypeContent.append(ch, start, length); // Collect formatMediaType content
        }

        if (isTranscodingStatus) {
            transcodingStatusContent.append(ch, start, length); // Collect formatMediaType content
        }
    }

    public RecordType getRecordType() {
        return recordType;
    }

    public boolean isRecordDr() {
        return recordIsDr;
    }

    public boolean recordContainsMetadata() {
        return recordHasMetadata;
    }

    public boolean isRecordTranscoded() {
        return recordIsTranscoded;
    }
}
