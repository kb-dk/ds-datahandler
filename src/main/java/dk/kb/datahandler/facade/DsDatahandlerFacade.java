package dk.kb.datahandler.facade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.datahandler.backend.api.v1.DsStorageApi;
import dk.kb.datahandler.backend.invoker.v1.ApiClient;
import dk.kb.datahandler.backend.model.v1.DsRecordDto;
import dk.kb.datahandler.config.ServiceConfig;
import dk.kb.datahandler.model.v1.OaiTargetDto;
import dk.kb.datahandler.oai.OaiHarvestClient;
import dk.kb.datahandler.oai.OaiRecord;
import dk.kb.datahandler.oai.OaiResponse;
import dk.kb.datahandler.webservice.exception.InvalidArgumentServiceException;

public class DsDatahandlerFacade {
    
    
    private static final Logger log = LoggerFactory.getLogger(DsDatahandlerFacade.class);
    public static Integer oaiIngestFull(String oaiTarget) throws Exception {
            
        
        OaiTargetDto oaiTargetDto = ServiceConfig.getOaiTargets().get(oaiTarget);
        
        if (oaiTargetDto == null) {            
          throw new  InvalidArgumentServiceException("No OAI targets defined in YAML file for:"+oaiTarget);
        }
        
        String recordBase=oaiTargetDto.getName(); //FIX
        String baseUrl=oaiTargetDto.getUrl();
               
        String set=oaiTargetDto.getSet();
        
        DsStorageApi dsAPI = getDsStorageApiClient();
        OaiHarvestClient client = new OaiHarvestClient(baseUrl, set);

        OaiResponse response = client.next();
        int totalRecordLoaded=0;
        while (response.getRecords().size() >0) {
            
            for (OaiRecord  oaiRecord : response.getRecords()) {            
                totalRecordLoaded++;
                DsRecordDto dsRecord = new DsRecordDto();
                dsRecord.setId(recordBase+":"+oaiRecord.getId());
                dsRecord.setBase(recordBase);
                dsRecord.setData(oaiRecord.getMetadata());            
                dsAPI.createOrUpdateRecordPost(dsRecord);                        
            }
            log.info("Ingesting base:"+recordBase + " process:"+totalRecordLoaded +" out of a total of "+response.getTotalRecords());            
            response = client.next(); //load next (may be empty)            
        }
 
        log.info("Completed full Ingesting base:"+recordBase+ " process:"+totalRecordLoaded +" out of a total of "+response.getTotalRecords());
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
