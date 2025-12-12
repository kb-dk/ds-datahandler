package dk.kb.datahandler.transcriptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.storage.model.v1.TranscriptionDto;
import dk.kb.util.Resolver;



public class TranscriptionsTest {

    
    private static final Logger log = LoggerFactory.getLogger(TranscriptionsTest.class);

    @Test
    void parseTranscriptionJsonTest() throws Exception {
        
        String json="transcriptions/transcription-test.json";
        URL file = Resolver.resolveURL(json);
        
        TranscriptionDto trans= TranscriptionIndexer.parseFile(file.getFile());
        assertEquals("0405154f-5543-4907-bf26-996d471596cb.mp3",trans.getFileName());
        assertEquals(1761642668000L,trans.getmTime()); //Date formattet to millis                
        assertEquals("for nyt personale. Og efter en tur i stormagasinet er der mere at glæde sig til.",trans.getTranscription());                
        assertTrue(trans.getTranscriptionLines().startsWith("0.19 - 1.53  for nyt personale.\n")); //Notice double white space and new line                        
    }
    
}
