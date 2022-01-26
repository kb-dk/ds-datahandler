package dk.kb.datahandler.oai;

import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;


public class OaiHarvestClient {

    private static final Logger log = LoggerFactory.getLogger(OaiHarvestClient.class);

    private String baseURL;
    private String set;
    private boolean completed=false;
    private String resumptionToken=null;


    public OaiHarvestClient (String baseURL,String set){
        this.baseURL=baseURL;
        this.set=set;
    }


    public OaiResponse next() throws Exception{
        OaiResponse oaiResponse = new OaiResponse();
        ArrayList<OaiRecord> oaiRecords = new  ArrayList<OaiRecord> ();
        oaiResponse.setRecords(oaiRecords);
        String uri= null;

        if (completed) {            
            //The caller should know not to ask for more since last batch had 0 entries.
            log.info("No more records to load for set:"+set);
            return new OaiResponse();
        }

        //For unknown reason cumulus/cups oai API failes if metaData+set parameter is repeated with resumptionToken! (bug)
        if (resumptionToken==null) {
            uri =baseURL+"?metadataPrefix=mods&verb=ListRecords&set="+set;                        
        }
        else {
            uri =baseURL+"?verb=ListRecords&resumptionToken="+resumptionToken;
        }      
         //uri = uri +"&from=2031-01-01"; //TODO DELETE. Just testing no records situation (error-tag)   
        //log.info("resumption token at:"+resumptionToken);
        String response=getHttpResponse(uri);
        //System.out.println(response);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        //Build Document
        Document document = builder.parse(new InputSource(new StringReader(response)));
        document.getDocumentElement().normalize();
        
        try {
        String error = document.getElementsByTagName("error").item(0).getTextContent();        
          log.info("No records returned from OAI server when harvesting set:"+set +" message:"+error);
        return oaiResponse;// will have no records
        }
        catch(Exception e) {
           //Ignore, no error tag was found 
           
        }
        
        try {
            String  resumptionToken=  document.getElementsByTagName("resumptionToken").item(0).getTextContent();
            String totalListSize =  document.getElementsByTagName("resumptionToken").item(0).getAttributes().getNamedItem("completeListSize").getNodeValue();
            oaiResponse.setTotalRecords(Long.parseLong(totalListSize));           
            this.resumptionToken = resumptionToken;          
        }
        catch(NullPointerException e) { //no more records
            this.resumptionToken=null;
            completed=true;
            log.info("No more records to load for set="+set);            
        }

        NodeList nList = document.getElementsByTagName("record");         

        for (int i =0;i<nList.getLength();i++) {                                         
            Element record =  (Element)nList.item(i);                      
            String metadata =  record.getElementsByTagName("metadata").item(0).getTextContent();            
            String identifier =  record.getElementsByTagName("identifier").item(0).getTextContent();

            OaiRecord oaiRecord = new OaiRecord();
            oaiRecord.setMetadata(metadata);
            oaiRecord.setId(identifier);
            oaiRecords.add(oaiRecord);                                  
        }

        return oaiResponse;
    }

    public static String getHttpResponse(String uri) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .build();

        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());

        return response.body();
    }
}
