package dk.kb.datahandler.kaltura;

import java.io.File;

import com.kaltura.client.KalturaClient;
import com.kaltura.client.KalturaConfiguration;
import com.kaltura.client.enums.KalturaMediaType;
import com.kaltura.client.enums.KalturaSessionType;
import com.kaltura.client.types.KalturaMediaEntry;
import com.kaltura.client.types.KalturaUploadToken;
import com.kaltura.client.types.KalturaUploadedFileTokenResource;

public class TestKaltura2 {

    
    public static void main(String[] args) throws Exception{
        String adminSecret = "XXXXXXXXXXXXXXXXXXXXXXXXx";
        Integer partnerId = 380;
        String filePath = "/media/teg/1200GB_SSD/workspace/KalturaUploadClient/05052581-fb20-43a4-82c7-d5596d575aaf.mp4";
        String uploadUserId = "teg@kb.dk";

        // initialize the client
        KalturaConfiguration config = new KalturaConfiguration();
        config.setEndpoint("https://kmc.kaltura.nordu.net");
        KalturaClient client = new KalturaClient(config);
        String ks = client.generateSession(adminSecret, uploadUserId, KalturaSessionType.ADMIN, partnerId);
        client.setKs(ks);

        // add the media entry
        KalturaMediaEntry mediaEntry = new KalturaMediaEntry();
        mediaEntry.name = "TEG test4 MP4";
        mediaEntry.mediaType = KalturaMediaType.VIDEO;
        mediaEntry.description="This is a description for test4";
        
        
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
        System.out.println("completed OK with ID:"+addContent.id);
        
    }
}
