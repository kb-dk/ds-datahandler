package dk.kb.datahandler.pvica;

import static org.junit.jupiter.api.Assertions.*;


import org.junit.jupiter.api.Test;

import dk.kb.datahandler.oai.OaiHarvestClient;
import dk.kb.datahandler.oai.OaiResponseFiltering;
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
    public void testFindPvicaParent() throws Exception{
        String xmlFile = "xml/pvica_parent_test.xml";        
        String xml = Resolver.resolveUTF8String(xmlFile);        
        String parent = OaiResponseFiltering.getPvicaParent(xml,"ds.radiotv");
        assertEquals("ds.radiotv:oai:du:9d9785a8-71f4-4b34-9a0e-1c99c13b001b", parent);        
    }

    @Test
    public void testManifestationNameSpaceFix() throws Exception{
        String xmlFile = "xml/pvica_manifestation_namespace_to_fix.xml";
        String xml = Resolver.resolveUTF8String(xmlFile);

        String xmlFixed= OaiHarvestClient.nameFixPvica(xml);
        assertTrue(xmlFixed.indexOf("<xip:Manifestation xmlns:xip=\"http://www.tessella.com/XIP/v4\"") > 0);
    }


    @Test
    public void testSkipPvicaRelRef() throws Exception{
        String xmlFile = "xml/pvica_skip_manifestationRelRef.xml";        
        String xml = Resolver.resolveUTF8String(xmlFile);        
        boolean skip = OaiResponseFiltering.skipManRefRefNot2(xml);
        assertEquals(true,skip);        
    }
    
    


}
