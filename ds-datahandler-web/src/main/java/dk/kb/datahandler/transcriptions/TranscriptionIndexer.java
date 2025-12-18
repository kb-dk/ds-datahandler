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
       
   /* Just for 
    public static void main(String[] args) throws Exception {
        String file="/home/teg/transcriptions_drop_folder/result_2025-10-28T09.11.08_0405154f-5543-4907-bf26-996d471596cb.mp3.json";
        TranscriptionDto dto = parseFileToFlatMapStructure(file);
        System.out.println(dto);
    }
    */
    
    /**
     * Parse a json file into a  TranscriptionDto object
     *     
     * Will throw exception if parsing fails. 
     */
    public static TranscriptionDto parseFile(String file) throws Exception{
    
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss",Locale.getDefault());
        
        TranscriptionDto transcription = new TranscriptionDto();
        String jsonString = Files.readString(Path.of(file), Charset.forName("UTF-8"));
    
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
    }
          
}
