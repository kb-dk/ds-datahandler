package dk.kb.datahandler.oai;

import dk.kb.datahandler.util.PreservicaOaiRecordHandler;
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
        PreservicaOaiRecordHandler handler = new PreservicaOaiRecordHandler();

        saxParser.parse(Resolver.resolveStream("xml/054c55b3-ed3a-442c-99dd-1b80c0218114.xml"), handler);

        assertTrue(handler.isRecordTranscoded());
    }

    @Test
    public void testFormatEnum() throws IOException, SAXException, ParserConfigurationException {
        SAXParser saxParser = factory.newSAXParser();
        PreservicaOaiRecordHandler handler = new PreservicaOaiRecordHandler();

        saxParser.parse(Resolver.resolveStream("xml/054c55b3-ed3a-442c-99dd-1b80c0218114.xml"), handler);

        assertEquals(PreservicaOaiRecordHandler.RecordType.TV,handler.getRecordType());
    }

    @Test
    public void testRecordHasMetadata() throws IOException, SAXException, ParserConfigurationException {
        SAXParser saxParser = factory.newSAXParser();
        PreservicaOaiRecordHandler handler = new PreservicaOaiRecordHandler();

        saxParser.parse(Resolver.resolveStream("xml/054c55b3-ed3a-442c-99dd-1b80c0218114.xml"), handler);

        assertTrue(handler.recordContainsMetadata());
    }

    @Test
    public void testDrChannel() throws IOException, SAXException, ParserConfigurationException {
        SAXParser saxParser = factory.newSAXParser();
        PreservicaOaiRecordHandler handler = new PreservicaOaiRecordHandler();

        saxParser.parse(Resolver.resolveStream("xml/054c55b3-ed3a-442c-99dd-1b80c0218114.xml"), handler);

        assertTrue(handler.isRecordDr());
    }

    @Test
    public void testFileReference() throws IOException, SAXException, ParserConfigurationException {
        SAXParser saxParser = factory.newSAXParser();
        PreservicaOaiRecordHandler handler = new PreservicaOaiRecordHandler();
        saxParser.parse(Resolver.resolveStream("xml/aaa668c2-bf17-4ce7-bf24-d0ff5d29d097.xml"), handler);
        assertEquals("c8d2e73c-0943-4b0d-ab1f-186ef10d8eb4",handler.getFileReference());
        handler = new PreservicaOaiRecordHandler();
        saxParser.parse(Resolver.resolveStream("xml/08909897-cf37-4bd9-a230-1b48c87cea18.xml"), handler);
        assertEquals("08909897-cf37-4bd9-a230-1b48c87cea18",handler.getFileReference());

    }
}
