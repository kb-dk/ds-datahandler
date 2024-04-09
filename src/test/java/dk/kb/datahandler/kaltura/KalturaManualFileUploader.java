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

import com.kaltura.client.enums.MediaType;

import dk.kb.kaltura.client.DsKalturaClient;

public class KalturaManualFileUploader {

    /**
     * <p>
     * Manual started job to upload the devel or stage collection of audio/video files to Kaltura. 
     * The job takes a folder of audio or video files as input. 
     * For each file title and description are fetched from Solr and used as meta-data in Kaltura
     * The files will be uploaded with the metadata: tag=DS-KALTURA
     * </p>
     * 
     *  This is a temporary solution for devel/stage to get data into Kaltura for the frontend.
     *  
     */
    public static void main(String[] args)  {

        String kalturaUrl= "https://kmc.kaltura.nordu.net";        
        String adminSecret = "XXXXXXXXXXXXXXXXXXXXXXxxx"; 
        Integer partnerId = 380; // Use this partner ID for DS project 
        String userId = "XXX@kb.dk"; //User must exist in kaltura.                 

        try {
            DsKalturaClient client = new DsKalturaClient(kalturaUrl,userId,partnerId,adminSecret,86400);            

           // String uploadFolder="/home/teg/kaltura_files/video/";
            //KalturaMediaType mediaType = KalturaMediaType.VIDEO;

            String uploadFolder="/home/teg/kaltura_files/audio/";
            
            List<String> fileNameList = getFilesInDirectory(uploadFolder);

            
            for (String file : fileNameList) {
                SolrDocument doc= getRecordByFileId(file.trim());
                ArrayList<String> titles = (ArrayList<String>)  doc.getFieldValue("title");
                String title=titles.get(0);//take first title. We still need a better title field in solr.
                String description = (String) doc.getFieldValue("description");             
                String refId=file;// This is our external id 
                System.out.println("Uploaded file:"+file +" with title:"+title);
                String filePath=uploadFolder+file;
                String tag="DS-KALTURA"; //So we can the uploaded collection easy in Kaltura.                
                
                String referenceId=null;
                referenceId= client.uploadMedia(filePath, refId,MediaType.AUDIO, title,description,tag );
                
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
