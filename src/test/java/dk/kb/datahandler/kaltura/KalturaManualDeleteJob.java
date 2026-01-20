package dk.kb.datahandler.kaltura;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import dk.kb.kaltura.client.DsKalturaClient;

public class KalturaManualDeleteJob {

    /**
     * <p>
     * Manual started job to that will delete streams+metadata on Kaltura for an
     * input list of Kaltura entryId's.
     * </p>
     * 
     * This job is intended to be run in the 'great takedown' before going live.
     * Input is a text file where each line is a Kaltura entryIds 
     * Output is a text file of entryIds that failed deletion.
     * If delete is most likely because the entryId was not found in Kaltura.
     * 
     * Notice. Out of 300K deletions about 20 failed and had to be tried again
     * 
     */
    public static void main(String[] args) {
        
        String kalturaUrl = "https://kmc.kaltura.nordu.net";
        String adminSecret = "";// Use token,tokenId  instead
        Integer partnerId = 397; // 398=stage, 397=prod. 
        String userId = "teg@kb.dk"; //User must exist in kaltura.                 
        String token = "abc"; // <- replace with correct token matching tokenId
        String tokenId = "xxxxx";
        int conversionQueueThreshold = 50;
        int conversionQueueDelaySeconds = 30;

        String input_entry_ids = "/home/teg/delete_kaltura/delete_kaltura_entry_id.csv"; // File with entryIds til be delete. One oneeach line
        String output_entry_ids = "/home/teg/delete_kaltura/delete_kaltura_entry_id_failed.csv"; // EntryIds that failed during deletion will be added in this file.
        try {
            createNewFileIfNotExists(output_entry_ids); // Will create new if not exists;

            DsKalturaClient client = new DsKalturaClient(kalturaUrl, userId, partnerId, token, tokenId, adminSecret,
                    86400, 3600, conversionQueueThreshold, conversionQueueDelaySeconds);
            int numberDeleteFailed = 0;
            int numberDeleteSuccess = 0;
            List<String> entryIds = readAllLines(input_entry_ids);
            System.out.println("Loaded file with Kaltura entryIds. Number of entries: " + entryIds.size());
            for (String entryId : entryIds) {
                try { // We need to continue with the rest if one fails.
                    boolean success = client.deleteStreamByEntryId(entryId);
                    System.out.println("success: " + success + " deleted entry_id: " + entryId);
                    if (success) {
                        numberDeleteSuccess++;
                    } else {
                        numberDeleteFailed++;
                        System.out.println("Failed deleting entryId: " + entryId);
                        addLineToFile(output_entry_ids, entryId);
                    }

                } catch (Throwable e) {
                    e.printStackTrace();
                    System.out.println("API error for entryId: " + entryId);
                    numberDeleteFailed++;
                }
            }
            System.out.println("Delete job completed, results:");
            System.out.println("Number delete success: " + numberDeleteSuccess);
            System.out.println("Number delete failed: " + numberDeleteFailed);
            if (numberDeleteFailed > 0) {
                System.out.println("See the output file for entries that failed: " + output_entry_ids);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /*
     * Read all lines and save in a list. Yes, everything will be in memory but this
     * is not an issue for a few million strings.
     */
    private static List<String> readAllLines(String file) throws IOException {
        List<String> allLines = Files.readAllLines(Paths.get(file));
        return allLines;
    }

    /*
     * Add a new line to a file. The file must already exist.
     */
    private static void addLineToFile(String fileName, String line) throws IOException {
        line = line + "\n"; // new line
        Files.write(Paths.get(fileName), line.getBytes(Charset.forName("UTF-8")), StandardOpenOption.APPEND);
    }

    /*
     * Create a new file if it does not exist
     */
    private static void createNewFileIfNotExists(String fileName) throws IOException {
        File file = new File(fileName);
        if (!file.exists()) {
            file.createNewFile();
            System.out.println("Created new empty file: " + fileName);
        } else {
            System.out.println("File already exists: " + fileName);
        }
    }
}
