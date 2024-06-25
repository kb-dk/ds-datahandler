package dk.kb.datahandler.oai;

import java.io.IOException;
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import dk.kb.util.webservice.exception.InternalServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import dk.kb.datahandler.model.v1.OaiTargetDto;
import dk.kb.util.xml.XMLEscapeSanitiser;

import org.w3c.dom.ls.*;
import org.xml.sax.SAXException;

public class OaiHarvestClient {

    private static final Logger log = LoggerFactory.getLogger(OaiHarvestClient.class);

    private OaiTargetJob oaiTargetJob = null;
    private OaiTargetDto oaiTarget = null;
    private boolean completed=false;
    private String resumptionToken=null;
    private String from;

    public OaiHarvestClient(OaiTargetJob oaiTargetJob, String from){
        this.oaiTargetJob=oaiTargetJob;
        this.oaiTarget=oaiTargetJob.getDto();
        this.from=from;
    }


    public OaiResponse next() throws IOException {
        OaiResponse oaiResponse = new OaiResponse();

        String baseURL=oaiTarget.getUrl();
        String uri= baseURL+"?verb=ListRecords";

        if (completed) {            
            //The caller should know not to ask for more since last batch had 0 entries.
            log.info("No more records to load for oai target:"+oaiTarget.getName());
            return new OaiResponse();
        }

        String set= oaiTarget.getSet();
        String metadataPrefix= oaiTarget.getMetadataprefix();

        uri=addQueryParamsToUri(uri, set, resumptionToken,metadataPrefix,from);
        log.info("calling uri:"+uri);
        //log.info("resumption token at:"+resumptionToken);
        String xmlResponse = getHttpResponse(uri, oaiTarget.getUsername(), oaiTarget.getPassword());

        Document document = sanitizeXml(xmlResponse,uri);

        String errorMessage = getErrorMessage(document);
         if (errorMessage != null && errorMessage.trim().length() >1) {                       
            log.info("Error message from OAI server when harvesting set:"+set +" message:"+errorMessage);                    
            oaiTargetJob.setCompletedTime(System.currentTimeMillis());            
            oaiResponse.setError(true);
            return oaiResponse;// will have no records
         }


        //More records or completed.
        String  resumptionToken=  getResumptionToken(document);
        oaiResponse.setTotalRecords(getResumptionTotalSize(document));        

        if (resumptionToken != null && !resumptionToken.isEmpty()) {
            this.resumptionToken = resumptionToken;  
            log.debug("next resumption token:"+resumptionToken);
            oaiResponse.setResumptionToken(resumptionToken);
        }
        else {
            this.resumptionToken=null;
            completed=true;
            log.info("No more records to load for set="+set);   
        }

        ArrayList<OaiRecord> records=extractRecordsFromXml(document);

        oaiResponse.setRecords(records);
        return oaiResponse;
    }


    /* Will construct the uri for next http request. Resumption token will be set if not null.
     * Also special coding since  Cumulus/Cups API is not OAI-PMH compliant. 
     */
    private String addQueryParamsToUri(String uri,String set, String resumptionToken, String metadataPrefix, String from) {

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
        return uri;
    }

    public ArrayList<OaiRecord> extractRecordsFromXml( Document document ) {
        NodeList nList = document.getElementsByTagName("record"); 

        ArrayList<OaiRecord> records= new ArrayList<OaiRecord>();
        for (int i =0;i<nList.getLength();i++) {                                         

            OaiRecord oaiRecord = new OaiRecord();
            records.add(oaiRecord);
            Element record =  (Element)nList.item(i);                                                                    
            String identifier =  record.getElementsByTagName("identifier").item(0).getTextContent();                        
            String datestamp =  record.getElementsByTagName("datestamp").item(0).getTextContent();
            String headerStatus = getHeaderStatus(record);            

            oaiRecord.setId(identifier);
            oaiRecord.setDateStamp(datestamp);

            if ("deleted".equals(headerStatus)) {
                oaiRecord.setDeleted(true);
            }
            else {// Get raw XML within the record tag                                    
                Element metadataElement=  (Element) record.getElementsByTagName("metadata").item(0);                            
                String metadataXml = serializeXmlElementToStringUTF8(document, metadataElement);            
                metadataXml = removeMetadataTag(metadataXml.trim());           
                //System.out.println("meta:"+metadataXml);
                oaiRecord.setMetadata(metadataXml);
            }

        }
        return records;
    }


    /*
     * Will return the error message if response contains an error
     * Return null if no error message
     * 
     */
    private String getErrorMessage(Document document) {        
        try {
            String error = document.getElementsByTagName("error").item(0).getTextContent();                               
            return error;
        }
        catch(Exception e) {
            //Ignore, no error tag was found            
        }
       return null;        
    }
    
    //If the OAI does not return valid XML, then we can not parse it.
    // All records in this document is lost and also those after because we have no resumption token

