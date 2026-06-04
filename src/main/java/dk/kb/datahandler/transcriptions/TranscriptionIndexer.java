package dk.kb.datahandler.transcriptions;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import dk.kb.storage.model.v1.TranscriptionDto;


public class TranscriptionIndexer {
    
    private static final Logger log = LoggerFactory.getLogger(TranscriptionIndexer.class);  
       
 
    public static void main(String[] args) throws Exception {
        String transcriptionFile="/home/teg/transcriptions_release_v2/fffe2a89-360f-45b6-867c-984849b6b342.ner.json";
        String segmentsFile="/home/teg/transcriptions_release_v2/fffe2a89-360f-45b6-867c-984849b6b342.segments.fw.json";
        
        TranscriptionDto dto = parseFile(transcriptionFile,segmentsFile);
        System.out.println(dto);
    }

    
    /**
     * Parse a json file into a  TranscriptionDto object
     *     
     * Will throw exception if parsing fails. 
     */
    public static TranscriptionDto parseFile(String transcriptionFile, String segmentsFile) throws Exception{
    
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss",Locale.getDefault());
        
        TranscriptionDto transcription = new TranscriptionDto();
        String  transcriptionString = Files.readString(Path.of(transcriptionFile), Charset.forName("UTF-8"));
        String  segmentsFileString = Files.readString(Path.of(segmentsFile), Charset.forName("UTF-8"));
        System.out.println(transcriptionString);
        System.out.println(segmentsFileString);
        return null;
        /*
        Gson gson = new Gson();
        JsonElement element = gson.fromJson (jsonString, JsonElement.class);
        JsonObject obj = element.getAsJsonObject();
                   
        String filename= obj.get("filename").getAsString();
        String timestamp=obj.get("timestamp").getAsString();    
        JsonObject content= obj.get("content").getAsJsonObject();
        String transcriptionText=content.get("text").getAsString();
        
        transcription.setFileName(filename);
        long mtime = LocalDateTime.parse(timestamp, formatter).atOffset(ZoneOffset.UTC).toInstant().toEpochMilli();        
        transcription.setmTime(mtime);
        transcription.setTranscription(transcriptionText);
        transcription.setFileName(filename);
        
        
        String fileId=filename;
        // remove extension if it is there
        int  extensionStart=filename.indexOf(".");
        if (extensionStart > 0) {
            fileId=filename.substring(0,extensionStart);
        }
        transcription.setFileId(fileId);;
        
        StringBuilder b = new StringBuilder();
        //all lines        
        JsonArray segments = content.get("segments").getAsJsonArray(); 
        for (int i =0 ;i <segments.size() ; i++) {
             JsonObject segment = segments.get(i).getAsJsonObject();
             String start = segment.get("start").getAsString();
             String end = segment.get("end").getAsString();
             String text = segment.get("text").getAsString();            
             String transcriptionLine = start + " - "+end +" "+text;
             b.append(transcriptionLine);
             b.append("\n"); //new line between each           
        }        
        transcription.setTranscriptionLines(b.toString());        
        return transcription;
    */
    }
          
}
