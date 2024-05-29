package dk.kb.datahandler.kaltura;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.impl.Http2SolrClient;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import com.kaltura.client.enums.KalturaMediaType;



public class KalturaUploadFolderIntegrationTest {


    /**
     * Takes a directory with video or audio files and uploads them to Kaltura.<br>
     * The upload will use file_id as eksternal kaltura id.<br>
     * Title and description will be uploaded also to Kaltura title and description field. 
     *<p/>
     * Before running this method, change log-level to warn i logback.test.xml to avoid spamming.
     * <p/>
     * This can not be changed to a unittest since it will modify Kaltura
     */
    public static void main(String[] args)  {

        String kalturaUrl= "https://kmc.kaltura.nordu.net";        
        String adminSecret = "XXXXXXXXXXXXXXXXXXXXXXxxx"; 
        Integer partnerId = 380; // Use this partner ID for DS project 
        String userId = "XXX@kb.dk"; //User must exist in kaltura.                 

        try {
            KalturaUploadClient client = new KalturaUploadClient(kalturaUrl,userId,partnerId,adminSecret);

           // String uploadFolder="/home/teg/kaltura_files/video/";
            //KalturaMediaType mediaType = KalturaMediaType.VIDEO;

            String uploadFolder="/home/teg/kaltura_files/audio/";
            KalturaMediaType mediaType = KalturaMediaType.AUDIO;


            List<String> fileNameList = getFilesInDirectory(uploadFolder);

            
            for (String file : fileNameList) {
                SolrDocument doc= getRecordByFileId(file.trim());
                ArrayList<String> titles = (ArrayList<String>)  doc.getFieldValue("title");
                String title=titles.get(0);//take first title. We still need a better title field in solr.
                String description = (String) doc.getFieldValue("description");             
                String refId=file;// This is our external id 
                System.out.println("Uploaded file:"+file +" with title:"+title);
                String filePath=uploadFolder+file;
        
                String referenceId=null;
                referenceId= client.uploadFile(filePath, mediaType, title,description, refId);
                
                System.out.println("Uploaded file:"+file + " got kaltura referenceId:"+referenceId);    
            }
            
            
        }
        catch(Exception e ) {
            e.printStackTrace();
        }
    }


    private static SolrDocument getRecordByFileId(String fileId) throws Exception{



        String solrUrl= "http://devel11:10007/solr/ds";
        Http2SolrClient client = new Http2SolrClient.Builder(solrUrl).build();

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery("file_id:\""+fileId+"\"");
        solrQuery.set("facet", "false");


        QueryResponse rsp = client.query(solrQuery, METHOD.POST); //do not cache        
        SolrDocumentList results = rsp.getResults();
        if (results.size() != 1) {
            System.out.println("No solr document found for file_id:"+fileId);
        }
        return results.get(0);
    }


    private static List<String> getFilesInDirectory(String dir) {
        return Stream.of(new File(dir).listFiles())  
                .map(File::getName)
                .collect(Collectors.toList());
    }

}
