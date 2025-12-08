package dk.kb.datahandler.kaltura;

import com.kaltura.client.enums.MediaType;
import dk.kb.kaltura.client.DsKalturaClient;
import dk.kb.kaltura.enums.FileExtension;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class UploadPreservicaStreamsFromFileIds {

    static String preservicaVideoStreamsFolder = "/home/teg/kuana-store/bart-access-copies-tv";
    static String preservicaAudioStreamsFolder = "/home/teg/kuana-store/bart-access-copies-radio";

    public static void main(String[] args) {

        String kalturaUrl = "https://kmc.kaltura.nordu.net";
        String adminSecret = "";// Use token,tokenId  instead
        Integer partnerId = 397; // 398=stage, 397=prod. 
        String userId = "teg@kb.dk"; //User must exist in kaltura.                 
        String token = "abc"; // <- replace with correct token matching tokenId
        String tokenId = "0_f2qyxk5i";

        //These must be originates_from:Preservica since filepath depends on this
        String fileWithFileIds = "/home/teg/eclipse-workspace/ds-datahandler/preservica_missing_streams_file_id.txt";

        try {
            DsKalturaClient client = new DsKalturaClient(kalturaUrl, userId, partnerId, token, tokenId, adminSecret, 86400, 3600);

            List<String> ids = loadLines(fileWithFileIds);
            for (String fileId : ids) {

                SolrDocument doc = getRecordByFileId(fileId);
                String title = ((ArrayList<String>) doc.getFieldValue("title")).get(0);
                String description = (String) doc.getFieldValue("description");
                String type = (String) doc.getFieldValue("resource_description");
                boolean malfunction = (Boolean) doc.getFieldValue("access_malfunction");
                String path = getFilePath(fileId, type);

                if (!malfunction) {
                    System.out.println(fileId);
                    System.exit(1);
                }

                MediaType media = null;
                FileExtension fileExtension = null;

                if (type.equals("VideoObject")) {
                    media = MediaType.VIDEO;
                    fileExtension = FileExtension.MP4;
                } else {
                    media = MediaType.AUDIO;
                    fileExtension = FileExtension.MP3;
                }

                String tags = "DS-KALTURA,manual-2024-11-14"; //tags are comma seperated
                String entryId = client.uploadMedia(path, fileId, media, title, description, tags, fileExtension);
                System.out.println("Uploaded: " + fileId + " and got entryId: " + entryId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getFilePath(String fileId, String type) {
        String path = "";
        if (type.equals("VideoObject")) {
            path = preservicaVideoStreamsFolder + splitPreservicaFilePath(fileId);
        } else if (type.equals("AudioObject")) {
            path = preservicaAudioStreamsFolder + splitPreservicaFilePath(fileId);
        }
        return path;
    }

    private static List<String> loadLines(String file) throws IOException {
        List<String> allLines = Files.readAllLines(Paths.get(file));
        return allLines;
    }

    private static String splitPreservicaFilePath(String fileId) {
        String pathSplit = "/" + fileId.substring(0, 2) + "/" + fileId.substring(2, 4) + "/" + fileId.substring(4, 6) + "/" + fileId;
        return pathSplit;
    }

    private static SolrDocument getRecordByFileId(String fileId) throws Exception {

        String solrUrl = "http://devel11:10007/solr/ds";
        Http2SolrClient client = new Http2SolrClient.Builder(solrUrl).build();

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery("file_id:\" " + fileId + "\"");
        solrQuery.set("facet", "false");

        QueryResponse rsp = client.query(solrQuery, METHOD.POST); //do not cache        
        SolrDocumentList results = rsp.getResults();
        if (results.size() != 1) {
            System.out.println("No solr document found for file_id: " + fileId);
        }
        return results.get(0);
    }
}
