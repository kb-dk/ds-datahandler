package dk.kb.datahandler.pvica;

import static org.junit.jupiter.api.Assertions.*;


import org.junit.jupiter.api.Test;

import dk.kb.datahandler.oai.OaiHarvestClient;
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
    public void testRemovalOfCollections() throws IOException {
        String xipCollection = Resolver.resolveUTF8String("xml/testCollection.xml");

        boolean answer = xipCollection.contains("<xip:Collection");
        assertTrue(answer);
    }




}
