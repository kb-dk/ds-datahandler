package dk.kb.datahandler.kaltura;

import static org.mockito.ArgumentMatchers.refEq;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.ListUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.gson.Gson;

import dk.kb.util.Resolver;



public class GenerateKalturaUploadXmlFromSolrDocs {

    final static String XML_KALTURA_ITEM_FRAGMENT_FILE="kaltura/kaltura_item.xml"; //Do not change.
    static String XML_KALTURA_ITEM_FRAGMENT;
    
 
 
 
        
 
    
    
    //Kaltura STAGE
    final static int VIDEO_CONVERSION_PROFILE_ID=1406; //This is Kaltura stage for Video
    final static int AUDIO_CONVERSION_PROFILE_ID=1403; //This is Kaltura stage for Audio
    
    //Kaltura PROD
    //final static int VIDEO_CONVERSION_PROFILE_ID=? // Ask Asger
    //final static int AUDIO_CONVERSION_PROFILE_ID=? // Ask Asger

    
    //This should be same on both STAGE and PROD
    final static int VIDEO_FLAVOR_PARAMS_ID=3;
    final static int AUDIO_FLAVOR_PARAMS_ID=359;
        
    //Kaltura enums
    final static int MEDIATYPE_VIDEO=1;
    final static int MEDIATYPE_AUDIO=5;
        
    final static String TAG1="ds-kaltura"; //This should not be changed
    final static String TAG2="XML-2024-07T11:27"; //Change this to about current date
    final static String TAG3="test"; //Asger used this on kaltura-STAGE,
    
    
    //Custom values that must be changed before running
    final static String SOLR_DOCS_JSON="/home/teg/Desktop/temp/solr_docs.json";

    
    public static void main(String[] args) {
        

        
        try {
            //Load xml fragment file only once
            XML_KALTURA_ITEM_FRAGMENT = readFile(XML_KALTURA_ITEM_FRAGMENT_FILE);
                        
            //Load the solr json with documents and convert to javaDTO's
            ArrayList<KalturaItemXml> itemXmlList = createKalturaItemsFromSolrJson(SOLR_DOCS_JSON);
            
            //Split list into sublistof 500
            List<List<KalturaItemXml>> lists = ListUtils.partition(itemXmlList, 500);
            
           for (List<KalturaItemXml> subList : lists) {
                 generateXmlFromListOfKalturaXML(subList);
                  
           }
            
                        
            //String xmlSubstituted= substituteValues(xml, xmlItem);
            //System.out.println(xmlSubstituted);                        
        }
        catch(Exception e) {        
            System.out.println("Error creating xml");
            e.printStackTrace();

        }


    }

    
    private static String generateXmlFromListOfKalturaXML(List<KalturaItemXml> list) throws Exception {
        StringBuilder xml= new StringBuilder();
        
        //TODO start xml
        for (KalturaItemXml kalturaXml : list) {
            String itemXMLString = substituteValues(XML_KALTURA_ITEM_FRAGMENT, kalturaXml);
            System.out.println(itemXMLString);
                        
        }
        
        
        //TODO end xml
        
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
                        
            //TODO download url depends on environment and origin and must be calculated from file_id
            String downloadUrl="http://test";
            KalturaItemXml itemXml= new KalturaItemXml(type, referenceId, name, description, tag1, tag2, tag3, conversionProfileId,  type,flavorParamsId, downloadUrl);
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
           return doc.getString("field");
        }
        catch(Exception e) {                      
            return "";            
        }
    }
    
    
    
    private static String substituteValues(String xml, KalturaItemXml xmlItem) {        
        xml=xml.replaceFirst("#TYPE", ""+xmlItem.getType());
        xml=xml.replaceFirst("#REFERENCE_ID", xmlItem.getReferenceId());
        xml=xml.replaceFirst("#NAME", xmlItem.getName());
        xml=xml.replaceFirst("#DESCRIPTION", xmlItem.getDescription());
        xml=xml.replaceFirst("#TAG1", xmlItem.getTag1());
        xml=xml.replaceFirst("#TAG2", xmlItem.getTag2());
        xml=xml.replaceFirst("#TAG3", xmlItem.getTag3());
        xml=xml.replaceFirst("#CONVERSION_PROFILE_ID", ""+xmlItem.getConversionProfileId());
        xml=xml.replaceFirst("#MEDIATYPE", ""+xmlItem.getMediaType());
        xml=xml.replaceFirst("#FLAVOR_PARAMS_ID",""+xmlItem.getFlavorParamsId());
        xml=xml.replaceFirst("#DOWNLOAD_URL", ""+xmlItem.getDownloadUrl());
        return xml;
    }

}
