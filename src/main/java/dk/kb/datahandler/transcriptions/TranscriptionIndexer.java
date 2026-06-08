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
        String transcriptionFile="/home/teg/transcriptions_release_v1/fffe2a89-360f-45b6-867c-984849b6b342.ner.json";
        String segmentsFile="/home/teg/transcriptions_release_v1/fffe2a89-360f-45b6-867c-984849b6b342.segments.fw.json";
        String infoFile="/home/teg/transcriptions_release_v1/fffe2a89-360f-45b6-867c-984849b6b342.info.fw.json";
        TranscriptionDto dto = parseFile(transcriptionFile,segmentsFile,infoFile);
        System.out.println(dto);
    }

    
    /**
     * Parse a json file into a  TranscriptionDto object
     *     
     * Will throw exception if parsing fails. 
     */
    public static TranscriptionDto parseFile(String transcriptionFile, String segmentsFile, String infoFile) throws Exception{
            
        
        TranscriptionDto transcription = new TranscriptionDto();
        String  transcriptionFileString = Files.readString(Path.of(transcriptionFile), Charset.forName("UTF-8"));
        String  segmentsFileString = Files.readString(Path.of(segmentsFile), Charset.forName("UTF-8"));
        String  infoFileString = Files.readString(Path.of(infoFile), Charset.forName("UTF-8"));

        Gson gson = new Gson();
        JsonElement transcriptionJson = gson.fromJson (transcriptionFileString, JsonElement.class);
        JsonObject transcriptionJsonObject = transcriptionJson.getAsJsonObject();
        JsonElement segmentsJson = gson.fromJson (segmentsFileString, JsonElement.class);
        JsonArray segmentsJsonArray = segmentsJson.getAsJsonArray(); 
        JsonElement infoJson = gson.fromJson (infoFileString, JsonElement.class);
        JsonObject infoJsonObject = infoJson.getAsJsonObject();
        String fileId=transcriptionJsonObject.get("file_id").getAsString();                   
        String segmentLines=extractTranscriptionLines(segmentsJsonArray);
                     
        String transcriptionText=transcriptionJsonObject.get("transcription").getAsString();
        //This field is no longer present in any of the two transcriptions files, so just use parse time instead. It is not used by any business logic anyway.
        long mtime = System.currentTimeMillis()*1000; //mtime format in ds project is 1/1000000 precision. 
        String fileName=infoJsonObject.get("source_basename").getAsString();
        //Consider extracting the duration as well in a future version, but this require database changes. It is in the info file
        
        transcription.setFileId(fileId);
        transcription.setFileName(fileName);
        transcription.setmTime(mtime);
        transcription.setTranscription(transcriptionText);
        transcription.setTranscriptionLines(segmentLines);
        return transcription;    
    }
        
    private static String extractTranscriptionLines( JsonArray segmentsJsonObject) { 
        StringBuilder b = new StringBuilder();
        for (int i =0 ;i <segmentsJsonObject.size() ; i++) {
             JsonObject segment = segmentsJsonObject.get(i).getAsJsonObject();
             String start = segment.get("start").getAsString();
             String end = segment.get("end").getAsString();
             String text = segment.get("text").getAsString();            
             String transcriptionLine = start + " - "+end +" "+text;
             b.append(transcriptionLine);
             b.append("\n"); //new line between each           
        }        
        return b.toString();        
    }
          
}
