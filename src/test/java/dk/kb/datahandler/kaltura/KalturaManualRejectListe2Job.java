package dk.kb.datahandler.kaltura;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import dk.kb.kaltura.client.DsKalturaClient;

public class KalturaManualRejectListe2Job {

    /**
     * 
     *  Change log level to INFO in lockback-test.xml to avoid http traffic spam when running this job 
     * 
     * <p>
     * Manual started job to that set streams to moderation status=REJECTED.
     * When a stream is rejected it can not be see from outside the KMC and can not be streamed. 
     *
     * 
     * 
     * </p>
     *
     * <p>
     * Input is a text file where each line is a recordId 
     * Output is a text file of recordId that failed reject 
     * 
     * The recordId will be mapped to kaltura entryId by calling solr first.
     * 
     * The most likely cause of failed reject is that the entryId can does not exist in the KMC.   
     * because many entries may already has been deleted in kaltura in delete job, so many will fail since they do not exist. 
     * 
     * </p>
     */
    public static void main(String[] args) {
        
        String kalturaUrl = "https://kmc.kaltura.nordu.net";
        String adminSecret = "";// Use token,tokenId  instead
        Integer partnerId = 397; // 398=stage, 397=prod. 
        String userId = "teg@kb.dk"; //User must exist in kaltura.                 
        String token = "abc"; // <- replace with correct token matching tokenId
        String tokenId = "0_xxx";

        String input_record_ids = "/home/teg/reject_liste2/reject_liste2.txt"; // File with entryIds to reject. One entryId on each line
        String output_record_ids = "/home/teg/reject_liste2/reject_liste2_failed.txt"; // EntryIds that failed reject will be added in this file.
        try {
            createNewFileIfNotExists(output_record_ids); // Will create new if not exists;

            List<String> recordIds = readAllLines(input_record_ids);
            System.out.println("Loaded file with recordIds. Number of entries:" + recordIds.size());
           
            
            DsKalturaClient client = new DsKalturaClient(kalturaUrl, userId, partnerId, token, tokenId, adminSecret, 86400, 3600);
            int numberRejectFailed = 0;
            int numberRejectSuccess = 0;
            
            for (String recordId : recordIds) {
                try { // We need to continue with the rest if one fails.
                    SolrDocument doc = getRecordById(recordId);
                    String entryId = (String) doc.getFieldValue("kaltura_id");
                                                                                
                    boolean success = client.blockStreamByEntryId(entryId);
                    System.out.println("success: " + success + " rejected recordId: " + recordId);
                    if (success) {
                        numberRejectSuccess++;
                    } else {
                        numberRejectFailed++;
                        System.out.println("Failed rejecting recordId: " + recordId);
                        addLineToFile(output_record_ids, entryId);
                    }

                } catch (Throwable e) {
                    e.printStackTrace();
                    System.out.println("API error for recordId: " + recordId);
                    numberRejectFailed++;
                }
            }
            System.out.println("Rejct job completed, results:");
            System.out.println("Number reject success: " + numberRejectSuccess);
            System.out.println("Number reject failed: " + numberRejectFailed);
            if (numberRejectFailed > 0) {
                System.out.println("See the output file for entries that failed:" + output_record_ids);
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
    
    private static SolrDocument getRecordById(String recordId) throws Exception {

        String solrUrl= "http://devel11:10007/solr/ds";
        Http2SolrClient client = new Http2SolrClient.Builder(solrUrl).build();

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery("id:\""+recordId+"\"");
        solrQuery.set("facet", "false");

        QueryResponse rsp = client.query(solrQuery, METHOD.POST); //do not cache        
        SolrDocumentList results = rsp.getResults();
        if (results.size() != 1) {
            System.out.println("No solr document found for file_id:"+recordId);
        }
        return results.get(0);
    }
}
