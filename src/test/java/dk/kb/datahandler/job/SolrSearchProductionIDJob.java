package dk.kb.datahandler.job;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.QueryingApi.QueryResponse;

public class SolrSearchProductionIDJob {

    public static void main(String [] args) throws Exception {
        Path path = Paths.get("/home/teg/Desktop/dr_production_id.txt");
        //http://devel11:10007/solr/#/ds/
        //http://localhost:50006/solr/ds.1.prod/"
        String prodUrl="http://localhost:50006/solr/ds.1.prod/";
        String develUrl="http://devel11:10007/solr/ds/";
        
        HttpSolrClient client = new HttpSolrClient.
                Builder(develUrl).
                    withConnectionTimeout(1000).
                    withSocketTimeout(60 * 1000).
                    build();
            SolrQuery query = new SolrQuery("*:*");
            
            org.apache.solr.client.solrj.response.QueryResponse res = client.query(query, METHOD.POST);
            System.out.println("#docs:"+res.getResults().getNumFound());
            
            
        List<String> lines = Files.readAllLines(path);
        System.out.print("#production ids in file:"+lines.size());    
        int total=0;
        int found=0;
        int not_found=0;
        int totalStreams=0;
        for (String prod_id: lines) {
        
            String prod_id_formatted=reformatDrProductionId(prod_id);
             System.out.print("Calling with:"+prod_id);      
            
             query = new SolrQuery("dr_production_id:"+prod_id_formatted);            
             res = client.query(query, METHOD.POST);
             long results=res.getResults().getNumFound();
             totalStreams+=results;
                          
             total++;
             System.out.println(prod_id +" results:"+res.getResults().getNumFound());
             if (results>0) {
                 found++;
             }
             else {
                 not_found++;
             }
        }
        System.out.println("Total:"+total);
        System.out.println("Found:"+found);
        System.out.println("Total streams:"+totalStreams);
        System.out.println("Not found:"+not_found);
        
    }
    
    private static String reformatDrProductionId(String productionId) {
        while (productionId.startsWith("0")) { //remove prefix zeroes
            productionId=productionId.substring(1);
        }
        //add another zero
        productionId +="0";
        
        return productionId;
    }
    
}
