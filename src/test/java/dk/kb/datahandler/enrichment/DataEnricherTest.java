package dk.kb.datahandler.enrichment;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.kb.datahandler.config.ServiceConfig;
import dk.kb.datahandler.oai.OaiRecord;
import dk.kb.util.Resolver;
import dk.kb.util.xml.XML;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

public class DataEnricherTest {

    private FragmentsClient fragmentsClientMock;

    @BeforeEach
    public void setUp() throws IOException, URISyntaxException {
        ServiceConfig.initialize("conf/ds-datahandler-behaviour.yaml","conf/ds-datahandler-local.yaml");

        fragmentsClientMock = Mockito.mock(FragmentsClient.class);

        String fragmentsString = Files.readString(Resolver.getPathFromClasspath("xml/fragments.json"));
        ObjectMapper mapper = new ObjectMapper();
        List<Fragment> fragments = mapper.readValue(fragmentsString, new TypeReference<List<Fragment>>(){});
        when(fragmentsClientMock.fetchMetadataFragments("test1")).thenReturn(fragments);

        MockedStatic<FragmentsClient> mockedStatic = Mockito.mockStatic(FragmentsClient.class);
        mockedStatic.when(FragmentsClient::getInstance).thenReturn(fragmentsClientMock);
    }

    @Test
    public void testEnrichment() throws IOException, URISyntaxException, ParserConfigurationException, TransformerException, SAXException, XPathExpressionException {
        OaiRecord oaiRecord = new OaiRecord();
        oaiRecord.setId("oai:io:test1");
        oaiRecord.setMetadata(Files.readString(Resolver.getPathFromClasspath("xml/unenriched-metadata-test1.xml")));

        int numberOfmetadatanodesBefore = countMetadataNodes(oaiRecord);
        DataEnricher.apply(oaiRecord);
        int numberOfmetadatanodesAfter = countMetadataNodes(oaiRecord);
        assertEquals(numberOfmetadatanodesBefore+1,numberOfmetadatanodesAfter);
    }

    private int countMetadataNodes(OaiRecord oaiRecord) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xpath = xPathFactory.newXPath();
        Document document = XML.fromXML(oaiRecord.getMetadata(),true);
        NodeList metadataNodes = (NodeList) xpath.evaluate("/XIP/Metadata", document, XPathConstants.NODESET);
        return metadataNodes.getLength();
    }


}