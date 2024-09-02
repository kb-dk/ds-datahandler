package dk.kb.datahandler.enrichment;

import dk.kb.datahandler.oai.OaiHarvestClient;
import dk.kb.datahandler.oai.OaiRecord;
import dk.kb.datahandler.util.PreservicaUtils;
import dk.kb.storage.model.v1.DsRecordDto;
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

public class DataEnricher {

    private static final Logger log = LoggerFactory.getLogger(DataEnricher.class);

    public static OaiRecord apply(OaiRecord record) throws ParserConfigurationException, IOException, SAXException, URISyntaxException, TransformerException {

        Document metadataDoc = XML.fromXML(record.getMetadata(),true);
        List<Fragment> fragments = FragmentsClient.getInstance().fetchMetadataFragments(extractOiId(record.getId()));
        //TODO if the fragment service returns 0 fragments for a (tv) record, it should fail.


        for (Fragment fragment : fragments) {
            Document fragmentDoc = XML.fromXML(fragment.getMetadataFragment(),true);
            fragmentDoc.getElementById("record");
            addMetadataFragments(metadataDoc, fragmentDoc);
        }

        record.setMetadata(XML.domToString(metadataDoc));
        return record;
    }


    //TODO remove
    public static DsRecordDto apply(DsRecordDto record) {
        Document recordData;
        try {
            recordData = XML.fromXML(record.getData(),true);
            List<Fragment> fragments = FragmentsClient.getInstance().fetchMetadataFragments(getIoId(record));
            if (!fragments.isEmpty()) {
                log.info("No fragments found "+record.getId());
            }
            for (Fragment fragment : fragments) {
                Document fragmentDoc = XML.fromXML(fragment.getMetadataFragment(),true);
                fragmentDoc.getElementById("record");
                addMetadataFragments(recordData, fragmentDoc);
            }
            record.setData(XML.domToString(recordData));
            // TODO error handling
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }
        //   String enrichedData = addMetadataFragments(recordData,fragments);
    //    record.setData(enrichedData);
        return record;
    }


    //TODO remove
    private static Document addMetadataFragments(Document record, Document fragments) {
        // TODO check if fragment exists
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

        } else {
            // propably should not happen
        }
        return record;
    }

    private static String extractOiId(String recordId) {
        String prefix = "oai:io";
        int lengthOfPrefix = prefix.length();
        int endOfPrefix = recordId.lastIndexOf(prefix);

        return recordId.substring(endOfPrefix + lengthOfPrefix);
    }

    //TODO remove
    static String getIoId(DsRecordDto dsRecord) {
        String prefix = ":oai:io:";
        int lengthOfPrefix = prefix.length();
        int endOfPrefix = dsRecord.getId().lastIndexOf(prefix);

        return dsRecord.getId().substring(endOfPrefix + lengthOfPrefix);
    }

}
