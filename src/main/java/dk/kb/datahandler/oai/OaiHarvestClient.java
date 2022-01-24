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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class OaiHarvestClient {

    private String baseURL;
    private String set;
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


        //For unknown reason cumulus oai API failes if metaData+set parameter is repeated with resumptionToken! (bug)
        if (resumptionToken==null) {
            uri =baseURL+"?metadataPrefix=mods&verb=ListRecords&set="+set;                        
        }
        else {
            uri =baseURL+"?verb=ListRecords&resumptionToken="+resumptionToken;

        }      

        String response=getHttpResponse(uri);
        //System.out.println(response);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        //Build Document
        Document document = builder.parse(new InputSource(new StringReader(response)));
        document.getDocumentElement().normalize();

        //Root
        Element root = document.getDocumentElement();

        try {
             String  resumptionToken=  document.getElementsByTagName("resumptionToken").item(0).getTextContent();
             String totalListSize =  document.getElementsByTagName("resumptionToken").item(0).getAttributes().getNamedItem("completeListSize").getNodeValue();
             oaiResponse.setTotalRecords(Long.parseLong(totalListSize));           
             this.resumptionToken = resumptionToken;          
        }
        catch(NullPointerException e) { //no more records
            this.resumptionToken=null;
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
