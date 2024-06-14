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
        };

        // Test each string
        for (String text : texts) {
            Matcher matcher = tvPattern.matcher(text);
            assertTrue(matcher.matches());
        }
    }

    @Test
    public void testXmlNamespacePrefixesRadioMatches(){
        Pattern radioPattern = OaiResponseFilterPreservicaSeven.RADIO_PATTERN;
        String[] texts = {
                "<formatMediaType>Sound</formatMediaType>",
                "<ns:formatMediaType>Sound</ns:formatMediaType>",
                "<namespace:formatMediaType>Sound</namespace:formatMediaType>",
        };

        // Test each string
        for (String text : texts) {
            Matcher matcher = radioPattern.matcher(text);
            assertTrue(matcher.matches());
        }
    }
}
