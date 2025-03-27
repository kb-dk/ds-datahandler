package dk.kb.datahandler.oai;

import dk.kb.datahandler.util.OaiRecordHandler;
import dk.kb.util.Resolver;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("integration")
public class OaiRecordHandlerTest {
    private static final SAXParserFactory factory = SAXParserFactory.newInstance();


    @Test
    public void testTranscodingStatus() throws IOException, SAXException, ParserConfigurationException {
        SAXParser saxParser = factory.newSAXParser();
        OaiRecordHandler handler = new OaiRecordHandler();

        saxParser.parse(Resolver.resolveStream("xml/054c55b3-ed3a-442c-99dd-1b80c0218114.xml"), handler);

        assertTrue(handler.isRecordTranscoded());
    }

    @Test
    public void testFormatEnum() throws IOException, SAXException, ParserConfigurationException {
        SAXParser saxParser = factory.newSAXParser();
        OaiRecordHandler handler = new OaiRecordHandler();

        saxParser.parse(Resolver.resolveStream("xml/054c55b3-ed3a-442c-99dd-1b80c0218114.xml"), handler);

        assertEquals(OaiRecordHandler.RecordType.TV,handler.getRecordType());
    }

    @Test
    public void testRecordHasMetadata() throws IOException, SAXException, ParserConfigurationException {
        SAXParser saxParser = factory.newSAXParser();
        OaiRecordHandler handler = new OaiRecordHandler();

        saxParser.parse(Resolver.resolveStream("xml/054c55b3-ed3a-442c-99dd-1b80c0218114.xml"), handler);

        assertTrue(handler.recordContainsMetadata());
    }

    @Test
    public void testDrChannel() throws IOException, SAXException, ParserConfigurationException {
        SAXParser saxParser = factory.newSAXParser();
        OaiRecordHandler handler = new OaiRecordHandler();

        saxParser.parse(Resolver.resolveStream("xml/054c55b3-ed3a-442c-99dd-1b80c0218114.xml"), handler);

        assertTrue(handler.isRecordDr());
    }
}
