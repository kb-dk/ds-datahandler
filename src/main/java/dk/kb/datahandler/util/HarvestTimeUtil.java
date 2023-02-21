package dk.kb.datahandler.util;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

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
    
    public static synchronized String loadLastHarvestTime(OaiTargetDto oaiTarget) throws Exception{            
        String oaiTargetNameFile = getFileNameFromOaiTarget(oaiTarget);        
        return loadLastHarvestTime(oaiTargetNameFile);        
    }
    
    
    public static synchronized void updateDatestampForOaiTarget(OaiTargetDto oaiTarget, String datestamp) throws Exception{        
        
        if (!validateDataFormat(datestamp)) {
            log.error("Datestamp not valid format:"+datestamp +" for Oai target:"+oaiTarget.getName());
            throw new InvalidArgumentServiceException("Datastamp not valid format:" + datestamp);
        }
        
        
        String oaiTargetNameFile = getFileNameFromOaiTarget(oaiTarget);
        updateDatestampForOaiTarget(oaiTargetNameFile, datestamp);        
    }
    
    
    protected static String loadLastHarvestTime(String oaiTargetNameFile ) throws Exception{                        
        Path oaiTargetFilePath = Paths.get( oaiTargetNameFile);
        if (!Files.exists(oaiTargetFilePath)) {            
            log.info("OAI target: "+oaiTargetNameFile +" does not have a last harvest time. Using default:"+defaultStartDate);
            return defaultStartDate;
        }
        else {                        
            String lastHarvestDate= getFirstLineFromFile(oaiTargetNameFile);
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
    
    /*
     * Validate UTC timestamp.
     * 
     *  example: 2021-03-24T19:57:34Z
     */
     
    public static boolean validateDataFormat(String datestamp) {            

        try {
            DateTimeFormatter.ISO_DATE_TIME.parse(datestamp);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
     }

    

}
