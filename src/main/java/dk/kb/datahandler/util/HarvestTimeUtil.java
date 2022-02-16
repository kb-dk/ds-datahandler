package dk.kb.datahandler.util;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * All access to this class is syncronized since we are using filesystem as persistence
 * 
 */

public class HarvestTimeUtil {
    
    private static final String defaultStartDate="1900-01-01"; //Used as start if no file has been written yet for that target
    private static final Logger log = LoggerFactory.getLogger(HarvestTimeUtil.class);
    
    public static synchronized String loadLastHarvestTime(String oaiTargetNameFile) throws Exception{
                
                
        
        Path oaiTargetFilePath = Paths.get( oaiTargetNameFile);
        if (!Files.exists(oaiTargetFilePath)) {            
            log.info("Target:"+oaiTargetNameFile +" does not have a last harvest time. Using default:"+defaultStartDate);
            return defaultStartDate;
        }
        else {                        
            String lastHarvestDate= getFirstLineFromFile(oaiTargetNameFile);
            log.info("Target:"+oaiTargetNameFile +" has last harvestDate:"+lastHarvestDate);
            return lastHarvestDate;
        }
               
    }
    
    
    public static String getFirstLineFromFile(String fileName) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(fileName)); 
        String line = br.readLine(); 
        br.close();
        return line;
    }
    
    
    public static void deleteFileAndWriteToFile(String fileName, String lastHarvestDate) throws IOException {        
        Files.deleteIfExists(Paths.get(fileName));                
        FileOutputStream outputStream = new FileOutputStream(fileName);
        byte[] strToBytes = lastHarvestDate.getBytes();
        outputStream.write(strToBytes);
        outputStream.close();                        
    }
    
    public static boolean checkDataFormat(String date) {            
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd",Locale.getDefault());
        formatter.setLenient(false);
        try {
            Date dateParsed= formatter.parse(date);
        } catch (ParseException e) {
            return false;
        }
        return true;
    }

    

}
