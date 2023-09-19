package dk.kb.datahandler.pvica;

import static org.junit.jupiter.api.Assertions.*;


import org.junit.jupiter.api.Test;

import dk.kb.datahandler.oai.OaiHarvestClient;
import dk.kb.datahandler.oai.OaiResponseFiltering;
import dk.kb.util.Resolver;

import java.io.IOException;

public class PvicaDataTest {
    
    @Test
    public void testNameSpaceFix() throws Exception{
        String xmlFile = "xml/pvica_namespace_tofix.xml";        
        String xml = Resolver.resolveUTF8String(xmlFile);        

        String xmlFixed= OaiHarvestClient.nameFixPvica(xml);          
        assertTrue(xmlFixed.indexOf("<xip:DeliverableUnit xmlns:xip=\"http://www.tessella.com/XIP/v4\"") > 0);               
        assertTrue(xmlFixed.indexOf("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"") > 0);            
    }


    @Test
    public void testFindPvicaParent() throws Exception{
        String xmlFile = "xml/pvica_parent_test.xml";        
        String xml = Resolver.resolveUTF8String(xmlFile);        
        String parent = OaiResponseFiltering.getPvicaParent(xml);
        assertEquals("6a8d279b-0276-4856-b2ba-77b0162f5d63", parent);        
    }

    @Test
    public void testSkipPvicaRelRef() throws Exception{
        String xmlFile = "xml/pvica_skip_manifestationRelRef.xml";        
        String xml = Resolver.resolveUTF8String(xmlFile);        
        boolean skip = OaiResponseFiltering.skipManRefRefNot2(xml);
        assertEquals(true,skip);        
    }
    
    


}
