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
    void parseTranscriptionFilesTest() throws Exception {
               
        //Load all 3 files and test mapping to TranscriptionDto
        String ner="transcriptions/ab6afdbc-baa7-4f91-80f8-00ef54b9ee7e.ner.json";
        URL nerFile = Resolver.resolveURL(ner);
        String segments="transcriptions/ab6afdbc-baa7-4f91-80f8-00ef54b9ee7e.segments.fw.json";
        URL segmentsFile = Resolver.resolveURL(segments);
        String info="transcriptions/ab6afdbc-baa7-4f91-80f8-00ef54b9ee7e.info.fw.json";
        URL infoFile = Resolver.resolveURL(info);
        TranscriptionDto trans= TranscriptionIndexer.parseFile(nerFile.getFile(),segmentsFile.getFile(),infoFile.getFile());
       
        assertEquals("ab6afdbc-baa7-4f91-80f8-00ef54b9ee7e.mp4",trans.getFileName());
        assertEquals("ab6afdbc-baa7-4f91-80f8-00ef54b9ee7e",trans.getFileId());
        assertEquals("Her er transcriptions segment1. Og her er transcriptions segment 2",trans.getTranscription());                       
        
        String transcriptionLines=trans.getTranscriptionLines();
        assertTrue(transcriptionLines.startsWith("73.55 - 115.25 Her er transcription segment1\n")); //Notice double white space and new line
   }
    
}
