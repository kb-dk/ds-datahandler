package dk.kb.datahandler.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

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

        File file = new File("target/");
        String targetFolder = file.getAbsolutePath() + "/";
        String persistenceFile=targetFolder+oaiTargetFile;
      
        //Delete old files if one exist from previous unittest
        Files.deleteIfExists(Paths.get(persistenceFile));
        
        
        // Test file does not exist
        String last = HarvestTimeUtil.loadLastHarvestTime( persistenceFile);        
        assertEquals(HarvestTimeUtil.defaultStartDate,last);
        
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
    void testGetNextDay() throws Exception {    	
    	    	
    	assertEquals("2020-01-02",HarvestTimeUtil.getNextDayIfNot2DaysInFuture("2020-01-01")); //1 day
    	assertEquals("2021-01-01",HarvestTimeUtil.getNextDayIfNot2DaysInFuture("2020-12-31")); //new year
    
        //Test 2 days in future
    	//Construct today in format yyyy-MM-dd
    	Calendar cal = Calendar.getInstance(TimeZone.getDefault(),Locale.getDefault());
    	String today = HarvestTimeUtil.formatDate2Day(cal.getTime());
    	
    	//Next day must be returned
    	String tomorrow=HarvestTimeUtil.getNextDayIfNot2DaysInFuture(today);    	
    	assertNotNull(tomorrow);    
    	
    	//This is 2 days in future.
    	String todayPlus2Days = HarvestTimeUtil.getNextDayIfNot2DaysInFuture(tomorrow);
    	assertNull(todayPlus2Days); //2 days in future     	    
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
    
}
