package dk.kb.datahandler.util;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import dk.kb.util.webservice.exception.InvalidArgumentServiceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.datahandler.config.ServiceConfig;
import dk.kb.datahandler.model.v1.OaiTargetDto;

/*
 * All access to this class is syncronized since we are using filesystem as persistence.
 * 
 * The file util methods are protected and called from unittest
 * 
 */

public class HarvestTimeUtil {
    
    public static final String defaultStartDate="1900-01-01T00:00:00Z"; //Used as start if no file has been written yet for that target
    private static final Logger log = LoggerFactory.getLogger(HarvestTimeUtil.class);
    private static final Charset UTF8= Charset.forName("UTF-8");            
    private static String dayPattern = "yyyy-MM-dd";
    
    
    public static synchronized String loadLastHarvestTime(OaiTargetDto oaiTarget) throws Exception{            
        String oaiTargetNameFile = getFileNameFromOaiTarget(oaiTarget);        
        return loadLastHarvestTime(oaiTargetNameFile);        
    }
    
    
    public static synchronized void updateDatestampForOaiTarget(OaiTargetDto oaiTarget, String datestamp) throws Exception{        
        
        //Hack to fix datestamp for Preservica.
        if (datestamp.length() >20) { // 2021-03-24T19:57:34.123Z -> 2021-03-24T19:57:34Z. 
            datestamp=datestamp.substring(0,20)+"Z";
        }
            
        if (!validateOaiDateFormat(datestamp)) {
            log.error("Datestamp not valid format:"+datestamp +" for Oai target:"+oaiTarget.getName());
            throw new InvalidArgumentServiceException("Datastamp not valid format:" + datestamp);
        }
        
        
        String oaiTargetNameFile = getFileNameFromOaiTarget(oaiTarget);
        updateDatestampForOaiTarget(oaiTargetNameFile, datestamp);        
    }
    
    /**
     * Validate day format is of form at yyyy-MM-dd.
     * Example: 2024-02-09
     * 
     * @param day  Validate is of correct format
     */
    public static boolean validateDayFormat(String day) {
    	SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dayPattern,Locale.getDefault());
        simpleDateFormat.setLenient(false); //day must exist
    try {    	
    	   Date date = simpleDateFormat.parse(day);    	   
    	}
    	catch(Exception e) {
    		log.warn("Parsing of day format failed:"+day);
    	    return false;
    	}
        return true;			
    	    	
    }
    
    /**
     * 
     * From a java date format yyyy-MM-dd
     * 
     * @param date  Java date object
     */
    public static String formatDate2Day(Date date) {
    	SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dayPattern,Locale.getDefault());    	    	    	
    	String day = simpleDateFormat.format(date);    	       	    
        return day;			    	    
    }
    
    
    /**
     * Return the next day. Return null if next day will be after tomorrow.  <br>
     * So if today is 2024-02-07, the last day that will be return is 2024-02-08. Else it will be null.
     * 
     * 
     * For input 2024-02-07 the output will be 2024-02-08  
     * 
     * @param day day in format yyyy-MM.dd
     */
    public static String getNextDayIfNot2DaysInFuture(String day) throws InvalidArgumentServiceException {
    	SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dayPattern,Locale.getDefault());
    	Date date = null;
    	try{
    		 date = simpleDateFormat.parse(day);
    	}
    	catch(Exception e) {
    		throw new InvalidArgumentServiceException("Not valid day:"+day);
    	}
    	
    	//Add one day
    	Calendar nextDayCal = Calendar.getInstance(TimeZone.getDefault(),Locale.getDefault());
    	nextDayCal.setTime(date ); //Set time from input
    	nextDayCal.add(Calendar.DATE, 1);
    	
    	Calendar future1DaysCal=Calendar.getInstance(TimeZone.getDefault(),Locale.getDefault());
        future1DaysCal.add(Calendar.DATE, 1); 
    	
        //more than 1 day in future. 1 day + 1 millis is enough 
        if (nextDayCal.getTimeInMillis() > future1DaysCal.getTimeInMillis()){        
        	return null;
        }
        
    	//return date format in yyyy-MM-dd
    	String nextDay=  simpleDateFormat.format(nextDayCal.getTime());
    	return nextDay;    	    	
    }
    
    protected static String loadLastHarvestTime(String oaiTargetNameFile ) throws Exception{                        
        Path oaiTargetFilePath = Paths.get( oaiTargetNameFile);
        if (!Files.exists(oaiTargetFilePath)) {            
            log.info("OAI target: "+oaiTargetNameFile +" does not have a last harvest time. Using default:"+defaultStartDate);
            return defaultStartDate;
        }
        else {                        
            String lastHarvestDate= getFirstLineFromFile(oaiTargetNameFile);
            if (!validateOaiDateFormat(lastHarvestDate)) {
                String errorMsg="Invalid saved last harvest dateformat:+"+lastHarvestDate +" for file:"+oaiTargetNameFile;
                log.error(errorMsg);
                throw new InvalidArgumentServiceException(errorMsg);
            }
            
            log.info("OAI target: "+oaiTargetNameFile +" has last harvestDate:"+lastHarvestDate);
            return lastHarvestDate;
        }               
    }
    
    protected static void updateDatestampForOaiTarget(String oaiTargetNameFile, String datestamp) throws Exception{        
        deleteFileAndWriteToFile(oaiTargetNameFile, datestamp);
        log.info("Update last harvest datestamp for oai target:"+oaiTargetNameFile+" with:"+datestamp);
    }

    protected static String getFileNameFromOaiTarget(OaiTargetDto oaiTarget) {        
        return ServiceConfig.getOaiTimestampFolder() +"/"+oaiTarget.getName()+".txt";        
    }
        
    
    protected static String getFirstLineFromFile(String fileName) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(fileName, UTF8)); 
        String line = br.readLine(); 
        br.close();
        return line;
    }
    
    
    protected static void deleteFileAndWriteToFile(String fileName, String lastHarvestDate) throws IOException {        
        Files.deleteIfExists(Paths.get(fileName));                
        FileOutputStream outputStream = new FileOutputStream(fileName);
        byte[] strToBytes = lastHarvestDate.getBytes(UTF8);
        outputStream.write(strToBytes);
        outputStream.close();                        
    }
    
    /**
     * 
     * Validate UTC timestamp, format is strict and only seconds allowed
     * 
     * example: 2021-03-24T19:57:34Z
     */
     
    public static boolean validateOaiDateFormat(String datestamp) {            

        try {
            DateTimeFormatter.ISO_DATE_TIME.parse(datestamp);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
     }

    

}
