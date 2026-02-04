package dk.kb.datahandler.preservica;

import dk.kb.datahandler.model.v1.OaiTargetDto;
import dk.kb.datahandler.oai.*;
import dk.kb.datahandler.util.PreservicaOaiRecordHandler;
import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.storage.model.v1.RecordTypeDto;
import dk.kb.util.Resolver;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("integration")
public class PreservicaDataTest {
    static final SAXParserFactory factory = SAXParserFactory.newInstance();


    @Test
    public void testPvicaOriginRadioDU() throws Exception{
        String xmlFile = "xml/pvica_origin_radio.xml";
        String xml = Resolver.resolveUTF8String(xmlFile);
        InputStream xmlInputStream = Resolver.resolveStream(xmlFile);
        OaiRecord record = new OaiRecord();
        record.setMetadata(xml);

        SAXParser saxParser = factory.newSAXParser();
        PreservicaOaiRecordHandler handler = new PreservicaOaiRecordHandler();

        saxParser.parse(xmlInputStream, handler);

        OaiResponseFilter oaiFilter = new OaiResponseFilterPreservicaSeven(null, null);
        String origin = oaiFilter.getOrigin(record, "preservica", handler);
        assertEquals("ds.radio", origin);
    }

    @Test
    public void testRecordForExtraNamespaceInFormatMediaType() throws Exception{
        String xml = Resolver.resolveUTF8String("xml/ebcbe76b-f130-4093-baa6-127fc7558fc5.xml");
        InputStream xmlInputStream = Resolver.resolveStream("xml/ebcbe76b-f130-4093-baa6-127fc7558fc5.xml");
        OaiRecord record = new OaiRecord();
        record.setMetadata(xml);

        SAXParser saxParser = factory.newSAXParser();
        PreservicaOaiRecordHandler handler = new PreservicaOaiRecordHandler();

        saxParser.parse(xmlInputStream, handler);

        OaiResponseFilter oaiFilter = new OaiResponseFilterPreservicaSeven(null, null);

        String origin = oaiFilter.getOrigin(record, "preservica", handler);
        assertEquals("ds.radio", origin);
    }

    @Test
    public void testPvicaOriginTvDU() throws Exception{
        String xmlFile = "xml/pvica_origin_tv.xml";
        String xml = Resolver.resolveUTF8String(xmlFile);
        InputStream xmlInputStream = Resolver.resolveStream(xmlFile);
        OaiRecord record = new OaiRecord();
        record.setMetadata(xml);

        SAXParser saxParser = factory.newSAXParser();
        PreservicaOaiRecordHandler handler = new PreservicaOaiRecordHandler();

        saxParser.parse(xmlInputStream, handler);

        OaiResponseFilter oaiFilter = new OaiResponseFilterPreservicaSeven(null, null);

        String origin = oaiFilter.getOrigin(record, "preservica", handler);
        assertEquals("ds.tv", origin);
    }

    @Test
    public void testRecordForTransformationErrors() throws IOException, SAXException, ParserConfigurationException {
        // Read XML as Document
        String xmlFile = "xml/pvica_failingRecord.xml";
        InputStream xml = Resolver.resolveStream(xmlFile);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(xml);

        //Empty test OAI Target
        OaiTargetDto oaiDto = new OaiTargetDto();
        OaiHarvestClient client = new OaiHarvestClient(oaiDto, "test");
        OaiRecord oaiRecord = client.extractRecordsFromXml(doc).get(0);
        String testStorageId = "ds.test:" + oaiRecord.getId();

        //Conversion from oaiRecord to dsRecord
        DsRecordDto dsRecord = new DsRecordDto();
        dsRecord.setId(testStorageId);
        dsRecord.setOrigin("ds.test");
        dsRecord.setData(oaiRecord.getMetadata());

        OaiResponseFilter oaiFilter = new OaiResponseFilterPreservicaSeven(null, null);
        RecordTypeDto resolvedType = oaiFilter.getRecordType(dsRecord, testStorageId);

        //Tests
        assertEquals(RecordTypeDto.DELIVERABLEUNIT, resolvedType);
        assertEquals("ds.test", dsRecord.getOrigin());
    }


}
