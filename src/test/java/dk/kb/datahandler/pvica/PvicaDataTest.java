package dk.kb.datahandler.pvica;

import static org.junit.jupiter.api.Assertions.*;


import org.junit.jupiter.api.Test;

import dk.kb.datahandler.oai.OaiHarvestClient;
import dk.kb.util.Resolver;

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
    public void testManifestationNameSpaceFix() throws Exception{
        String xmlFile = "xml/pvica_manifestation_namespace_to_fix.xml";
        String xml = Resolver.resolveUTF8String(xmlFile);

        String xmlFixed= OaiHarvestClient.nameFixPvica(xml);
        assertTrue(xmlFixed.indexOf("<xip:Manifestation xmlns:xip=\"http://www.tessella.com/XIP/v4\"") > 0);
    }





}
