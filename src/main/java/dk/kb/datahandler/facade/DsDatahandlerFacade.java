package dk.kb.datahandler.facade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.datahandler.backend.api.v1.DsStorageApi;
import dk.kb.datahandler.backend.invoker.v1.ApiClient;
import dk.kb.datahandler.backend.model.v1.DsRecordDto;
import dk.kb.datahandler.oai.OaiHarvestClient;
import dk.kb.datahandler.oai.OaiRecord;
import dk.kb.datahandler.oai.OaiResponse;

public class DsDatahandlerFacade {
    
    
    private static final Logger log = LoggerFactory.getLogger(DsDatahandlerFacade.class);
    public static Integer oaiIngestFull(String oaiTarget) throws Exception {
            
        ApiClient apiClient = new ApiClient();
        apiClient.setHost("devel11.statsbiblioteket.dk");
        apiClient.setPort(10001);
        apiClient.setBasePath("/ds-storage/v1");
        DsStorageApi dsAPI = new DsStorageApi(apiClient);
        
        String cumulusImageRecordBase="cumulus_image";
        
        String baseUrl="http://www5.kb.dk/cop/oai/";
        //String metadataPrefix="mods";        
        //String verb="ListRecords";
        String set="oai:kb.dk:images:billed:2010:okt:billeder";
         
        OaiHarvestClient client = new OaiHarvestClient(baseUrl, set);

        OaiResponse response = client.next();
        for (OaiRecord  oaiRecord : response.getRecords()) {            
            DsRecordDto dsRecord = new DsRecordDto();
            dsRecord.setId(cumulusImageRecordBase+"_"+oaiRecord.getId());
            dsRecord.setBase(cumulusImageRecordBase);
            dsRecord.setData(oaiRecord.getMetadata());            
            dsAPI.createOrUpdateRecordPost(dsRecord);                        
        }
        
        
        
        
        log.info("CALLED!");
        return 1000;
    }
    
}
