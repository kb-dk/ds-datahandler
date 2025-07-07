package dk.kb.datahandler.util;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import dk.kb.util.webservice.exception.InvalidArgumentServiceException;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.datahandler.model.v1.OaiTargetDto;
import dk.kb.datahandler.model.v1.OaiTargetDto.DateStampFormatEnum;

import static org.junit.jupiter.api.Assertions.*;

public class HarvestTimeUtilTest {

    private static final Logger log = LoggerFactory.getLogger(HarvestTimeUtilTest.class);
    private static final String DAY_PATTERN = "yyyy-MM-dd";
    
    @Test
    void filePersistenceTest() throws Exception {

        try {
         
        String oaiTargetFile="testOai.txt";
        // Write the files to target folder.

        File file = new File("target/");
        String targetFolder = file.getAbsolutePath() + "/";
        String persistenceFile=targetFolder+oaiTargetFile;
      
        //Delete old files if one exist from previous unittest
        Files.deleteIfExists(Paths.get(persistenceFile));
        
        
        // Test file does not exist
        String last = HarvestTimeUtil.loadLastHarvestTime( persistenceFile);        
        assertEquals(HarvestTimeUtil.DEFAULT_START_DATE,last);
        
        //Test create new file
        HarvestTimeUtil.deleteFileAndWriteToFile(persistenceFile, "2020-01-02T12:34:59Z");
        last = HarvestTimeUtil.loadLastHarvestTime( persistenceFile);                       
        assertEquals("2020-01-02T12:34:59Z",last);
        
        //Write again when file already is created. This time it is deleted before recreated
        HarvestTimeUtil.deleteFileAndWriteToFile(persistenceFile, "2020-01-03T00:00:00Z");
        last = HarvestTimeUtil.loadLastHarvestTime( persistenceFile);
        assertEquals( "2020-01-03T00:00:00Z",last);
        }
        catch(Exception e) {
            e.printStackTrace();
            fail("Error with oai targets file persistence",e);
            
        }
    }
    

    @Test
    void testValidateDayFormat() throws Exception {
    	assertTrue(HarvestTimeUtil.validateDayFormat("2024-01-01"));
    	assertTrue(HarvestTimeUtil.validateDayFormat("2024-12-12"));
    	assertFalse(HarvestTimeUtil.validateDayFormat("2024-02-31")); //31. feb does not exist
    	
    }

      
    
    @Test
    void testUtcSecondsFormat() {                  
        //All valid
        assertTrue(HarvestTimeUtil.validateOaiDateFormat("2020-01-01T00:00:00Z"));
        assertTrue(HarvestTimeUtil.validateOaiDateFormat("2021-03-24T19:57:34Z"));
                
        //Not valid
        assertFalse(HarvestTimeUtil.validateOaiDateFormat("2021-03-32T19:57:34Z")); //day 32 does not exist
        assertFalse(HarvestTimeUtil.validateOaiDateFormat("2021-03-24"));
        assertFalse(HarvestTimeUtil.validateOaiDateFormat("2021-03-24T19:57:34.00.00Z"));
        assertFalse(HarvestTimeUtil.validateOaiDateFormat("2021-03-24T19:57:34.00.000Z"));         
    }

    @Test
    void testParseModifiedTimeFromToInstantWithDateTime () {
        String datetime = "2020-01-01T00:00:00Z";

        assertEquals(Instant.parse(datetime), HarvestTimeUtil.parseModifiedTimeFromToInstant(datetime));
    }

    @Test
    void testParseModifiedTimeFromToInstantWithDate () {
        String datetime = "2020-01-01T00:00:00Z";
        String date = "2020-01-01";

        assertEquals(Instant.parse(datetime), HarvestTimeUtil.parseModifiedTimeFromToInstant(date));
    }

    @Test
    void testParseModifiedTimeFromToInstantWithWrongDatetime () {
        String datetime = "2020-01-01 00:00:00Z";

        assertThrows(InvalidArgumentServiceException.class, () -> HarvestTimeUtil.parseModifiedTimeFromToInstant(datetime));
    }
    
    /**
     * 
     * From a java date format yyyy-MM-dd
     * 
     * @param date  Java date object
     */
    public static String formatDate2Day(Date date) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DAY_PATTERN,Locale.ROOT);            
        String day = simpleDateFormat.format(date);               
        return day;        
    }
    
}
