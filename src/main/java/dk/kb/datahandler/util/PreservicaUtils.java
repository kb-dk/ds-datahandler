package dk.kb.datahandler.util;

import dk.kb.datahandler.preservica.PreservicaManifestationExtractor;
import dk.kb.storage.invoker.v1.ApiException;
import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.storage.util.DsStorageClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class PreservicaUtils {

    private static final Logger log = LoggerFactory.getLogger(PreservicaUtils.class);

    /**
     * Initialize a {@link PreservicaManifestationExtractor} which fetches a presentation representation through the
     * Preservica 7 APIs and sets the id for the representation as referenceId for the input record.
     * @param record to get manifestation for.
     * @return the updated record with presentation copy as reference id.
     */
    public static DsRecordDto fetchManifestation(DsRecordDto record, PreservicaManifestationExtractor plugin, AtomicInteger counter, AtomicLong currentTime) {
        counter.getAndIncrement();

        if (counter.get() % 200 == 0){
            log.info("200 Records have been updated in '{}' milliseconds. In total '{}' records have been processed.",
                    System.currentTimeMillis() - currentTime.get(), counter.get());
            currentTime.set(System.currentTimeMillis());
        }
        record = plugin.apply(record);

        return record;
    }

    /**
     * Streaming wrapper for the recordPost method of the {@link DsStorageClient}
     * @param storageClient to post the record to.
     * @param record to post.
     */
    public static void safeRecordPost(DsStorageClient storageClient, DsRecordDto record) {
        try {
            storageClient.recordPost(record);
        } catch (ApiException e) {
            log.error("ApiException has been thrown. Record probably already exists.");
        }
    }

    /**
     * Extract Preservica ID for an InformationObject from {@link DsRecordDto} ID.
     * @param dsRecord with an ID in the format ds.tv:oai:io:3006e2f8-3f73-477a-a504-4d7cb1ae1e1c
     * @return a
     */
    public static String getPreservicaIoId(DsRecordDto dsRecord) {
        String prefix = ":oai:io:";
        int lengthOfPrefix = prefix.length();
        int endOfPrefix = dsRecord.getId().lastIndexOf(prefix);

        return dsRecord.getId().substring(endOfPrefix + lengthOfPrefix);
    }

    /**
     * Filter records for validity.
     * @param record to ensure has values correctly set.
     * @return true if record is valid.
     */
    public static boolean validateRecord(DsRecordDto record) {
        return record.getReferenceId() != null && !record.getReferenceId().isEmpty();
    }


    /**
     * Parse the response from {@link dk.kb.datahandler.preservica.client.DsPreservicaClient#getAccessRepresentationForIO(String)}
     * and extract the id of the returned ContentObject.
     * @param xml an {@link InputStream} containing a RepresentationResponse for an InformationObject.
     * @return the ID of the ContentObject related to the newest access representation for an InformationObject.
     */
    public static String parseRepresentationResponseForContentObject(InputStream xml) throws XMLStreamException {
        // Create an XMLEventReader
        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLEventReader eventReader = factory.createXMLEventReader(xml);

        // Variables to hold data
        String elementName;
        String contentObject = "";
        boolean isInRepresentation = false;
        boolean isContentObject = false;

        // Loop through the XML events
        while (eventReader.hasNext()) {
            XMLEvent event = eventReader.nextEvent();
            if (event.isStartElement()) {
                StartElement startElement = event.asStartElement();
                elementName = startElement.getName().getLocalPart();
                if (elementName.equals("Representation")) {
                    isInRepresentation = true;
                }
                if (isInRepresentation && elementName.equals("ContentObject")) {
                    isContentObject = true;
                }
            } else if (event.isCharacters() && isContentObject) {
                Characters characters = event.asCharacters();
                if (!characters.isWhiteSpace()) {
                    contentObject = characters.getData();
                }
            } else if (event.isEndElement()) {
                elementName = event.asEndElement().getName().getLocalPart();
                if ("ContentObject".equals(elementName) && isContentObject) {
                    isContentObject = false;
                    if (contentObject.isEmpty()){
                        log.warn("No ContentObjects have been found in the parsed XML.");
                    }
                }
                if ("Representation".equals(elementName) && isInRepresentation) {
                    isInRepresentation = false;
                    if (contentObject.isEmpty()){
                        log.warn("No Representation tags have been found in the parsed XML.");
                    }
                }
            }
        }

        return contentObject;
    }

    /**
     * Parse the response from {@link dk.kb.datahandler.preservica.client.DsPreservicaClient#getFileRefForContentObject(String)}
     * and extract the fileRef of the returned ContentObject. This fileRef represents the name of the representation on
     * the server and is NOT the same as the name of the bitstream, eventhough the bitstream streams the file from the
     * file referenced by the fileRef extracted here.
     * @param xml an {@link InputStream} containing a IdentifierResponse for a ContentObject.
     * @return the fileRef for the file on the server representing the ContentObject put into the method.
     */
    public static String parseIdentifierResponseForFileRef(InputStream xml) throws XMLStreamException {
        // Create an XMLEventReader
        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLEventReader eventReader = factory.createXMLEventReader(xml);

        // Variables to hold data
        String elementName;
        String fileRef = "";
        boolean isCorrectType = false;
        boolean isIdentifer = false;
        boolean isFileRef = false;

        // Loop through the XML events
        while (eventReader.hasNext()) {
            XMLEvent event = eventReader.nextEvent();
            if (event.isStartElement()) {
                StartElement startElement = event.asStartElement();
                elementName = startElement.getName().getLocalPart();
                if (elementName.equals("Identifier")) {
                    isIdentifer = true;
                }

                if (isIdentifer && elementName.equals("Type")){
                    event = eventReader.nextEvent();
                    if (event.isCharacters()) {
                        Characters characters = event.asCharacters();
                        if ("co_v4_fileRef".equals(characters.getData())) {
                            isFileRef = true;
                        }
                    }
                }
            } else if (event.isCharacters() && isIdentifer && isFileRef) {
                Characters characters = event.asCharacters();
                if (!characters.isWhiteSpace()) {
                    fileRef = characters.getData();
                }
            } else if (event.isEndElement()) {
                elementName = event.asEndElement().getName().getLocalPart();
                if (elementName.equals("Identifier") && isIdentifer) {
                    isIdentifer = false;
                    if (fileRef.isEmpty()){
                        log.warn("No fileRef have been found in the parsed XML.");
                    }
                }
                if (elementName.equals("Value") && isFileRef) {
                    isFileRef = false;
                    if (fileRef.isEmpty()){
                        log.warn("No fileRef have been found in the parsed XML.");
                    }
                }
            }
        }

        return fileRef;
    }

}
