package dk.kb.datahandler.pvica;

import static dk.kb.datahandler.oai.OaiResponseFiltering.setRecordType;
import static org.junit.jupiter.api.Assertions.*;


import dk.kb.datahandler.model.v1.OaiTargetDto;
import dk.kb.datahandler.oai.OaiRecord;
import dk.kb.datahandler.oai.OaiTargetJob;
import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.storage.model.v1.RecordTypeDto;
import org.junit.jupiter.api.Test;

import dk.kb.datahandler.oai.OaiHarvestClient;
import dk.kb.datahandler.oai.OaiResponseFiltering;
import dk.kb.util.Resolver;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

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
        String parent = OaiResponseFiltering.getPvicaParent(xml,"ds.radiotv");
        assertEquals("ds.radiotv:oai:du:9d9785a8-71f4-4b34-9a0e-1c99c13b001b", parent);        
    }

    @Test
    public void testGetPvicaOriginRadio() throws Exception{
        String xmlFile = "xml/pvica_origin_radio.xml";
        String xml = Resolver.resolveUTF8String(xmlFile);
        String origin = OaiResponseFiltering.getCorrectPvicaOrigin(xml);
        assertEquals("ds.radio", origin);
    }
    @Test
    public void testGetPvicaOriginTv() throws Exception{
        String xmlFile = "xml/pvica_origin_tv.xml";
        String xml = Resolver.resolveUTF8String(xmlFile);
        String origin = OaiResponseFiltering.getCorrectPvicaOrigin(xml);
        assertEquals("ds.tv", origin);
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
        setRecordType(collectionRecord, collectionRecord.getId());

        assertEquals(collectionRecord.getRecordType(), RecordTypeDto.COLLECTION);
    }
    @Test
    public void testRecordTypeDU() {
        DsRecordDto collectionRecord = new DsRecordDto();
        collectionRecord.setId("ds.test:oai:du:232234234");
        setRecordType(collectionRecord, collectionRecord.getId());

        assertEquals(collectionRecord.getRecordType(), RecordTypeDto.DELIVERABLEUNIT);
    }
    @Test
    public void testRecordTypeMan() {
        DsRecordDto collectionRecord = new DsRecordDto();
        collectionRecord.setId("ds.test:oai:man:232234234");
        setRecordType(collectionRecord, collectionRecord.getId());

        assertEquals(collectionRecord.getRecordType(), RecordTypeDto.MANIFESTATION);
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
        OaiHarvestClient client = new OaiHarvestClient(job, "test");
        OaiRecord oaiRecord = client.extractRecordsFromXml(doc).get(0);
        String testStorageId = "ds.test:" + oaiRecord.getId();

        //Conversion from oaiRecord to dsRecord
        DsRecordDto dsRecord = new DsRecordDto();
        dsRecord.setId(testStorageId);
        dsRecord.setOrigin("ds.test");
        dsRecord.setData(oaiRecord.getMetadata());
        OaiResponseFiltering.setRecordType(dsRecord, testStorageId);

        //Tests
        assertEquals(RecordTypeDto.DELIVERABLEUNIT, dsRecord.getRecordType());
        assertEquals("ds.test", dsRecord.getOrigin());
    }
    
    


}
