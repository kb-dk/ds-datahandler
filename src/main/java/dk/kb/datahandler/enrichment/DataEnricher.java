package dk.kb.datahandler.enrichment;

import dk.kb.datahandler.oai.OaiRecord;
import dk.kb.util.xml.XML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

/**
 *
 */
public class DataEnricher {

    private static final Logger log = LoggerFactory.getLogger(DataEnricher.class);

    /**
     * Fetches metadata segments and adds them as <Metadata> elements of the XIP -node to the metdata of the oaiRecord
     * Note: only works of metadata is in XIP format.
     *
     * @param record OaiRecord to be enriched
     * @return the enriched records
     */
    public static OaiRecord apply(OaiRecord record)  {
        log.debug("Enriching {}",record.getId());

        Document metadataDoc;
        List<Fragment> fragments;
        try {
            metadataDoc = XML.fromXML(record.getMetadata(), true);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            log.warn("Unable to pass OAI record metadata {} ",record.getId(),e);
            throw new RuntimeException(e);
        }

        try {
             fragments = FragmentsClient.getInstance().fetchMetadataFragments(extractOiId(record.getId()));
             if (fragments.isEmpty()) {
                 log.debug("No fragments found for {}", record.getId());
             }
        } catch (URISyntaxException e) {
            log.warn("Unable to fetch metadata fragments for {}",record.getId(),e);
            throw new RuntimeException(e);
        }

        try {
            for (Fragment fragment : fragments) {
                Document fragmentDoc = XML.fromXML(fragment.getMetadataFragment(), true);
                fragmentDoc.getElementById("record");
                addMetadataFragments(metadataDoc, fragmentDoc);
                record.setMetadata(XML.domToString(metadataDoc));
            }
        } catch (ParserConfigurationException | IOException | SAXException | TransformerException e) {
            log.warn("Unable to add metadata fragments to {}",record.getId(),e);
            throw new RuntimeException(e);
        }
        return record;
    }

    private static void addMetadataFragments(Document record, Document fragments) {
        NodeList nodeList = record.getElementsByTagName("XIP");
        if (nodeList.getLength() > 0 ) {
            Node xipNode = nodeList.item(0);
            Element metadataNode = record.createElement("Metadata");

            // Copy attributes
            NamedNodeMap attributes = fragments.getDocumentElement().getAttributes();
            for (int i = 0; i < attributes.getLength(); i++) {
                Node attr = attributes.item(i);
                Attr importedAttr = (Attr) record.importNode(attr, true);
                if (importedAttr.getName().startsWith("xmlns") || importedAttr.getName().contains(":schemaLocation")) {
                    metadataNode.setAttributeNS(importedAttr.getNamespaceURI(), importedAttr.getName(), importedAttr.getValue());
                } else {
                    metadataNode.setAttributeNode(importedAttr);
                }
            }

            // COPY child nodes
            NodeList childNodes = fragments.getDocumentElement().getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node child = record.importNode(childNodes.item(i), true);
                metadataNode.appendChild(child);
            }
            xipNode.appendChild(metadataNode);

        }
    }

    private static String extractOiId(String recordId) {
        String prefix = "oai:io:";
        int lengthOfPrefix = prefix.length();
        int endOfPrefix = recordId.lastIndexOf(prefix);

        return recordId.substring(endOfPrefix + lengthOfPrefix);
    }


}
