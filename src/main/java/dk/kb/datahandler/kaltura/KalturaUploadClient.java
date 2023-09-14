package dk.kb.datahandler.kaltura;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kaltura.client.KalturaClient;
import com.kaltura.client.KalturaConfiguration;
import com.kaltura.client.enums.KalturaMediaType;
import com.kaltura.client.enums.KalturaSessionType;
import com.kaltura.client.types.KalturaMediaEntry;
import com.kaltura.client.types.KalturaUploadToken;
import com.kaltura.client.types.KalturaUploadedFileTokenResource;


public class KalturaUploadClient {

    private KalturaClient client = null;
    private static final Logger log = LoggerFactory.getLogger(KalturaUploadClient.class);

    /**
     * Instantiate a session to Kaltura that can be used to upload files. The session will be reused for uploads.
     * This method can only upload files up to 2GB due to HTML spec limit. To upload larger files this method
     * must be changed to support upload by chunking the file.  
     * 
     * @param kalturaUrl The Kaltura API url. Using the baseUrl will automatic append the API service part to the URL. 
     * @param userId The userId that must be defined in the kaltura, userId is email xxx@kb.dk in our kaltura
     * @param partnerId The partner id for kaltura. Kind of a collectionId. 
     * @param adminSecret The admin secret that must not be shared that gives access to API
     */
    public KalturaUploadClient(String kalturaUrl, String userId, int partnerId, String adminSecret) throws Exception {
        try {
            KalturaConfiguration config = new KalturaConfiguration();
            config.setEndpoint(kalturaUrl);
            KalturaClient client = new KalturaClient(config);
            String ks = client.generateSession(adminSecret, userId, KalturaSessionType.ADMIN, partnerId);
            client.setKs(ks);
            this.client=client;
        }
        catch (Exception e) {
            log.error("Connecting to Kaltura failed for kaltura url:"+kalturaUrl,e.getMessage());
            throw new Exception (e);
        }

    }

    /**
     * @param filePath File path to the media file to upload. 
     * @param referenceId. Use our internal ID's there. The referenceId that later be used to a pointer to the play component to show the file
     * @return Status message from Kaltura. The message will return the internal id.    
     */

    public String uploadFile(String filePath, KalturaMediaType mediaType, String name, String description, String referenceId) throws Exception {

        log.info("Upload started for file "+filePath + " with referenceId"+referenceId);     
        try {     
            // add the media entry
            KalturaMediaEntry mediaEntry = new KalturaMediaEntry();
            mediaEntry.name = name;
            mediaEntry.mediaType = mediaType;
            mediaEntry.description=description;
            mediaEntry.referenceId=referenceId;

            mediaEntry = client.getMediaService().add(mediaEntry);

            // add the upload token
            KalturaUploadToken uploadToken = new KalturaUploadToken();
            uploadToken = client.getUploadTokenService().add(uploadToken);

            // initialize the file
            File file = new File(filePath);

            // upload the file
            client.getUploadTokenService().upload(uploadToken.id, file);

            // link between the file and the media entry
            KalturaUploadedFileTokenResource uploadedFileTokenResource = new KalturaUploadedFileTokenResource();
            uploadedFileTokenResource.token = uploadToken.id;
            KalturaMediaEntry addContent = client.getMediaService().addContent(mediaEntry.id, uploadedFileTokenResource);     

            String kalturaId=addContent.id;

            log.info("Upload completed. File "+filePath +" got internal id:"+kalturaId);
            return kalturaId;
        }
        catch (Exception e) {
            log.error("Error uploadfile file to kaltura:"+filePath,e.getMessage());
            throw new Exception (e);
        }          
    }   
}




