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
       
    /**
     * Parse the 3 transcription data files together and create the transcriptionDto.<br>
     * 
     * Overview of how the 3 files are mapped to the transcriptionDto<br><br>
     *
     * <table border=1>
     *  <th>Transcription file type</th><th>json field</th><th>TranscriptionDTO</th>
     *  <tr><td>ner.json</td><td>file_id</td><td>fileId</td></tr>
     *  <tr><td>ner.json</td><td>transcription</td><td>transcription</td></tr>
     *  <tr><td>segments.fw.json</td><td>start<br>end<br> text<br></td><td>transcriptionLines<br> (start - end text)</td></tr>
     *  <tr><td>(none)</td><td>(data not files)</td><td>mTime (use time now)</td></tr>
     *  <tr><td>info.fw.json</td><td>source_basename</td><td>fileName</td></tr>
     * </table>
     * 
     *
     * @param transcriptionFile - the file with suffix ner.json
     * @param segmentsFile - the file with suffix suffix ner.json
     * @param infoFile - the file with suffix info.fw.json
     * 
     * @return The transcriptionDto with the combined transcription data from the 3 files. Will throw exception if parsing fail.
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

    /** 
     * Extract the start,end,text from each json object in the json array and 
     * concatenate them as <start> - <end> <text> <newline> for each object.
     * 
     * @param segmentsJsonObject JsonArray object from the segments.fw.json file.
     */
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
