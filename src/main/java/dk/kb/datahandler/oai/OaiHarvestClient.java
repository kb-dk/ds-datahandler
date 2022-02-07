package dk.kb.datahandler.oai;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.Base64;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import dk.kb.util.xml.XMLEscapeSanitiser;

import org.w3c.dom.ls.*;

public class OaiHarvestClient {

    private static final Logger log = LoggerFactory.getLogger(OaiHarvestClient.class);

    private String baseURL;
    private String set;
    private boolean completed=false;
    private String resumptionToken=null;
    private String from;
    private String metadataPrefix;
    private String user;
    private String password;

    public OaiHarvestClient (String baseURL,String set, String metadataPrefix, String from, String user, String password){
        this.baseURL=baseURL;
        this.set=set;
        this.from=from;
        this.metadataPrefix=metadataPrefix;
        this.user=user;
        this.password=password;
    }


    public OaiResponse next() throws Exception{
        OaiResponse oaiResponse = new OaiResponse();
        ArrayList<OaiRecord> oaiRecords = new  ArrayList<OaiRecord> ();
        oaiResponse.setRecords(oaiRecords);
        String uri= baseURL+"?verb=ListRecords";

        if (completed) {            
            //The caller should know not to ask for more since last batch had 0 entries.
            log.info("No more records to load for set:"+set);
            return new OaiResponse();
        }

        //For unknown reason cumulus/cups oai API failes if metaData+set parameter is repeated with resumptionToken! (bug)
        if (resumptionToken==null && set != null) { //COPS fails if set is still used with resumptiontoken
            uri += "&set="+set;                        
        }
        else {
            if (resumptionToken != null) {             
               uri +="&resumptionToken="+resumptionToken;
            }
        }      
        if (from != null && resumptionToken == null) {
            uri += "&from="+from;            
        }
        if (metadataPrefix != null && resumptionToken == null) {
            uri +="&metadataPrefix="+metadataPrefix;            
        }
        
        //TODO user
               
        //uri = uri +"&from=2031-01-01"; //TODO DELETE. Just testing no records situation (error-tag)   
        //log.info("resumption token at:"+resumptionToken);
        String response=getHttpResponse(uri,user,password); //TODO USER PASSWORD

        //System.out.println("response:"+response);
        //Important to remove invalid XML encodings since they will be present in metadata. 
        //If they are not replaced, the DOM parse will fail completely to read anything.
        XMLEscapeSanitiser sanitiser = new XMLEscapeSanitiser(""); //Do not replace with anything
        String responseSanitized  =  sanitiser.apply(response);
        
        //System.out.println(response);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        
        
        //If the OAI does not return valid XML, then we can not parse it.
        // All records in this dokument is lost and also those after because we have no resumption token
        Document document = null;
        try {
        document = builder.parse(new InputSource(new StringReader(responseSanitized)));
        document.getDocumentElement().normalize();
        }
        catch(Exception e) {                       
            log.error("Invalid XML from OAI harvest. ",e);            
            log.error("The invalid XML was retrived from this url:"+uri);                        
            completed=true;
            return oaiResponse;  
        }
        
        
        try {
        String error = document.getElementsByTagName("error").item(0).getTextContent();        
          log.info("No records returned from OAI server when harvesting set:"+set +" message:"+error);
        return oaiResponse;// will have no records
        }
        catch(Exception e) {
           //Ignore, no error tag was found 
           
        }
        
        
        String  resumptionToken=  getResumptionToken(document);
        oaiResponse.setTotalRecords(getResumptionTotalSize(document));        
        
        if (resumptionToken != null) {
            this.resumptionToken = resumptionToken;  
            oaiResponse.setResumptionToken(resumptionToken);
        }
        else {
            this.resumptionToken=null;
            completed=true;
            log.info("No more records to load for set="+set);   
        }
         
        NodeList nList = document.getElementsByTagName("record");         

        for (int i =0;i<nList.getLength();i++) {                                         
           
            OaiRecord oaiRecord = new OaiRecord();
            oaiRecords.add(oaiRecord);
            Element record =  (Element)nList.item(i);                                                                    
            String identifier =  record.getElementsByTagName("identifier").item(0).getTextContent();                        
            String headerStatus = getHeaderStatus(record);            
                                              
            oaiRecord.setId(identifier);
            if ("deleted".equals(headerStatus)) {
                oaiRecord.setDeleted(true);
            }
            else {// Get raw XML within the record tag                                    
            Element metadataElement=  (Element) record.getElementsByTagName("metadata").item(0);                            
            String metadataXml = serializeXmlElementToStringUTF8(document, metadataElement);            
            metadataXml = removeMetadataTag(metadataXml);
            //System.out.println(metadataXml);
            oaiRecord.setMetadata(metadataXml);
            }
            
        }

        return oaiResponse;
    }
    
    
    public static String getHttpResponse(String uri, String user, String password) throws Exception {
        
        HttpClient client = HttpClient.newBuilder()
                .authenticator(new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(
                                user,
                                password.toCharArray());
                    }

                }).build();
        
        
        
        HttpRequest request = HttpRequest.newBuilder()                          
                .uri(URI.create(uri))              
                .setHeader("User-Agent", "Java 11 HttpClient Bot")            
                .build();
        
        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
        if (200 != response.statusCode()) {
            log.error("Not status code 200:" + response.statusCode());
            
        }       
         //System.out.println(response.headers());
         
        return response.body();
    }
  
    //Dirty string hacking. But can not find a way to do this with the DOM parser       
    public static String removeMetadataTag(String xml) {   
      if (!xml.startsWith("<metadata>")) {//delete records do not have this        
          return xml;
      }
        
       xml = xml.replaceFirst("<metadata>", "");       
       xml = xml.substring(0,xml.length()-12); //End of string always </metadata>
       return xml;       
    }
       
    
    /*
     * Get the raw XML text from a node. Also make sure encoding is UTF-8.  
     * 
     */
    public String serializeXmlElementToStringUTF8(Document document , Element element) { 
        DOMImplementation impl = document.getImplementation();
        DOMImplementationLS implLS = (DOMImplementationLS) impl.getFeature("LS", "3.0");
        LSSerializer lsSerializer = implLS.createLSSerializer();
        lsSerializer.getDomConfig().setParameter("format-pretty-print", true); // optional.
        LSOutput lsOutput = implLS.createLSOutput();
        lsOutput.setEncoding("UTF-8");  //The reason we do all this stuff.
        Writer stringWriter = new StringWriter();
        lsOutput.setCharacterStream(stringWriter);
        lsSerializer.write(element, lsOutput);
        return stringWriter.toString();
      }

    public String getHeaderStatus(Element record) {
        try {
            Element header =  (Element) record.getElementsByTagName("header").item(0);
            String status = header.getAttribute("status");
              return status;           
            }
            catch(Exception e)
            {
             return null;
            }        
    }
    
    
    public String getResumptionToken( Document document) {
        try {
            String  resumptionToken=  document.getElementsByTagName("resumptionToken").item(0).getTextContent();
            return resumptionToken;                    
        }
        catch(NullPointerException e) { //no more records
         return null;
                     
        }
    }
    
    /*
     * Not required by OAI standard. Cumulus returns it. Pvica does not
     * 
     */
    public String getResumptionTotalSize( Document document) {
        try {
            String totalListSize =  document.getElementsByTagName("resumptionToken").item(0).getAttributes().getNamedItem("completeListSize").getNodeValue();
             return totalListSize;                    
        }
        catch(NullPointerException e) { //no more records
         return "?";                    
        }
    }
    
    
    
}
