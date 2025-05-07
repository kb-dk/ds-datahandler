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

    public enum RecordType {RADIO, TV, UNKNOWN}

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
        String cleanQName = cleanQName(qName);
        // TODO: Move logic for DR record to a new extending DrArchiveRecordHandler
        if (cleanQName.equalsIgnoreCase("publisher")) {
            isPublisher = true;
            publisherContent.setLength(0); // Reset content
        }

        if (cleanQName.equalsIgnoreCase("formatMediaType")) {
            isFormatMediaType = true;
            formatMediaTypeContent.setLength(0); // Reset content
        }

        if (cleanQName.equalsIgnoreCase("transcodingStatus")) {
            isTranscodingStatus = true;
            transcodingStatusContent.setLength(0); // reset content
        }

        if (cleanQName.equalsIgnoreCase("Metadata")) {
            isMetadata = true;

            // Check if the schemaUri attribute is present and is pbcore schema
            String schemaUri = attributes.getValue("schemaUri");
            if (schemaUri != null && schemaUri.equals(pbCoreSchemaUri)) {
                recordHasMetadata = true;
            }
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
        String cleanQName = cleanQName(qName);
        if (cleanQName.equalsIgnoreCase("publisher")) {
            // Check if the publisher content matches "dr" case-insensitively
            if (publisherContent.toString().toLowerCase(Locale.ROOT).startsWith("dr")) {
                recordIsDr = true;
            }
            isPublisher = false; // Reset the flag
        }

        if (cleanQName.equalsIgnoreCase("formatMediaType")) {
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

        if (cleanQName.equalsIgnoreCase("transcodingStatus")) {
            // Check what type of record we have in hand
            if (transcodingStatusContent.toString().equals("done")) {
                recordIsTranscoded = true;
            }

            isTranscodingStatus = false; // reset flag
        }

        if (cleanQName.equalsIgnoreCase("Metadata")) {
            isMetadata = false;
        }

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
    }

    public RecordType getRecordType() {

        if (recordType == null ) {
            log.warn("Record type was null, setting it to UNKNOWN");
            return RecordType.UNKNOWN;
        }

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

    /**
     * Preservica can sometimes deliver data, where namespace prefixes have been prefixed as part of the tag names. So that a {@code formatMediaType}-tag is present as a {@code
     * ns1:formatMediaType}-tag. These values are not equal, when parsed by this RecordHandler, therefore this method strips {@code QNames} if they contain the prefix.
     *
     * @return a clean qName
     */
    private static String cleanQName(String qName){
        if (qName.contains(":")){
            return qName.substring(qName.lastIndexOf(":")+1);
        }

        return qName;
    }
}
