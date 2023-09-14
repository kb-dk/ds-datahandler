package dk.kb.datahandler.kaltura;

import com.kaltura.client.enums.KalturaMediaType;

public class KalturaIUploadIntegrationTest {

    /**
     * This will upload a file to kaltura. 
     * Do not check in code with an adminsecret to kaltura!
     * 
     */
    public static void main(String[] args)  {
        try {
            //Create the client
            String kalturaUrl= "https://kmc.kaltura.nordu.net";        
            String adminSecret = "XXXXXXXXXXXXXXXXXXXXXXXXX"; 
            Integer partnerId = 380; // Use this partner ID for DS project 
            String userId = "teg@kb.dk"; //User must exist in kaltura.                 
            KalturaUploadClient client = new KalturaUploadClient(kalturaUrl,userId,partnerId,adminSecret);

            //Upload a file
            String filePath = "/media/teg/1200GB_SSD/workspace/KalturaUploadClient/05052581-fb20-43a4-82c7-d5596d575aaf.mp4";
            KalturaMediaType mediaType = KalturaMediaType.VIDEO;
            String id = client.uploadFile(filePath,mediaType,"Test upload from integration client","This is uploaded from integration client","abcd31415");
            System.out.println("Upload succes and got kaltura id:"+id);
        }
        catch(Exception e) {        
            e.printStackTrace();      

        }
    }

}
