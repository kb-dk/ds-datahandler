package dk.kb.datahandler.kaltura;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONObject;


import dk.kb.util.Resolver;


/**
 * The purpose of this class is to parse a Solr document from ds-solr with records and make Kaltura XML's with downloadlink and metadata for bulk upload in Kaltura.
 * The XML files are then upload to Kaltura manual if there are few, or sent to Petur for very large scale uploads.
 * <p>
 * The solr json document can be produced with this query: holdback_expired_date:[NOW TO *] 
 * Notice for stage you also have to add the file_id:* since we have documents without streams 
 * <p>
 * The Kaltura upload XML has the following structure. To see the item XML see the 'kaltura_item.xml' template. 
 * Change the template if upload format changes or maybe one of the 3 tags are removed 
 * The number of items in each file must be limited or kaltura will break. The limit is currently sat at 100 items for each xml file.
 * <p>
 * <?xml version='1.0' encoding='utf-8'?>
 * <mrss>
 *   <channel>
 *     <item>...</item>
 *     <item>...</item>
 *     .... more items
 *   </channel>
 * </mrss>   
 * <p>
 *
 * The file name of the xml files should follow this syntax. Each file having 100 items.
 * DRA_2024-07-07_PROD_1.xml
 * DRA_2024-07-07_PROD_2.xml'
 * ..... 
 * <p>
 * Before starting the job, read the constants below and change values. 
 * <p>
 * Test file can be extracted from solr with:
 * {@code curl 'http://devel11:10007/solr/ds/select?indent=true&q.op=OR&q=*%3A*%20AND%20migrated_from%3ADOMS%20AND%20file_id%3A*&rows=501&useParams=' > solr_doms.json }
 * {@code curl 'http://devel11:10007/solr/ds/select?indent=true&q.op=OR&q=*%3A*%20AND%20NOT%20migrated_from%3ADOMS%20AND%20file_id%3A*&rows=501&useParams=' > solr_preservia.json}
 * <p>
 * Always test the download urls are correct and working before uploading to Kaltura
 *
 */

public class GenerateKalturaUploadXmlFromSolrDocs {

    final static String XML_KALTURA_ITEM_FRAGMENT_FILE="kaltura/kaltura_item.xml"; //Do not change.
    final static String XML_START="<?xml version='1.0' encoding='utf-8'?>\n<mrss>\n<channel>\n";
    final static String XML_END="<\\channel>\n<\\mrss>\n";        

    static String XML_KALTURA_ITEM_FRAGMENT; //Not final since it will be loaded once in the main method

    // The number of item xml blocks in each file. This is a limit Kaltura like.
    final static int NUMBER_ITEMS_IN_EACH=100;

    //Change for STAGE or PROD! (I do not know PROD values yet)
    final static int VIDEO_CONVERSION_PROFILE_ID=1406; //This is Kaltura stage for Video
    final static int AUDIO_CONVERSION_PROFILE_ID=1403; //This is Kaltura stage for Audio

    //The KUANA paths (stage) FTP server has not been established
    //Name for stage will probably be: deic-download-stage.kb.dk
    final static String FTP_PRESERVICA_RADIOTV_PATH="https://deic-download.kb.dk/radio-tv/";     
    final static String FTP_BART_TV_PATH="https://deic-download.kb.dk/kuana-store/bart-access-copies-tv/";
    final static String FTP_BART_RADIO_PATH="https://deic-download.kb.dk/kuana-store/bart-access-copies-radio/";

    //This should be same on both STAGE and PROD
    final static int VIDEO_FLAVOR_PARAMS_ID=3;
    final static int AUDIO_FLAVOR_PARAMS_ID=359;

    //Kaltura enums. Same on stage/prod
    final static int MEDIATYPE_VIDEO=1;
    final static int MEDIATYPE_AUDIO=5;

    final static String TAG1="ds-kaltura"; //This should not be changed
    final static String TAG2="XML-2024-09-23"; //Change this to about current date
    final static String TAG3="test"; //Asger used this on kaltura-STAGE. Dont know what to use for PROD.


    //Custom values that must be changed before running
    final static String SOLR_DOCS_JSON="/home/teg/Desktop/temp/solr_preservica_audio.json";
    //The output folder for the xml files
    final static String OUTPUT_FOLDER="/home/teg/Desktop/temp/";
    //Fix Date in file before running job.
    final static String XML_FILE_PATTERN="DRA_2024-9-23_STAGE_#NUMBER.xml";   //Kaltura stage
    //static String filePattern="DRA_2024-07-07_PROD_#NUMBER.xml";  //Kaltura production



