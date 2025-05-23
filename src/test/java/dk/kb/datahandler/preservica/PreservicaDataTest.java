package dk.kb.datahandler.preservica;

import dk.kb.datahandler.job.JobCache;
import dk.kb.datahandler.model.v1.DsDatahandlerJobDto;
import dk.kb.datahandler.model.v1.OaiTargetDto;
import dk.kb.datahandler.oai.*;
import dk.kb.datahandler.util.PreservicaOaiRecordHandler;
import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.storage.model.v1.RecordTypeDto;
import dk.kb.util.Resolver;
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
        DsDatahandlerJobDto job = JobCache.createNewOaiJob(oaiDto,null);
        OaiHarvestClient client = new OaiHarvestClient(job,oaiDto, "test");
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

    /**
     * Create an OAI test response containing two InformationObjects.
     * @return OAI Response with two fake IO records.
     */
    private static OaiResponse createTestOaiResponseWithIOs() {
        // Create OAI-PMH test records.
        OaiRecord informationObject1 = new OaiRecord();
        informationObject1.setId("oai:io:12345678-test-test-test-testtest1111");
        informationObject1.setMetadata("<Metadata schemaUri=\"http://www.pbcore.org/PBCore/PBCoreNamespace.html\">"+
                "<formatMediaType>Sound</formatMediaType>");
        OaiRecord informationObject22 = new OaiRecord();
        informationObject22.setId("oai:io:12345678-test-test-test-testtest2222");
        informationObject22.setMetadata("<Metadata schemaUri=\"http://www.pbcore.org/PBCore/PBCoreNamespace.html\">"+
                "<formatMediaType>Sound</formatMediaType>");

        ArrayList<OaiRecord> oaiRecords = new ArrayList<>();
        oaiRecords.add(informationObject1);
        oaiRecords.add(informationObject22);
        // Create test OaiResponse.
        OaiResponse testResponse = new OaiResponse();
        testResponse.setRecords(oaiRecords);
        return testResponse;
    }

    private static OaiResponse createPreserviceFiveOaiResponse() {
        // Create OAI-PMH test records.
        OaiRecord deliverableUnit1 = new OaiRecord();

        deliverableUnit1.setId("oai:du:12345678-test-test-test-testtest1111");
        deliverableUnit1.setMetadata("<Metadata schemaURI=\"http://www.pbcore.org/PBCore/PBCoreNamespace.html\">"+
                "<formatMediaType>Sound</formatMediaType>");
        OaiRecord deliverableUnit2 = new OaiRecord();
        deliverableUnit2.setId("oai:du:12345678-test-test-test-testtest2222");
        deliverableUnit2.setMetadata("<Metadata schemaURI=\"http://www.pbcore.org/PBCore/PBCoreNamespace.html\">"+
                "<formatMediaType>Sound</formatMediaType>");
        OaiRecord preservationManifestation = new OaiRecord();
        preservationManifestation.setId("oai:man:12345678-test-test-test-testtest2222");
        preservationManifestation.setMetadata("<ManifestationRelRef>1</ManifestationRelRef>");
        OaiRecord collection = new OaiRecord();
        collection.setId("oai:col:123456-test-1234");
        // Create ArrayList of OAI records.
        ArrayList<OaiRecord> oaiRecords = new ArrayList<>();
        oaiRecords.add(collection);
        oaiRecords.add(deliverableUnit1);
        oaiRecords.add(deliverableUnit2);
        oaiRecords.add(preservationManifestation);
        // Create test OaiResponse.
        OaiResponse testResponse = new OaiResponse();
        testResponse.setRecords(oaiRecords);
        return testResponse;
    }

}
