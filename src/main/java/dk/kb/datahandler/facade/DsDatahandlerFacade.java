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
            
        DsStorageApi dsAPI = getDsStorageApiClient();
        
//        String cumulusImageRecordBase="cumulus_image";
        String vManusRecordBase="cumulus_image";
        String baseUrl="http://www5.kb.dk/cop/oai/";
 
        
        //String metadataPrefix="mods";        
        //String verb="ListRecords";
        String set="oai:kb.dk:images:billed:2010:okt:billeder";
        String set_vmanus="oai:kb.dk:manus:vmanus:2011";
        OaiHarvestClient client = new OaiHarvestClient(baseUrl, set_vmanus);

        OaiResponse response = client.next();
        int totalRecordLoaded=0;
        while (response.getRecords().size() >0) {
            
            for (OaiRecord  oaiRecord : response.getRecords()) {            
                totalRecordLoaded++;
                DsRecordDto dsRecord = new DsRecordDto();
                dsRecord.setId(vManusRecordBase+":"+oaiRecord.getId());
                dsRecord.setBase(vManusRecordBase);
                dsRecord.setData(oaiRecord.getMetadata());            
                dsAPI.createOrUpdateRecordPost(dsRecord);                        
            }
            log.info("Ingesting base:"+vManusRecordBase + " process:"+totalRecordLoaded +" out of a total of "+response.getTotalRecords());            
            response = client.next(); //load next (may be empty)            
        }
 
        log.info("Completed full Ingesting base:"+vManusRecordBase + " process:"+totalRecordLoaded +" out of a total of "+response.getTotalRecords());
        return totalRecordLoaded;
    }
    
    private static DsStorageApi getDsStorageApiClient() {
        ApiClient apiClient = new ApiClient();
        apiClient.setHost("devel11.statsbiblioteket.dk");
        apiClient.setPort(10001);
        apiClient.setBasePath("/ds-storage/v1");
        DsStorageApi dsAPI = new DsStorageApi(apiClient);
        return dsAPI;
    }
    
}
