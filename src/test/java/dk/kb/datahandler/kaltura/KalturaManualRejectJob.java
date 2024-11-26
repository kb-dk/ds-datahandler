package dk.kb.datahandler.kaltura;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import dk.kb.kaltura.client.DsKalturaClient;

public class KalturaManualRejectJob {

    /**
     * <p>
     * Manual started job to that set streams to moderation status=REJECTED.
     * When a stream is rejected it can not be see from outside the KMC and can not be streamed. 
     * </p>
     *
     * <p>
     * Input is a text file where each line is a Kaltura entryIds 
     * Output is a text file of entryIds that failed reject 
     * The most likely cause of failed entryIds is that they can does not exist in the KMC.
     * </p>
     */
    public static void main(String[] args) {
        
        String kalturaUrl = "https://kmc.kaltura.nordu.net";
        String adminSecret = "";// Use token,tokenId  instead
        Integer partnerId = 398; // 398=stage, 397=prod. 
        String userId = "xxx@kb.dk"; //User must exist in kaltura.                 
        String token="abc"; // <- replace with correct token matching tokenId
        String tokenId="0_f2qyxk5i";

        String input_entry_ids = "/home/teg/kaltura_entryids_to_reject.txt"; // File with entryIds to reject. One entryId on each line
        String output_entry_ids = "/home/teg/kaltura_entryids_reject_failed.txt"; // EntryIds that failed reject will be added in this file.
        try {
            createNewFileIfNotExists(output_entry_ids); // Will create new if not exists;

            DsKalturaClient client = new DsKalturaClient(kalturaUrl, userId, partnerId, token,tokenId,adminSecret, 86400);
            int numberRejectFailed = 0;
            int numberRejectSuccess = 0;
            List<String> entryIds = readAllLines(input_entry_ids);
            System.out.println("Loaded file with Kaltura entryIds. Number of entries:" + entryIds.size());
            for (String entryId : entryIds) {
                try { // We need to continue with the rest if one fails.
                    boolean success = client.blockStreamByEntryId(entryId);
                    System.out.println("success:" + success  +" rejected entry id:"+entryId);
                    if (success) {
                        numberRejectSuccess++;
                    } else {
                        numberRejectFailed++;
                        System.out.println("Failed deleting entryId:" + entryId);
                        addLineToFile(output_entry_ids, entryId);
                    }

                } catch (Throwable e) {
                    e.printStackTrace();
                    System.out.println("API error for entryId:" + entryId);
                    numberRejectFailed++;
                }
            }
            System.out.println("Rejct job completed, results:");
            System.out.println("Number reject success=" + numberRejectSuccess);
            System.out.println("Number reject failed=" + numberRejectFailed);
            if (numberRejectFailed > 0) {
                System.out.println("See the output file for entries that failed:" + output_entry_ids);
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
            System.out.println("Created new empty file:" + fileName);
        } else {
            System.out.println("File already exists:" + fileName);
        }
    }

}
