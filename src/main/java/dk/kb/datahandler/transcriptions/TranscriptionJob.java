package dk.kb.datahandler.transcriptions;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.cxf.helpers.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.util.webservice.exception.InvalidArgumentServiceException;

public class TranscriptionJob {
    private static final Logger log = LoggerFactory.getLogger(TranscriptionJob.class);
    
    public static void main(String[] args) {
        String dropFolder="/home/teg/transcriptions_drop_folder";
        String completedFolder="/home/teg/transcriptions_completed_folder";
        processTranscriptions(dropFolder,completedFolder);
        
        
        
    }
    
    
    public synchronized static int processTranscriptions(String dropFolderFilePath,String completedFolderFilePath){     
        validateFoldersExist(dropFolderFilePath, completedFolderFilePath);
        log.debug("Starting transcriptionjob with dropFolder='{}' and completedFolder='{}'",dropFolderFilePath,completedFolderFilePath);                        
                    
        File dropFolderDir = new File(dropFolderFilePath);                               
        File[] files = dropFolderDir.listFiles();
        log.info("Transcription job started, #files in dropFolder:"+files.length);
        
        for (File file : files) {
            process(file);
            
        }

                        
        return 0;                
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
     * Process the file. Will return true if success.
     * Will fail if json can not be parsed or file does not have .json extension.
     * Completed files will be moved to the completed folder with .failed or .completed added to filename
     * 
     */
    private static boolean process(File file) {               
        return true;
    }
    
}
