package dk.kb.datahandler.pvica;

import dk.kb.datahandler.model.v1.OaiTargetDto;
import dk.kb.datahandler.oai.*;
import dk.kb.storage.invoker.v1.ApiException;
import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.storage.model.v1.RecordTypeDto;
import dk.kb.storage.util.DsStorageClient;
import dk.kb.util.Resolver;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PvicaDataTest {
    
    @Test
    public void testDeliverableUnitNameSpaceFix() throws Exception{
        String xmlFile = "xml/pvica_deliverableunit_namespace_tofix.xml";
        String xml = Resolver.resolveUTF8String(xmlFile);        

        String xmlFixed= OaiHarvestClient.nameFixPvica(xml);
        assertTrue(xmlFixed.indexOf("<xip:DeliverableUnit xmlns:xip=\"http://www.tessella.com/XIP/v4\"") > 0);
        assertTrue(xmlFixed.indexOf("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"") > 0);            
    }

    @Test
    public void testFindPvicaParent() throws Exception{
        String xmlFile = "xml/pvica_parent_test.xml";        
        String xml = Resolver.resolveUTF8String(xmlFile);
        OaiRecord record = new OaiRecord();
        record.setMetadata(xml);
        OaiResponseFilter oaiFilter = new OaiResponseFilterPreservicaFive(null, null);
        String parent = oaiFilter.getParentID(record,"ds.radiotv");
        assertEquals("ds.radiotv:oai:du:9d9785a8-71f4-4b34-9a0e-1c99c13b001b", parent);        
    }

    @Test
    public void testPvicaOriginRadioDU() throws Exception{
        String xmlFile = "xml/pvica_origin_radio.xml";
        String xml = Resolver.resolveUTF8String(xmlFile);

        OaiRecord record = new OaiRecord();
        record.setMetadata(xml);
        OaiResponseFilter oaiFilter = new OaiResponseFilterPreservicaFive(null, null);
        String origin = oaiFilter.getOrigin(record, "preservica");
        assertEquals("ds.radio", origin);
    }
    @Test
    public void testPvicaOriginRadioManifestation() throws Exception{
        String xmlFile = "xml/pvica_origin_radio_manifestation.xml";
        String xml = Resolver.resolveUTF8String(xmlFile);

        OaiRecord record = new OaiRecord();
        record.setMetadata(xml);
        OaiResponseFilter oaiFilter = new OaiResponseFilterPreservicaFive(null, null);
        String origin = oaiFilter.getOrigin(record, "preservica");
        assertEquals("ds.radio", origin);
    }
    @Test
    public void testPvicaOriginTvDU() throws Exception{
        String xmlFile = "xml/pvica_origin_tv.xml";
        String xml = Resolver.resolveUTF8String(xmlFile);

        OaiRecord record = new OaiRecord();
        record.setMetadata(xml);
        OaiResponseFilter oaiFilter = new OaiResponseFilterPreservicaFive(null, null);
        String origin = oaiFilter.getOrigin(record, "preservica");
        assertEquals("ds.tv", origin);
    }
    @Test
    public void testPvicaOriginTvManifestation() throws Exception{
        String xmlFile = "xml/pvica_origin_tv_manifestation.xml";
        String xml = Resolver.resolveUTF8String(xmlFile);

        OaiRecord record = new OaiRecord();
        record.setMetadata(xml);
        OaiResponseFilter oaiFilter = new OaiResponseFilterPreservicaFive(null, null);
        String origin = oaiFilter.getOrigin(record, "preservica");
        assertEquals("ds.tv", origin);
    }

    @Test
    public void testRemovalOfCollectionsAndPreservationManifestations() throws ApiException {
        // We don't need a working storage to test this functionality. Therefore this mock
        DsStorageClient mockedStorage = Mockito.mock(DsStorageClient.class);
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

        OaiResponseFilter oaiFilter = new OaiResponseFilterPreservicaFive("preservica", mockedStorage);

        oaiFilter.addToStorage(testResponse);
        assertEquals(2, oaiFilter.getProcessed());
    }
    @Test
    public void testManifestationNameSpaceFix() throws Exception{
        String xmlFile = "xml/pvica_manifestation_namespace_to_fix.xml";
        String xml = Resolver.resolveUTF8String(xmlFile);

        String xmlFixed= OaiHarvestClient.nameFixPvica(xml);
        assertTrue(xmlFixed.indexOf("<xip:Manifestation xmlns:xip=\"http://www.tessella.com/XIP/v4\"") > 0);
    }

    @Test
    public void testRecordTypeCol() {
        DsRecordDto collectionRecord = new DsRecordDto();
        collectionRecord.setId("ds.test:oai:col:232234234");
        OaiResponseFilter oaiFilter = new OaiResponseFilterPreservicaFive(null, null);
        RecordTypeDto resolvedType = oaiFilter.getRecordType(collectionRecord, collectionRecord.getId());

        assertEquals(RecordTypeDto.COLLECTION, resolvedType);
    }
    @Test
    public void testRecordTypeDU() {
        DsRecordDto collectionRecord = new DsRecordDto();
        collectionRecord.setId("ds.test:oai:du:232234234");
        OaiResponseFilter oaiFilter = new OaiResponseFilterPreservicaFive(null, null);
        RecordTypeDto resolvedType = oaiFilter.getRecordType(collectionRecord, collectionRecord.getId());

        assertEquals(RecordTypeDto.DELIVERABLEUNIT, resolvedType);
    }
    @Test
    public void testRecordTypeMan() {
        DsRecordDto collectionRecord = new DsRecordDto();
        collectionRecord.setId("ds.test:oai:man:232234234");
        OaiResponseFilter oaiFilter = new OaiResponseFilterPreservicaFive(null, null);
        RecordTypeDto resolvedType = oaiFilter.getRecordType(collectionRecord, collectionRecord.getId());

        assertEquals(RecordTypeDto.MANIFESTATION, resolvedType);
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
        OaiTargetDto dto = new OaiTargetDto();
        OaiTargetJob job = new OaiTargetJob(1, dto);
        OaiHarvestClient client = new OaiHarvestClient(job, "test",null);
        OaiRecord oaiRecord = client.extractRecordsFromXml(doc).get(0);
        String testStorageId = "ds.test:" + oaiRecord.getId();

        //Conversion from oaiRecord to dsRecord
        DsRecordDto dsRecord = new DsRecordDto();
        dsRecord.setId(testStorageId);
        dsRecord.setOrigin("ds.test");
        dsRecord.setData(oaiRecord.getMetadata());

        OaiResponseFilter oaiFilter = new OaiResponseFilterPreservicaFive(null, null);
        RecordTypeDto resolvedType = oaiFilter.getRecordType(dsRecord, testStorageId);

        //Tests
        assertEquals(RecordTypeDto.DELIVERABLEUNIT, resolvedType);
        assertEquals("ds.test", dsRecord.getOrigin());
    }

}
