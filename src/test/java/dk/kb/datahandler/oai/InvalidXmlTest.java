package dk.kb.datahandler.oai;



import static org.junit.jupiter.api.Assertions.fail;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
public class InvalidXmlTest {

    private static final Logger log = LoggerFactory.getLogger(InvalidXmlTest.class);
    
    @Test
    void invalidXmlTest() throws Exception {
    String uri="http://www5.kb.dk/cop/oai/?verb=ListRecords&resumptionToken=KB!214000!mods!0001-01-01!9999-12-31!oai:kb.dk:images:luftfo:2011:maj:luftfoto"; 
    
       
     String xmlResponse = OaiHarvestClient.getHttpResponse(uri);
     DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
     DocumentBuilder builder = factory.newDocumentBuilder();

  
     //TODO fix xmlResponse 

     
     //If the OAI does not return valid XML, then we can not parse it.
     // All records in this dokument is lost and also those after because we have no resumption token
     Document document = null;
     try {
     document = builder.parse(new InputSource(new StringReader(xmlResponse)));
     document.getDocumentElement().normalize();
     }
     catch(Exception e) {       
          
         log.error("Invalid XML from OAI harvest. ",e);            
         log.error("The invalid XML was retrived from this url:"+uri);                                           
       fail("Invalid XML");
     }
     
     
     
    }
    
    
}
