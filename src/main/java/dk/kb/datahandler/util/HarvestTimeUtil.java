package dk.kb.datahandler.util;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.webservice.exception.InvalidArgumentServiceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.datahandler.config.ServiceConfig;
import dk.kb.datahandler.model.v1.OaiTargetDto;
import dk.kb.datahandler.model.v1.OaiTargetDto.DateStampFormatEnum;

/*
 * All access to this class is syncronized since we are using filesystem as persistence.
 * 
 *  HarvestTimeUtil methods are protected and called from unittest
 * 
 */

public class HarvestTimeUtil {

    public static final String DEFAULT_START_DATE="1900-01-01T00:00:00Z"; //Used as start if no file has been written yet for that target
    private static final Logger log = LoggerFactory.getLogger(HarvestTimeUtil.class);
    private static final Charset UTF8= StandardCharsets.UTF_8;         
    private static final String DAY_PATTERN = "yyyy-MM-dd";


    public static synchronized String loadLastHarvestTime(OaiTargetDto oaiTarget) throws Exception{            
        String oaiTargetNameFile = getFileNameFromOaiTarget(oaiTarget);        
        return loadLastHarvestTime(oaiTargetNameFile);        
    }


    public static synchronized void updateDatestampForOaiTarget(OaiTargetDto oaiTarget, String datestamp) {

        //Hack to fix datestamp returned from Preservica. Format returned are not in OAI standard and is parsed wrong (downgrade to seconds) when given to preservica.
        //Only preservica 6 does not, preservica 5 gives correct format.
        if (datestamp.length() >20) { // 2021-03-24T19:57:34.123Z -> 2021-03-24T19:57:34Z 
            datestamp=datestamp.substring(0,19)+"Z";
        }
        else if(datestamp.length()==10) {             
            datestamp=datestamp+"T00:00:00Z";
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
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DAY_PATTERN,Locale.ROOT);
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


     

    protected static String loadLastHarvestTime(String oaiTargetNameFile ) throws Exception{                        
        Path oaiTargetFilePath = Paths.get( oaiTargetNameFile);
        if (!Files.exists(oaiTargetFilePath)) {            
            log.info("OAI target: "+oaiTargetNameFile +" does not have a last harvest time. Using default:"+DEFAULT_START_DATE);
            return DEFAULT_START_DATE;
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

    protected static void updateDatestampForOaiTarget(String oaiTargetNameFile, String datestamp) {
        try {
            deleteFileAndWriteToFile(oaiTargetNameFile, datestamp);
        } catch (IOException e) {
            log.warn("Datahandler was unable to update datestamp '{}' for OAI target in file: '{}'.",
                    datestamp, oaiTargetNameFile);
            throw new InternalServiceException(e);
        }
        log.info("Updated last harvest datestamp for oai target: '{}' with datestamp: '{}'", oaiTargetNameFile, datestamp);
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
     * <p/>
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

    /**
     * Some OAI servers only support day yyyy-MM-dd format and UTC format will be reduced to day 
     * 
     * @param date in UTC format
     * @param oaiTarget The oai target that has the datestamp format
     * @return date in UTC format or date in day format
     */
    private static String formatDateForOaiTarget(String date, OaiTargetDto oaiTarget) {
        if (oaiTarget.getDateStampFormat().equals(DateStampFormatEnum.DATETIME)) {
            if(date.length()==10) {
                date=date+"T00:00:00Z";// expand if not already date
            }
            return date;
        }
        else {
            return date.substring(0,10); //reduce
        }                
    }

    /**
     * Some OAI targets needs to be split into days instead of a full date interval. 
     * Will format from and until date to a format supported by the OAI target<br> 
     * 
     * @param oaiTarget The OAI target to generate intervals for. The interval timeformat will match what the oai target has defined. 
     * @param from  Generate day intervals starting from this day (included). If null it will start from the start_day attribute defined for the oai target.
     * 
     */
    public static String generateFrom( OaiTargetDto oaiTarget , String from){

        if(from != null) {
            if (!HarvestTimeUtil.validateOaiDateFormat(from)) {
                log.warn("From datestamp not in UTC format:"+from); //Should not happen as this is called internally only
                throw new InvalidArgumentServiceException("From datestamp not in UTC format:"+from);
            }
        }        
        else { //From is null and set default        
            //From date          
           from=HarvestTimeUtil.DEFAULT_START_DATE; //year 1900. 
                  
        }

        String fromFormatted = formatDateForOaiTarget(from, oaiTarget);                         
        return fromFormatted;

    }


    

}
