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
    
  
    
   
    
    private static final Logger log = LoggerFactory.getLogger(KalturaUtil.class);
  
     static {        
        DOMS_RADIOTV_PATH = ServiceConfig.getConfig().getString("streams.domsRadioTvPath");
        PRESERVICA_TV_PATH=ServiceConfig.getConfig().getString("streams.preservicaTvPath");
        PRESERVICA_RADIO_PATH=ServiceConfig.getConfig().getString("streams.preservicaRadioPath");        
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
     * Map Kaltura MediaType  to correct flavourParamId
     * 
     * @param resourceDescription VideoObject or AudioObject
     * @return flavourParamId  FlavourId for video or Audio. Depend on the KMC partner ID. Not the same across kaltura environments
     */
    public static int getFlavourParamId(MediaType mediaType) {
        if (MediaType.VIDEO.equals(mediaType)){
            return ServiceConfig.getConfig().getInteger("kaltura.flavourParamIdVideo");
        }
        else {
            return ServiceConfig.getConfig().getInteger("kaltura.flavourParamIdAudio");
        }                         
    }
    
    
    /**
     * Generate the path of the filesystem to the stream.
     * 
     * @param originatesFrom DOMS or Preservica
     * @param resourceDescription VideoObject or AudioObjext  
     * @throws IOException If path can not be resolved
     */
    public static String generateStreamPath(String fileId, String originatesFrom, String resourceDescription) throws IOException {
        
        if ("DOMS".equals(originatesFrom)) {            
                return generateDomsDownloadPath(fileId, resourceDescription);                        
        }
        else if ("Preservica".equals(originatesFrom)) {        
            return generatePreservicaRadioTvPath(fileId, resourceDescription);                
        }
       else {
          log.warn("Unknown  originatesFrom='{}' for fileID= '{}'", originatesFrom, fileId);
          throw new IOException ("UnKnown originates from:"+originatesFrom);
       }        
    }
    
    // 2 character folders, different folder for tv and radio. No file extension
    private static String generatePreservicaRadioTvPath(String fileId, String resourceDescription) {           
        String pathSplit= fileId.substring(0,2)+"/"+fileId.substring(2,4)+"/"+fileId.substring(4,6)+"/"+fileId;       
        if ("VideoObject".equals(resourceDescription)){
            return  PRESERVICA_TV_PATH+pathSplit;    
        }
        else {
            return  PRESERVICA_RADIO_PATH+pathSplit;
        }                     
    }
   

    // 1  character folders and same folder for radio and tv. Add extension .mp3 or .mp4.
    private static String generateDomsDownloadPath(String fileId,String resourceType) {           
        String pathSplit= fileId.substring(0,1)+"/"+fileId.substring(1,2)+"/"+fileId.substring(2,3)+"/"+fileId.substring(3,4)+"/"+fileId;       
       
        String fileWithOutExtension=DOMS_RADIOTV_PATH+pathSplit; //Must add extension .mp3 og .mp4               
        if ("VideoObject".equals(resourceType)) {
            return fileWithOutExtension+".mp4";            
        }
        else {
            return fileWithOutExtension+".mp3";
        }            
    }
    
}
