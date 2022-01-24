package dk.kb.datahandler.storageclient;

import dk.kb.datahandler.api.v1.DsDatahandlerApi;
import dk.kb.datahandler.backend.api.v1.DsStorageApi;
import dk.kb.datahandler.backend.invoker.v1.ApiClient;
import dk.kb.datahandler.backend.model.v1.RecordBaseDto;

import java.util.List;


/*
 * This is an integration test that must be run manually.
 *
 */
public class StorageClientTest{

    
    public static void main(String[] args) throws Exception {
        //Få main metoden i StorageClientTest til at lave et rigtigt kald.  Devel server API kører her: http://devel11:10001/ds-storage/api/
        //Følgende metode på storage er simpel og kræver ingen parametre:
        //http://devel11:10001/ds-storage/v1/getBasesConfiguration
    
        ApiClient apiClient = new ApiClient();
        apiClient.setHost("devel11.statsbiblioteket.dk");
        apiClient.setPort(10001);
        apiClient.setBasePath("/ds-storage/v1");
        DsStorageApi dsAPI = new DsStorageApi(apiClient);
        List<RecordBaseDto> basesConf = dsAPI.getBasesConfiguration();        
        System.out.println(basesConf);
    }
    
}
