package dk.kb.datahandler.oai;

import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PreservicaFilterTest {

    @Test
    public void testXmlNamespacePrefixesTvMatches(){
        Pattern tvPattern = OaiResponseFilterPreservicaSeven.TV_PATTERN;
        String[] texts = {
                "<formatMediaType>Moving Image</formatMediaType>",
                "<ns:formatMediaType>Moving Image</ns:formatMediaType>",
                "<ns:formatMediaType>Moving image</ns:formatMediaType>",
                "<namespace:formatMediaType>Moving Image</namespace:formatMediaType>",
                "<ns2:formatMediaType xmlns:ns2=\"http://www.pbcore.org/PBCore/PBCoreNamespace.html\">Moving Image</ns2:formatMediaType>"
        };

        // Test each string
        for (String text : texts) {
            Matcher matcher = tvPattern.matcher(text);
            assertTrue(matcher.find());
        }
    }

    @Test
    public void testXmlNamespacePrefixesRadioMatches(){
        Pattern radioPattern = OaiResponseFilterPreservicaSeven.RADIO_PATTERN;
        String[] texts = {
                "<formatMediaType>Sound</formatMediaType>",
                "<ns:formatMediaType>Sound</ns:formatMediaType>",
                "<namespace:formatMediaType>Sound</namespace:formatMediaType>",
                "<ns2:formatMediaType xmlns:ns2=\"http://www.pbcore.org/PBCore/PBCoreNamespace.html\">Sound</ns2:formatMediaType>"
        };


        // Test each string
        for (String text : texts) {
            Matcher matcher = radioPattern.matcher(text);
            assertTrue(matcher.find());
        }
    }

    @Test
    public void testDRFilterPositive(){
        Pattern pattern = OaiResponseFilterDrArchive.DR_PATTERN;

        String[] channels = {
                "<namespace:publisher>DR P3</namespace:publisher>",
                "<publisher>dr2</publisher>"
        };

        for (String channel : channels) {
            Matcher matcher = pattern.matcher(channel);
            assertTrue(matcher.find());
        }

    }

    @Test
    public void testDRFilterNegative(){
        Pattern pattern = OaiResponseFilterDrArchive.DR_PATTERN;

        String[] channels = {
                "<namespace:publisher>TV 2</namespace:publisher>",
                "<publisher>tvkolding</publisher>"
        };

        for (String channel : channels) {
            Matcher matcher = pattern.matcher(channel);
            assertFalse(matcher.find());
        }

    }

    @Test
    public void transcodingDone() {
        Pattern pattern = OaiResponseFilterPreservicaSeven.TRANSCODING_PATTERN;

        String[] texts = {
            "<Metadata schemaUri=\"http://kuana.kb.dk/types/radiotv_transcoding_status/0/1/#\">\n" +
                    "      <Ref>858702e2-53c3-4ec4-8d60-c8786b1a247d</Ref>\n" +
                    "      <Entity>ba464c30-6b09-429d-b733-361dce224ef9</Entity>\n" +
                    "      <Content>\n" +
                    "         <radiotvTranscodingStatus:radiotvTranscodingStatus xmlns:radiotvTranscodingStatus=\"http://kuana.kb.dk/types/radiotv_transcoding_status/0/1/#\"\n" +
                    "                                                            xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                    "            <transcodingStatus>done</transcodingStatus>\n" +
                    "         </radiotvTranscodingStatus:radiotvTranscodingStatus>\n" +
                    "      </Content>\n" +
                    "   </Metadata>\n"
        };

        for(String text : texts) {
            Matcher matcher = pattern.matcher(text);
            assertTrue(matcher.find());
        }
    }

    @Test
    public void transcodingNotDone() {
        Pattern pattern = OaiResponseFilterPreservicaSeven.TRANSCODING_PATTERN;

        String[] texts = {
                "<Metadata schemaUri=\"http://kuana.kb.dk/types/radiotv_transcoding_status/0/1/#\">\n" +
                        "      <Ref>858702e2-53c3-4ec4-8d60-c8786b1a247d</Ref>\n" +
                        "      <Entity>ba464c30-6b09-429d-b733-361dce224ef9</Entity>\n" +
                        "      <Content>\n" +
                        "         <radiotvTranscodingStatus:radiotvTranscodingStatus xmlns:radiotvTranscodingStatus=\"http://kuana.kb.dk/types/radiotv_transcoding_status/0/1/#\"\n" +
                        "                                                            xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                        "            <transcodingStatus>idle</transcodingStatus>\n" +
                        "         </radiotvTranscodingStatus:radiotvTranscodingStatus>\n" +
                        "      </Content>\n" +
                        "   </Metadata>\n"
        };

        for(String text : texts) {
            Matcher matcher = pattern.matcher(text);
            assertFalse(matcher.find());
        }
    }
}
