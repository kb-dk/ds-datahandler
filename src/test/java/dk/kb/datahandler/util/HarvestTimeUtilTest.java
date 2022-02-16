package dk.kb.datahandler.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HarvestTimeUtilTest {

    private static final Logger log = LoggerFactory.getLogger(HarvestTimeUtilTest.class);

    @Test
    void filePersistenceTest() throws Exception {

        try {
         
        String oaiTargetFile="testOai.txt";
        // Write the files to target folder.
        String path = "target/";
        File file = new File(path);
        String targetFolder = file.getAbsolutePath() + "/";
        String persistenceFile=targetFolder+oaiTargetFile;
      
        //Delete old files if one exist from previous unittest
        Files.deleteIfExists(Paths.get(persistenceFile));
        
        
        // Test file does not exist
        String last = HarvestTimeUtil.loadLastHarvestTime( persistenceFile);
        assertEquals("1900-01-01",last);
        
        //Test create new file
        HarvestTimeUtil.deleteFileAndWriteToFile(persistenceFile, "2020-01-02");
        last = HarvestTimeUtil.loadLastHarvestTime( persistenceFile);                       
        assertEquals("2020-01-02",last);
        
        //Write again when file already is created. This time it is deleted before recreated
        HarvestTimeUtil.deleteFileAndWriteToFile(persistenceFile, "2020-01-03");
        last = HarvestTimeUtil.loadLastHarvestTime( persistenceFile);
        assertEquals("2020-01-03",last);
                
        
        }
        catch(Exception e) {
            e.printStackTrace();
            fail("Error with oai targets file persistence",e);
            
        }

    }
}
