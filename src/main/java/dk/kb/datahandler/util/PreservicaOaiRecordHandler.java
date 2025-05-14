package dk.kb.datahandler.util;

import dk.kb.datahandler.oai.OaiResponseFilterPreservicaSeven;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.Locale;

public class PreservicaOaiRecordHandler extends DefaultHandler {
    private static final Logger log = LoggerFactory.getLogger(OaiResponseFilterPreservicaSeven.class);


    public boolean recordIsTranscoded = false;
    public boolean recordHasMetadata = false;
    public boolean recordIsDr = false;
    public RecordType recordType = null;
    public String fileReference = null;

    public enum RecordType {RADIO, TV, UNKNOWN}

    private boolean isPublisher = false;
    private boolean isFormatMediaType = false;
    private boolean isTranscodingStatus = false;
    private boolean isTranscodingMetadata = false;
    private boolean isSpecificRadioTvTranscodingStatus = false;
    private boolean isAccessFilePath = false;

    // TODO: Could these be one stringbuilder which gets reset after each iteration
    private StringBuilder publisherContent = new StringBuilder();
    private StringBuilder formatMediaTypeContent = new StringBuilder();
    private StringBuilder transcodingStatusContent = new StringBuilder();
    private StringBuilder accessFilePathContent = new StringBuilder();

    private static final String pbCoreSchemaUri = "http://www.pbcore.org/PBCore/PBCoreNamespace.html";
    private static final String transcodingSchemaUri = "http://id.kb.dk/schemas/radiotv_access/transcoding_status";

    /**
     * This method is called when the parser encounters a start element in the XML document.
     * It processes specific start elements to prepare for the content that will follow.
     *
     * @param uri        The Namespace URI, or the empty string if the element has no Namespace.
     * @param localName  The local name (without prefix), or the empty string if Namespace processing is not being performed.
     * @param qName      The qualified name (with prefix), or the empty string if qualified names are not available.
     * @param attributes The attributes of the element, or <code>null</code> if the element has no attributes.
     *
     * <p>
     * The following start elements are processed:
     * </p>
     * <ul>
     *   <li><strong>publisher</strong></li>
     *   <li><strong>formatMediaType</strong></li>
     *   <li><strong>transcodingStatus</strong></li>
     *   <li><strong>Metadata</strong></li>
     * </ul>
     */
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
            // Check if the schemaUri attribute is present and is pbcore schema
            String schemaUri = attributes.getValue("schemaUri");
            if (schemaUri != null && schemaUri.equals(pbCoreSchemaUri)) {
                recordHasMetadata = true;
            }
            if (transcodingSchemaUri.equalsIgnoreCase(schemaUri)) {
                isTranscodingMetadata = true;
            }
        }

        if ("specificRadioTvTranscodingStatus".equalsIgnoreCase(qName)) {
            isSpecificRadioTvTranscodingStatus = true;
        }

        if ("accessFilePath".equalsIgnoreCase(qName)) {
            isAccessFilePath = true;
        }
    }

    /**
     * This method is called when the parser encounters an end element in the XML document.
     * It processes specific end elements to determine the state of the record being parsed.
     *
     * @param uri        The Namespace URI, or the empty string if the element has no Namespace.
     * @param localName  The local name (without prefix), or the empty string if Namespace processing is not being performed.
     * @param qName      The qualified name (with prefix), or the empty string if qualified names are not available.
     *
     * <p>
     * The following end elements are processed:
     * </p>
     *
     * <ul>
     *   <li>
     *     <strong>publisher</strong>: If the element is "publisher", the method checks if the
     *     publisher content starts with "dr" (case-insensitively). If it does, the flag
     *     <code>recordIsDr</code> is set to <code>true</code>
     *   </li>
     *   <li>
     *     <strong>formatMediaType</strong>: If the element is "formatMediaType", the method
     *     checks the type of the record based on the content. It sets the <code>recordType</code>
     *     based on the content value.
     *   </li>
     *   <li>
     *     <strong>transcodingStatus</strong>: If the element is "transcodingStatus", the method
     *     checks if the transcoding status is "done". If so, it sets the flag
     *     <code>recordIsTranscoded</code> to <code>true</code>.
     *   </li>
     * </ul>
     */
    @Override
    public void endElement(String uri, String localName, String qName) {
        if (qName.equalsIgnoreCase("publisher")) {
            // Check if the publisher content matches "dr" case-insensitively
            if (publisherContent.toString().toLowerCase(Locale.ROOT).startsWith("dr")) {
                recordIsDr = true;
            }
            isPublisher = false; // Reset the flag
        }

        if (qName.equalsIgnoreCase("formatMediaType")) {
            // Check what type of record we have in hand
            switch (formatMediaTypeContent.toString().toLowerCase(Locale.ROOT)){
                case "moving image":
                    recordType = RecordType.TV;
                    break;
                case "sound":
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
            if (transcodingStatusContent.toString().equals("done")) {
                recordIsTranscoded = true;
            }

            isTranscodingStatus = false; // reset flag
        }

        if (qName.equalsIgnoreCase("Metadata")) {
            isTranscodingMetadata= false;
        }

        if ("specificRadioTvTranscodingStatus".equalsIgnoreCase(qName)) {
            isSpecificRadioTvTranscodingStatus = false;
        }

        if ("accessFilePath".equalsIgnoreCase(qName)) {
            if (isTranscodingMetadata && isSpecificRadioTvTranscodingStatus) {
                fileReference = getFileReference(accessFilePathContent);
            }
            isAccessFilePath = false;
        }

    }

    private String getFileReference(StringBuilder accessFilePathContent) {
        String fileRef = accessFilePathContent.toString();
        int lastSlashIndex = fileRef.lastIndexOf("/");
        if (lastSlashIndex != -1) {
            fileRef = fileRef.substring(lastSlashIndex + 1);
        }
        int firstDotIndex = fileRef.indexOf(".");
        if (firstDotIndex != -1) {
            fileRef = fileRef.substring(0, firstDotIndex);
        }
        return fileRef;
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (isPublisher) {
            publisherContent.append(ch, start, length); // Collect publisher content
        }

        if (isFormatMediaType) {
            formatMediaTypeContent.append(ch, start, length); // Collect formatMediaType content
        }

        if (isTranscodingStatus) {
            transcodingStatusContent.append(ch, start, length); // Collect formatMediaType content
        }

        if (isTranscodingMetadata && isSpecificRadioTvTranscodingStatus && isAccessFilePath) {
            accessFilePathContent.append(ch, start, length);
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

    public String getFileReference() {
        return fileReference;
    }
}
