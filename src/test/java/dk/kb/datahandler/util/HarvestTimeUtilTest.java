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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.datahandler.model.v1.OaiTargetDto;
import dk.kb.datahandler.model.v1.OaiTargetDto.DateStampFormatEnum;
import dk.kb.datahandler.oai.OaiFromUntilInterval;
import dk.kb.util.webservice.exception.InvalidArgumentServiceException;

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
    void testGetNextDay() throws Exception {    	
    	    	
    	assertEquals("2020-01-02",HarvestTimeUtil.getNextDayIfNot2DaysInFuture("2020-01-01")); //1 day
    	assertEquals("2021-01-01",HarvestTimeUtil.getNextDayIfNot2DaysInFuture("2020-12-31")); //new year        
        assertEquals("2022-02-01",HarvestTimeUtil.getNextDayIfNot2DaysInFuture("2022-01-31")); //new year 
      //assertEquals("2022-11-01",HarvestTimeUtil.getNextDayIfNot2DaysInFuture("2022-10-30")); //new year

        
//      assertEquals("1942-11-03",HarvestTimeUtil.getNextDayIfNot2DaysInFuture("1942-11-02")); //new year
        
        
        
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
    void testGenerateDayIntervals() throws Exception {    
        OaiTargetDto target = getDayOnlyTarget();
        ArrayList<OaiFromUntilInterval> intervals= HarvestTimeUtil.generateFromUntilIntervalForFullIngest(target);
        System.out.println(intervals);
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
    void testOaiFromUntilInterval() {
       //Date formats must have same format and be yyyy-DD-mm or yyyy-MM-ddTmm:hh:ssZ. Until can be null  
        
       //All valid
       assertTrue(oaiFromUntilIntervalValid("2020-01-01T00:00:00Z", "2020-02-01T00:00:00Z"));                                      
       assertTrue(oaiFromUntilIntervalValid("2020-01-01T00:00:00Z", null));  
       assertTrue(oaiFromUntilIntervalValid("2020-01-01", "2020-02-01"));                        
       assertTrue(oaiFromUntilIntervalValid("2020-01-01", null));                    
                       
        //None valid
       assertFalse(oaiFromUntilIntervalValid("2020-01-01T00:00:00.00Z", null)); //millis not allowed
       assertFalse(oaiFromUntilIntervalValid("2020-01-01T", null));
       assertFalse(oaiFromUntilIntervalValid(null,"2020-01-01T00:00:00Z"));
       assertFalse(oaiFromUntilIntervalValid("2020-01-01","2020-01-01T00:00:00Z"));//Mixing formats
       assertFalse(oaiFromUntilIntervalValid(null,null));
        
    }
    
    private boolean oaiFromUntilIntervalValid(String from, String until) {
       try {
         new OaiFromUntilInterval(from,until);
       return true;
     }
     catch(Exception e) {
         return false;
     }
    }
    
    
    private OaiTargetDto getDayOnlyTarget()  {
        
        OaiTargetDto target= new OaiTargetDto();
        target.setDayOnly(true);
        target.setName("test day only target");
        target.setDateStampFormat(DateStampFormatEnum.DATE);
        target.setStartDay("2022-01-15");
        return target;
        
        
        
    }
    
    
}