    //Important to remove invalid XML encodings since they will be present in metadata. 
    //If they are not replaced, the DOM parse will fail completely to read anything.


    public static Document sanitizeXml(String xmlResponse, String uri) { //uri only for log

        //System.out.println(response);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        XMLEscapeSanitiser sanitiser = new XMLEscapeSanitiser(""); //Do not replace with anything
        String responseSanitized  =  sanitiser.apply(xmlResponse);
        Document document = null;

        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.parse(new InputSource(new StringReader(responseSanitized)));
            document.getDocumentElement().normalize();
        } catch (IOException | SAXException | ParserConfigurationException e) {
            log.error("Invalid XML from OAI harvest from this URI: '{}'", uri, e);
            //throw new IOException()
            throw new InternalServiceException("Invalid XML from OAI harvest from this URI: '{}'", uri, e);
        }

        return document;

    }

    /**
     * Call server and get response setting both password callback authenticator and set basic authentication in every single call
     * Preservica5  used the callback and set a session cookie
     * Preservica6 wants basic authentication in every single call.
     * <p>
     * The solution is to do both.
     */
    protected static String getHttpResponse(String uri, String user, String password) throws IOException {
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
                .header("User-Agent", "Java 11 HttpClient Bot")
                .header("Authorization", getBasicAuthenticationHeader(user, password))
                .build();

        HttpResponse<String> response = null;
        try {
            response = client.send(request, BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            log.warn("An error occurred when sending OAI-PMH request: '{}'", request.toString());
            throw new IOException(e);
        }
        if (200 != response.statusCode()) {
            log.error("Not status code 200:" + response.statusCode());

        }       
      
        //log.debug("http header:"+response.headers());
        //log.debug("http body:"+response.body());if

        return response.body();
    }

    //Dirty string hacking. But can not find a way to do this with the DOM parser       
    protected static String removeMetadataTag(String xml) {   
        xml = xml.replaceFirst("<metadata>", "");       
        xml = xml.substring(0,xml.length()-11); //End of string always </metadata>
        return xml;       
    }


    /*
     * Get the raw XML text from a node. Also make sure encoding is UTF-8.  
     * 
     */
    private String serializeXmlElementToStringUTF8(Document document , Element element) { 
        DOMImplementation impl = document.getImplementation();
        DOMImplementationLS implLS = (DOMImplementationLS) impl.getFeature("LS", "3.0");
        LSSerializer lsSerializer = implLS.createLSSerializer();
        // lsSerializer.getDomConfig().setParameter("format-pretty-print", true); //         
        LSOutput lsOutput = implLS.createLSOutput();
        lsOutput.setEncoding("UTF-8");  //The reason we do all this stuff.
        Writer stringWriter = new StringWriter();
        lsOutput.setCharacterStream(stringWriter);
        lsSerializer.write(element, lsOutput);
        String xml_utf8= stringWriter.toString();
        
        xml_utf8=nameFixPvica(xml_utf8);                        
        return xml_utf8;
    }

    /**
     * Replaces malformed namespaces with correctly defined namespaces.
     * Records with the preservica namespace XIP are delivered without the namespace correctly defined.
     * The OAI-PMH resultset contains the namespace at the top of the set and is then inherited by records.
     * @param xml_utf8  XML file containing a single XIP record,
     *                  which is either a xip:Manifestation or xip:DeliverableUnit.
     * @return          the xip XML record, with the namespace updated.
     */
    public static String nameFixPvica(String xml_utf8) {
        xml_utf8=xml_utf8.replaceFirst("<xip:(DeliverableUnit|Manifestation|Collection)","<xip:$1 xmlns:xip=\"http://www.tessella.com/XIP/v4\"");
        xml_utf8=xml_utf8.replaceFirst("xmlns:PBCoreDescriptionDocument=\"http://www.pbcore.org/PBCore/PBCoreNamespace.html\" xsi:schemaLocation=\"http://www.pbcore.org/PBCore/PBCoreNamespace.html\">",  "xmlns:PBCoreDescriptionDocument=\"http://www.pbcore.org/PBCore/PBCoreNamespace.html\" xsi:schemaLocation=\"http://www.pbcore.org/PBCore/PBCoreNamespace.html\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">");
                                        
        return xml_utf8;
    }
    
    private String getHeaderStatus(Element record) {
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

    private static final String getBasicAuthenticationHeader(String username, String password) {    	
    	String valueToEncode = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(valueToEncode.getBytes(StandardCharsets.UTF_8));
    }
    
    
    private String getResumptionToken( Document document) {
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
    private String getResumptionTotalSize( Document document) {
        try {
            String totalListSize =  document.getElementsByTagName("resumptionToken").item(0).getAttributes().getNamedItem("completeListSize").getNodeValue();
            return totalListSize;                    
        }
        catch(NullPointerException e) { //no more records
            return "?";                    
        }
    }



}
