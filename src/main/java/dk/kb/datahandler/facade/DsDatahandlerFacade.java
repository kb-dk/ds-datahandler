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
import dk.kb.datahandler.util.HarvestTimeUtil;


public class DsDatahandlerFacade {


    private static final Logger log = LoggerFactory.getLogger(DsDatahandlerFacade.class);


    public static Integer oaiIngestDelta(String oaiTargetName) throws Exception {                

        OaiTargetDto oaiTargetDto = ServiceConfig.getOaiTargets().get(oaiTargetName);        
        String datestamp = HarvestTimeUtil.loadLastHarvestTime(oaiTargetDto);        
        return oaiIngest(oaiTargetDto, datestamp);
    }


    public static Integer oaiIngestFull(String oaiTargetName) throws Exception {
        OaiTargetDto oaiTargetDto = ServiceConfig.getOaiTargets().get(oaiTargetName);   
        return oaiIngest(oaiTargetDto , null);
    }


    /*
     * If from is null it will harvest everything.
     * Format for from is yyyy-MM-dd as this is only one supported by COPs/Cumulus.
     * Will be changed later when more OAI targets comes.
     *  
     */
    public static Integer oaiIngest(OaiTargetDto oaiTargetDto, String from) throws Exception {

        //In the OAI spec, the from parameter can be both yyyy-MM-dd or full UTC timestamp (2021-10-09T09:42:03Z)        
        //But COP only supports the short version. So when this is called use short format
        //Dirty but quick solution fix.
        if (oaiTargetDto.getUrl().indexOf("kb.dk/cop/")> 0) {
            from = from.substring(0,10);               
        }

        String recordBase=oaiTargetDto.getName(); //FIX
        String baseUrl=oaiTargetDto.getUrl();
        String set=oaiTargetDto.getSet();
        String metadataPrefix = oaiTargetDto.getMetadataprefix();
        String user = oaiTargetDto.getUsername();
        String password = oaiTargetDto.getPassword();

        DsStorageApi dsAPI = getDsStorageApiClient();        
        OaiHarvestClient client = new OaiHarvestClient(baseUrl, set, metadataPrefix, from, user, password);
        OaiResponse response = client.next();
        int totalRecordLoaded=0;
        while (response.getRecords().size() >0) {

            for (OaiRecord  oaiRecord : response.getRecords()) {                
                totalRecordLoaded++;
                String storageId=recordBase+":"+oaiRecord.getId();
                if (oaiRecord.isDeleted()) { //mark for delete
                    dsAPI.markRecordForDelete(storageId);  
                }
                else { //Create or update                
                    DsRecordDto dsRecord = new DsRecordDto();
                    dsRecord.setId(storageId);
                    dsRecord.setBase(recordBase);
                    dsRecord.setData(oaiRecord.getMetadata());                                          
                    dsAPI.recordCreateOrUpdateRecordPost(dsRecord);
                }
            }
            log.info("Ingesting base:"+recordBase + " process:"+totalRecordLoaded +" out of a total of "+response.getTotalRecords());            

            //Update timestamp with timestamp from last OAI record.
            OaiRecord lastRecord = response.getRecords().get(response.getRecords().size()-1);                        
            HarvestTimeUtil.updateDatestampForOaiTarget(oaiTargetDto,lastRecord.getDateStamp());

            response = client.next(); //load next (may be empty)            
        }

        log.info("Completed full Ingesting base:"+recordBase+ " process:"+totalRecordLoaded);
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
