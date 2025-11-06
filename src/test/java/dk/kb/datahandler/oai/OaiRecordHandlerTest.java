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

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
public class OaiRecordHandlerTest {
    private static final SAXParserFactory factory = SAXParserFactory.newInstance();

    @Test
    public void testTranscodingStatus() throws IOException, SAXException, ParserConfigurationException {
        SAXParser saxParser = factory.newSAXParser();
        PreservicaOaiRecordHandler handler = new PreservicaOaiRecordHandler();
        saxParser.parse(Resolver.resolveStream("xml/twoprofiles.xml"), handler);
        assertEquals(PreservicaOaiRecordHandler.TranscodingStatus.SUCCESS, handler.lastTranscodingStatus);
        handler = new PreservicaOaiRecordHandler();
        saxParser.parse(Resolver.resolveStream("xml/updatedprofileswithrecord.xml"), handler);
        assertEquals(PreservicaOaiRecordHandler.TranscodingStatus.SUCCESS, handler.lastTranscodingStatus);

    }

    @Test
    public void testFormatEnum() throws IOException, SAXException, ParserConfigurationException {
        SAXParser saxParser = factory.newSAXParser();
        PreservicaOaiRecordHandler handler = new PreservicaOaiRecordHandler();

        saxParser.parse(Resolver.resolveStream("xml/054c55b3-ed3a-442c-99dd-1b80c0218114.xml"), handler);

        assertEquals(PreservicaOaiRecordHandler.RecordType.TV,handler.getRecordType());
    }

    @Test
    public void testRecordHasMetadataAndNestedRecord() throws IOException, SAXException, ParserConfigurationException {
        SAXParser saxParser = factory.newSAXParser();
        PreservicaOaiRecordHandler handler = new PreservicaOaiRecordHandler();

        saxParser.parse(Resolver.resolveStream("xml/054c55b3-ed3a-442c-99dd-1b80c0218114.xml"), handler);

        //also tests that a nested record will not break parsing.
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
    public void testFileIdPvicaWithCorrectPresenstaion() throws IOException, SAXException, ParserConfigurationException {
        SAXParser saxParser = factory.newSAXParser();
        PreservicaOaiRecordHandler handler = new PreservicaOaiRecordHandler();
        saxParser.parse(Resolver.resolveStream("xml/aaa668c2-bf17-4ce7-bf24-d0ff5d29d097.xml"), handler);
        assertEquals("c8d2e73c-0943-4b0d-ab1f-186ef10d8eb4", handler.fileId);
    }

    @Test
    public void testFileIdDomsMigWithPresenstation()  throws IOException, SAXException, ParserConfigurationException {
        SAXParser saxParser = factory.newSAXParser();
        PreservicaOaiRecordHandler handler = new PreservicaOaiRecordHandler();
        saxParser.parse(Resolver.resolveStream("xml/08909897-cf37-4bd9-a230-1b48c87cea18.xml"), handler);
        assertEquals("08909897-cf37-4bd9-a230-1b48c87cea18", handler.fileId);
    }

    @Test
    public void testFileIdMultipleProfiles() throws IOException, SAXException, ParserConfigurationException {
        SAXParser saxParser = factory.newSAXParser();
        PreservicaOaiRecordHandler handler = new PreservicaOaiRecordHandler();
        saxParser.parse(Resolver.resolveStream("xml/twoprofiles.xml"), handler);
        assertEquals("9ac17530-7a9a-4f43-bf4d-0b22459db1c5", handler.fileId);
        handler = new PreservicaOaiRecordHandler();
        saxParser.parse(Resolver.resolveStream("xml/updatedprofileswithrecord.xml"), handler);
        assertEquals("ed685674-cc4e-44e3-8556-8d83010482aa", handler.fileId);
    }
}