    public static void main(String[] args) {

        try {
            //Load xml fragment file only once
            XML_KALTURA_ITEM_FRAGMENT = readFile(XML_KALTURA_ITEM_FRAGMENT_FILE);

            //Load the solr json with documents and convert to javaDTO's
            ArrayList<KalturaItemXml> itemXmlList = createKalturaItemsFromSolrJson(SOLR_DOCS_JSON);
            System.out.println("Solr document has #docs="+itemXmlList.size());

            //Split list into sublists
            List<List<KalturaItemXml>> lists = ListUtils.partition(itemXmlList, NUMBER_ITEMS_IN_EACH);
            int blockCount=1;

            //generate file for each block 
            for (List<KalturaItemXml> subList : lists) {
                String xmlWithItems = generateXmlFromListOfKalturaXML(subList);                                         
                String fileName=XML_FILE_PATTERN.replaceFirst("#NUMBER", ""+blockCount++);
                Path filePath=Paths.get(OUTPUT_FOLDER+fileName);

                //Save file
                writeToFile(xmlWithItems, filePath);                                             
            }                                                                  
        }
        catch(Exception e) {        
            System.out.println("Error creating xml");
            e.printStackTrace();

        }
    }

    private static String generateXmlFromListOfKalturaXML(List<KalturaItemXml> list) throws Exception {
        StringBuilder xml= new StringBuilder();

        xml.append(XML_START);
        for (KalturaItemXml kalturaXml : list) {
            String itemXMLString = substituteValues(XML_KALTURA_ITEM_FRAGMENT, kalturaXml);
            xml.append(itemXMLString);
            xml.append("\n"); 
        }
        xml.append(XML_END);

        return xml.toString();
    }

    private static ArrayList<KalturaItemXml> createKalturaItemsFromSolrJson(String jsonFile) throws Exception{

        String solrJson= readFile(jsonFile);;

        JSONObject solrResponse = new JSONObject(solrJson);
        JSONArray json_docs = solrResponse.getJSONObject("response").getJSONArray("docs");

        ArrayList<KalturaItemXml> itemXmlList = new ArrayList<KalturaItemXml>(); 

        for (int i=0;i< json_docs.length();i++) {

            JSONObject doc = (JSONObject) json_docs.get(i);

            String referenceId= doc.getString("file_id");            
            String name = getFirstTitle(doc);
            String description = getFieldOrEmpty(doc,"description");
            String migratedFrom=getFieldOrEmpty(doc, "migrated_from");
            String tag1= TAG1;
            String tag2= TAG2;
            String tag3= TAG3;
            int type;  //Will be different for video/audio
            int conversionProfileId; //Will be different for video/audio
            int flavorParamsId; //Will be different for video/audio

            String resourceType=doc.getString("resource_description");
            if ("VideoObject".equals(resourceType)) {                
                type=MEDIATYPE_VIDEO;
                conversionProfileId=VIDEO_CONVERSION_PROFILE_ID;
                flavorParamsId=VIDEO_FLAVOR_PARAMS_ID;                
            }
            else if ("AudioObject".equals(resourceType)) {
                type=MEDIATYPE_AUDIO;
                conversionProfileId=AUDIO_CONVERSION_PROFILE_ID;
                flavorParamsId=AUDIO_FLAVOR_PARAMS_ID;
            }
            else {
                System.out.println("Unknown resource_description:"+resourceType);
                continue; //Do not add 
            }


            String downloadUrl=generateDownloadUrl(migratedFrom,resourceType,referenceId);
            KalturaItemXml itemXml= new KalturaItemXml(type, referenceId, name, description, migratedFrom, tag1, tag2, tag3, conversionProfileId,  type,flavorParamsId, downloadUrl);
            itemXmlList.add(itemXml);                                                  
        }

        return itemXmlList;        
    }

    /*
     * Read a text file to string.  
     */
    private static String readFile(String fileName) throws IOException {
        String text = Resolver.resolveUTF8String(fileName);       
        return text;
    }


    // Return first title value since it is multivalued. All documents should have at least 1 title.
    private static String getFirstTitle(JSONObject doc ) {
        try {
            JSONArray titleArray = doc.getJSONArray("title"); //title field is multivalued
            return titleArray.get(0).toString(); //First
        }
        catch(Exception e) {            
            System.out.println("No title found for id:"+doc.getString("ID"));
            return "";

        }
    }


