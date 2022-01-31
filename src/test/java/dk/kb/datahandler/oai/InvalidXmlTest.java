package dk.kb.datahandler.oai;



import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;


import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import dk.kb.util.Resolver;
import dk.kb.util.xml.XMLEscapeSanitiser;
public class InvalidXmlTest {

    private static final Logger log = LoggerFactory.getLogger(InvalidXmlTest.class);

    @Test
    void simpleInvalidXmlTest() throws Exception {

        //Loads a miminal xml with 1 invalid xml encoding character (decimal encoding)
        //This is already test in the KB-util, but seems important enough for a test to see you use it correct
        
        String xmlRaw= Resolver.resolveUTF8String("xml/simpleInvalidXml.xml");

        //Test it fails if not sanitised         
        try {
            parseXmlAndGetTextTagContent(xmlRaw);
            fail();
        }
        catch(Exception e)           
        {            
        }

        XMLEscapeSanitiser sanitiser = new XMLEscapeSanitiser("");
        String xmlSanitized  =  sanitiser.apply(xmlRaw);

        String text=null;
        try {
          text= parseXmlAndGetTextTagContent(xmlSanitized);
        }
        catch(Exception e)           
        {
            fail();
        }                 
        assertEquals("test!", text.trim()); //invalid encoding removed
    }

    
    
    
    /*
     * Parse the XML as UTF-8 (in header of XML samples).
     * Will return the content of the text-tag within the metadata tag.
     * Throw Exception if XML parse fails 
     */
    public String parseXmlAndGetTextTagContent(String xmlString)  throws Exception{        
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = null;
        try {
            document = builder.parse(new InputSource(new StringReader(xmlString)));
            document.getDocumentElement().normalize();
        }
        catch(Exception e) {                                                               
            throw new Exception(e); 
        }        

        //Get metadata, then text-tag value
        Element metadataElement=  (Element) document.getElementsByTagName("metadata").item(0);                            
        Element textElement=  (Element) metadataElement.getElementsByTagName("text").item(0);
        return textElement.getTextContent();
    }
  

}
