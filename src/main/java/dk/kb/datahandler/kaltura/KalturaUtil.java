package dk.kb.datahandler.kaltura;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kaltura.client.enums.MediaType;

import dk.kb.datahandler.config.ServiceConfig;

public class KalturaUtil {


    private static String DOMS_RADIOTV_PATH;     
    private static String PRESERVICA_TV_PATH;
    private static String PRESERVICA_RADIO_PATH;
    
    //these values are from preservica field and has to be defined here.   
    enum ORGINATES_FROM {
        DOMS,
        Preservica
    }
    
    private static final Logger log = LoggerFactory.getLogger(KalturaUtil.class);

    static {        
        DOMS_RADIOTV_PATH = ServiceConfig.getStreamPathDomsRadioTv();
        PRESERVICA_TV_PATH=ServiceConfig.getStreamPathPreservicaTv();
        PRESERVICA_RADIO_PATH=ServiceConfig.getStreamPathPreservicaRadio();        
    }
    
    /**
     * Map resourceDescription to Kaltura MediaType
     * 
     * @param resourceDescription VideoObject or AudioObject
     * @return Kaltura MediaType with correct format.
     */
    public static MediaType getMediaType(String resourceDescription) {
        
        if ("VideoObject".equals(resourceDescription)) {             
            return MediaType.VIDEO;
        }
        else {
            return MediaType.AUDIO;
        }                         
    }
    
    /**
     * Map Kaltura MediaType to correct conversionProfileId
     * 
     * @param mediaType Video or Audio
     * @return conversionProfileId corrosponds to a Id on kaltura, that specifies what flavors should be created from
     * source file. Dependent on the KMC partner ID. Not the same across kaltura environments
     */
    public static int getGetConversionProfileId(MediaType mediaType) {
        if (MediaType.VIDEO.equals(mediaType)){
            return ServiceConfig.getConversionProfileIdVideo();
        }
        else {
            return ServiceConfig.getConversionProfileIdAudio();
        }                         
    }
    
    
    /**
     * Generate the path on the filesystem to the stream.
     * 
     * @param solrFilePath the partial filePath from preservica that now has the parent folders and can have extension. But missing mount.
     * @param originatesFrom DOMS or Preservica
     * @param resourceDescription VideoObject or AudioObjext  
     * @throws IOException If path can not be resolved
     */
    public static String generateStreamPath(String solrFilePath, String originatesFrom, String resourceDescription) throws IOException {        
        if (ORGINATES_FROM.DOMS.name().equals(originatesFrom)) {            
                return generateDomsDownloadPath(solrFilePath);                        
        }
        else if (ORGINATES_FROM.Preservica.name().equals(originatesFrom)) {        
            return generatePreservicaRadioTvPath(solrFilePath, resourceDescription);                
        }
       else {
          log.warn("Unknown originatesFrom='{}' for solr filePath= '{}'", originatesFrom, solrFilePath);
          throw new IOException ("UnKnown originates from:"+originatesFrom);
       }        
    }
    
    
    // 2 character folders, different folder for tv and radio. No file extension
    private static String generatePreservicaRadioTvPath(String filePath, String resourceDescription) {           
           
        if ("VideoObject".equals(resourceDescription)){
            return  PRESERVICA_TV_PATH+filePath;    
        }
        else if ("AudioObject".equals(resourceDescription)){
            return  PRESERVICA_RADIO_PATH+filePath;
        }                     
        else { //should not happen here since this is also validated when creating the records.
            log.warn("Resource description='{}' unknown for filePath='{}'",resourceDescription,filePath); //Default to radio. 
            return  PRESERVICA_RADIO_PATH+filePath;
        }       
    }
   

    // 1  character folders and same folder for radio and tv. Extension is path of fullpath, so resourcetype not needed
    private static String generateDomsDownloadPath(String filePath) {                         
        return DOMS_RADIOTV_PATH+filePath;               
    }
    
}
