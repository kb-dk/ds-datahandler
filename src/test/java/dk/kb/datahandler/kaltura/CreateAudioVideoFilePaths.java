package dk.kb.datahandler.kaltura;

import java.util.ArrayList;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;


//stage7: develro@pvica...
//prod: develro@sala...

public class CreateAudioVideoFilePaths {

    final static String VIDEO="VideoObject";
    final static String AUDIO="AudioObject";
    final static String VIDEOPATH="";
    final static String AUDIOPATH="";
    
    public static void main(String[] args) throws Exception{
   
        String type=VIDEO;
        
        
        
         ArrayList<SolrDocument> docs = getRecordByFileId(type);
         for (SolrDocument doc : docs) {
         
             String fileId=(String) doc.getFieldValue("file_id");
           //  String pathSplit= "/"+fileId.substring(0,2)+"/"+fileId.substring(2,4)+"/"+fileId.substring(4,6)+"/"+fileId;
             
          
             if (type.equals(VIDEO)){
                 
                 System.out.println(fileId);
                 
             }
             else {
                 System.out.println(fileId);
             }
         }
         
         

    }
    
    /**
     * 
     * @param resourceDescription VideoObject or Audiobject
     */
    private static ArrayList<SolrDocument> getRecordByFileId(String resourceDescription) throws Exception{


        
        ArrayList<SolrDocument> list= new ArrayList<SolrDocument>();
        
        String solrUrl= "http://devel11:10007/solr/ds";
        Http2SolrClient client = new Http2SolrClient.Builder(solrUrl).build();

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery("resource_description:"+resourceDescription +" AND streaming_url:*");
        solrQuery.set("facet", "false");
        solrQuery.setFields("file_id");
        solrQuery.setRows(5000); //Test corpus should not have more
        QueryResponse rsp = client.query(solrQuery, METHOD.POST); //do not cache        
        SolrDocumentList results = rsp.getResults();
        if (results.size() == 0) {
            System.out.println("No solr document with streaming_url found for resourceDescription:"+resourceDescription);
        }
        System.out.println("number found:"+results.size());
        for (SolrDocument result: results) {
            list.add(result);
        }
        client.close();          
        
        return list;
    }



}
