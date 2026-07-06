package dk.kb.datahandler.transcriptions;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.datahandler.config.ServiceConfig;
import dk.kb.storage.model.v1.TranscriptionDto;
import dk.kb.storage.util.DsStorageClient;
import dk.kb.util.webservice.exception.InvalidArgumentServiceException;

public class TranscriptionJob {
    private static final Logger log = LoggerFactory.getLogger(TranscriptionJob.class);
    
    /**
     * Process all files in the transcription drop folder.<br>
     * Each transcription is created from 3 different files that have same name but different suffixes.<br>
     * 
     * Example names of the 3 files: <br>  
     *  <ul>
     *   <li>ab6afdbc-baa7-4f91-80f8-00ef54b9ee7e.info.fw.json</li>
     *   <li>ab6afdbc-baa7-4f91-80f8-00ef54b9ee7e.ner.json</li>   
     *   <li>ab6afdbc-baa7-4f91-80f8-00ef54b9ee7e.segments.fw.json</li>
     *  </ul>
     * 
     * When each transcription is process the 3 files will be moved to the completed folder and added and additional suffix. <br>
     * For success the suffix '.completed' will be all (all 3 files)<br>
     * In case of parsing error or missing files the suffix '.failed' will be all (all of the 3 files in the drop folder)<br>
     * <br>
     * A successful transcription will be send to the ds-storage module.<br> 
     * The ds-storage module will persist the transcriptions and touch the relevant storage record so it is marked for delta indexing. <br>
     * The ds-present module has a config property that will fetch transcriptions from the ds-storage module at index time so they are part of the solr document sent to solr.<br>
     * <br>
     * @param dropFolderFilPath The full path to the drop folder for transcriptions.
     * @param completedFolderFilePath  The full path to the completed folder for transcriptions.
     * 
     * @returns The number of successful transcriptions created. 
     */
    public synchronized static int processTranscriptions(String dropFolderFilePath,String completedFolderFilePath) throws Exception{     
        validateFoldersExist(dropFolderFilePath, completedFolderFilePath);
        log.debug("Starting transcriptionjob with dropFolder='{}' and completedFolder='{}'",dropFolderFilePath,completedFolderFilePath);                        
       
        int parsedSucces=0;
        int parsedFailed=0;
        File dropFolderDir = new File(dropFolderFilePath);       
        File completedFolderDir = new File(completedFolderFilePath);
        
        //only list those ending with '.ner.json'
        //for also load the file matching with suffix  '.segments.fw.json' 
       
        File[] files = dropFolderDir.listFiles((d, name) -> name.endsWith(".ner.json"));
        
        log.info("Transcription job started, #files in dropFolder:"+files.length);
        
        for (File transcriptionFile : files) {                                   
             //Check segments file is also present.
            String segmentsFileName=toSegmentsFilename(transcriptionFile.getName());
            File segmentsFile=new File(dropFolderDir, segmentsFileName);
            
            String infoFileName=toInfoFilename(transcriptionFile.getName());
            File infoFile=new File(dropFolderDir, infoFileName);
                    
            boolean segmentsFileExist=segmentsFile.exists();
            boolean infoFileExist=infoFile.exists(); 
            
            //Set all 3 as failed.
            if (!segmentsFileExist && !infoFileExist) {
                log.error("Segments or info file is missing for transcription:"+transcriptionFile.getAbsolutePath());
                moveFileToCompletedFolder(transcriptionFile, false,completedFolderDir);    
                moveFileToCompletedFolder(segmentsFile, false,completedFolderDir);
                moveFileToCompletedFolder(infoFile, false,completedFolderDir);
                continue; //Skip processing            
            }
            
            boolean success=process(transcriptionFile,segmentsFile,infoFile);
            if (success) {
                parsedSucces++;                
            }
            else {
                parsedFailed++;
            }
            //Move all 3 files. Logic handles if some of them is missing
            moveFileToCompletedFolder(transcriptionFile, success,completedFolderDir);
            moveFileToCompletedFolder(segmentsFile,  success,completedFolderDir);
            moveFileToCompletedFolder(infoFile, success,completedFolderDir);           
            
            log.info("Completed indexing transcription:"+transcriptionFile.getName());
        }
        log.info("Transcription job completed. success='{}' and failed ='{}'",parsedSucces,parsedFailed );
                        
        return parsedSucces;                
    }
    
    /*
     * Both folders must exist.
     */
    private static void validateFoldersExist(String dropFolderFilePath,String completedFolderFilePath) {
        //Validate folders exists on file system already
        Path uploadFolder = Paths.get(dropFolderFilePath);
        Path completedFolder = Paths.get(completedFolderFilePath);
        if (!(Files.exists(uploadFolder)) && !Files.isDirectory(uploadFolder)) {
            throw new InvalidArgumentServiceException("Drop folder for transcriptions does not exist:"+dropFolderFilePath);                        
        }
        
        if (!(Files.exists( completedFolder)) && !Files.isDirectory(completedFolder)) {
            throw new InvalidArgumentServiceException("Completed folder transcriptions does not exist:"+completedFolderFilePath);                        
        }        
    }
    
    /**
     * File must already been validated to exist before this method is called.
     * Process the 3 transcriptions files and send the data to ds-storage. The ds_record matching the file_id will be touched 
     * so that it will be selected next time delta-index is started and the transcription will be searchable.
     * 
     * @param transcriptionFile File with suffix: .ner.json
     * @param segmentsFile File with suffix: segments.fw.json
     * @param infoFile File with suffix: info.fw.json
     * @re
     */
    private static boolean process(File transcriptionFile ,File segmentsFile, File infoFile ) {         
        try {
           TranscriptionDto transcription = TranscriptionIndexer.parseFile(transcriptionFile.getAbsolutePath(),segmentsFile.getAbsolutePath(),infoFile.getAbsolutePath());           
           DsStorageClient storageClient = new DsStorageClient(ServiceConfig.getDsStorageUrl());
           storageClient.createOrUpdateTranscription(transcription);                     
        }
        catch(Exception e) {
           log.error("Error processing transcription file:"+transcriptionFile , e); 
           return false;
        }        
        return true;
    }
    
   
    /*
    * Will move file from drop folder to completed folder.
    * Suffix 'completed' or 'failed' will be added to file as new extension depending on success
    *
    */
    private static void moveFileToCompletedFolder(File file, boolean success, File dropFolder) throws Exception {        
        String fullPath=file.getName();
        String newFileName;
        
        if (success) {
            newFileName=fullPath+".completed";
        }
        else {
            newFileName=fullPath+".failed";
        }                
        Path from= file.toPath();
        Path to =  Paths.get(dropFolder+"/"+newFileName);        
     try {
         Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
      }
     catch(Exception e) {
         log.warn("Failed moving file to completed folder, file does probably not exist:"+file.getAbsolutePath());
     }
        
    }
    
    private static String toSegmentsFilename(String filename) {
        return filename.replace(".ner.json", ".segments.fw.json");
    }
    
    private static String toInfoFilename(String filename) {
        return filename.replace(".ner.json", ".info.fw.json");
    }
    
}