    //Return empty string if field does not exist
    private static String getFieldOrEmpty(JSONObject doc, String field ) {
        try {
            return doc.getString(field);
        }
        catch(Exception e) {                      
            return "";            
        }
    }

    /*
     * Escape text correct for XML 
     */
    private static String xmlEncode(String text) {        
        return StringEscapeUtils.escapeXml11(text);        
    }


    /*
     * Write text to a new file
     */
    private static void writeToFile(String text, Path filePath) throws Exception {        
        Files.writeString(filePath, text, StandardCharsets.UTF_8);        
        System.out.println("Created file:"+filePath.toAbsolutePath());
    }


    /*
     * Substitute values in the xml template. Use XML encode for strings that can have special characters.
     */
    private static String substituteValues(String xml, KalturaItemXml xmlItem) {        
        xml=xml.replaceFirst("#TYPE",""+xmlItem.getType());
        xml=xml.replaceFirst("#REFERENCE_ID",  xmlEncode(xmlItem.getReferenceId()));
        xml=xml.replace("#NAME",  xmlEncode(xmlItem.getName())); //Not regexp replace since text can be complex
        xml=xml.replace("#DESCRIPTION",  xmlEncode(xmlItem.getDescription())); //Not regexp replace since text can be complex
        xml=xml.replaceFirst("#TAG1",  xmlEncode(xmlItem.getTag1()));
        xml=xml.replaceFirst("#TAG2",  xmlEncode(xmlItem.getTag2()));
        xml=xml.replaceFirst("#TAG3",  xmlEncode(xmlItem.getTag3()));
        xml=xml.replaceFirst("#CONVERSION_PROFILE_ID", ""+xmlItem.getConversionProfileId());
        xml=xml.replaceFirst("#MEDIATYPE", ""+xmlItem.getMediaType());
        xml=xml.replaceFirst("#FLAVOR_PARAMS_ID",""+xmlItem.getFlavorParamsId());
        xml=xml.replaceFirst("#DOWNLOAD_URL", ""+ xmlEncode(xmlItem.getDownloadUrl()));
        return xml;
    }




    //File path examples. Notice there is no extension for bart-tv and bart-radio
    //Example Preservica/Kuana: https://deic-download.kb.dk/radio-tv/e/b/4/f/eb4fcb8c-99e3-415b-8cc8-33a6ffb17b73.mp4
    //Example bart-tv:   https://deic-download.kb.dk/kuana-store/bart-access-copies-tv/00/01/32
    //Example bart-radio: https://deic-download.kb.dk/kuana-store/bart-access-copies-radio/00/00/02/00000288-7859-4782-b736-a2dc964316e8

    private static String generateDownloadUrl(String migratedFrom, String resourceType,String referenceId) {

        if ("DOMS".equals(migratedFrom)){ //This is bart
            if ("VideoObject".equals(resourceType)) {              
                return generateBartTvDownloadUrl(referenceId);

            }
            else {              
                return generateBartRadioDownloadUrl(referenceId);
            }          
        }
        else {
            return generateKuanaTVDownloadUrl(referenceId, resourceType);              
        }        
    }


    // 2  character folders
    private static String generateBartTvDownloadUrl(String fileId) {           
        String pathSplit= fileId.substring(0,2)+"/"+fileId.substring(2,4)+"/"+fileId.substring(4,6)+"/"+fileId;       
        return FTP_BART_TV_PATH+pathSplit;     
    }

    // 2  character folders
    private static String generateBartRadioDownloadUrl(String fileId) {           
        String pathSplit= fileId.substring(0,2)+"/"+fileId.substring(2,4)+"/"+fileId.substring(4,6)+"/"+fileId;       
        return FTP_BART_RADIO_PATH+pathSplit;               
    }

    // 1  character folders + add extension .mp3 or .mp4
    private static String generateKuanaTVDownloadUrl(String fileId,String resourceType) {           
        String pathSplit= fileId.substring(0,1)+"/"+fileId.substring(1,2)+"/"+fileId.substring(2,3)+"/"+fileId;       
        String fileWithOutExtension=FTP_PRESERVICA_RADIOTV_PATH+pathSplit; //Must add extension .mp3 og .mp4               
        if ("VideoObject".equals(resourceType)) {
            return fileWithOutExtension+".mp4";            
        }
        else {
            return fileWithOutExtension+".mp3";
        }            
    }

}
