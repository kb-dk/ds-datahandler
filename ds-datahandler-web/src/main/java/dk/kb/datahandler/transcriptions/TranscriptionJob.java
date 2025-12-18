package dk.kb.datahandler.transcriptions;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.datahandler.config.ServiceConfig;
import dk.kb.storage.model.v1.TranscriptionDto;
import dk.kb.storage.util.DsStorageClient;
import dk.kb.util.webservice.exception.InvalidArgumentServiceException;

public class TranscriptionJob {
    private static final Logger log = LoggerFactory.getLogger(TranscriptionJob.class);
       
    public synchronized static int processTranscriptions(String dropFolderFilePath,String completedFolderFilePath) throws Exception{     
        validateFoldersExist(dropFolderFilePath, completedFolderFilePath);
        log.debug("Starting transcriptionjob with dropFolder='{}' and completedFolder='{}'",dropFolderFilePath,completedFolderFilePath);                        
       
        int parsedSucces=0;
        File dropFolderDir = new File(dropFolderFilePath);       
        File completedFolderDir = new File(completedFolderFilePath);
        
        File[] files = dropFolderDir.listFiles();
        log.info("Transcription job started, #files in dropFolder:"+files.length);
        
        for (File file : files) {
            boolean validExtension=validateFileJson(file);                        
            if (!validExtension) {
                log.warn("File in drop folder is not JSON extension:"+file.getAbsolutePath());
                moveFileToCompletedFolder(file, validExtension,completedFolderDir);    
                continue; //Skip processing            
            }
            
            boolean success=process(file);
            moveFileToCompletedFolder(file, success,completedFolderDir);
            parsedSucces++;
            log.info("Completed indexing transcription:"+file.getName());
        }

                        
        return parsedSucces;                
    }
    
    /*
     * But folders must exist.
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
    
    /*
     * File must already been validated to be json-file before calling this method
     * Process the file and send the data to ds-storage. 
     * 
     * Will return true if success.
     */
    private static boolean process(File file) {         
        try {
           TranscriptionDto transcription = TranscriptionIndexer.parseFile(file.getAbsolutePath());           
           DsStorageClient storageClient = new DsStorageClient(ServiceConfig.getDsStorageUrl());
           storageClient.createOrUpdateTranscription(transcription);                     
        }
        catch(Exception e) {
           log.error("Error processing transcription:"+file , e); 
           return false;
        }        
        return true;
    }
    
    
    /*
     * Validate file has json extension
     */
    private static boolean validateFileJson(File file) {
        String extension=FilenameUtils.getExtension(file.getName());
     
        if (extension== null || !"json".equals(extension)) {
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
        Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);                    
    }
    
}
