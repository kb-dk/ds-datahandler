package dk.kb.datahandler.oai;

import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class PreservicaFilterTest {

    @Test
    public void testXmlNamespacePrefixesTvMatches(){
        Pattern tvPattern = OaiResponseFilterPreservicaSeven.TV_PATTERN;
        String[] texts = {
                "<formatMediaType>Moving Image</formatMediaType>",
                "<ns:formatMediaType>Moving Image</ns:formatMediaType>",
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
}
