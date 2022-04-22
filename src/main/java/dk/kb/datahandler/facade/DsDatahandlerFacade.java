package dk.kb.datahandler.facade;


import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.datahandler.backend.api.v1.DsStorageApi;
import dk.kb.datahandler.backend.invoker.v1.ApiClient;
import dk.kb.datahandler.backend.model.v1.DsRecordDto;
import dk.kb.datahandler.config.ServiceConfig;
import dk.kb.datahandler.model.v1.OaiJobDto;
import dk.kb.datahandler.model.v1.OaiTargetDto;
import dk.kb.datahandler.oai.OaiHarvestClient;
import dk.kb.datahandler.oai.OaiJobCache;
import dk.kb.datahandler.oai.OaiRecord;
import dk.kb.datahandler.oai.OaiResponse;
import dk.kb.datahandler.oai.OaiTargetJob;
import dk.kb.datahandler.util.HarvestTimeUtil;


public class DsDatahandlerFacade {
    
    private static final Logger log = LoggerFactory.getLogger(DsDatahandlerFacade.class);

    public static Integer oaiIngestDelta(String oaiTargetName) throws Exception {                

        OaiTargetDto oaiTargetDto = ServiceConfig.getOaiTargets().get(oaiTargetName);                
        OaiTargetJob job = createNewJob(oaiTargetDto);        
        //register job
        OaiJobCache.addNewJob(job);

        try {
            String datestamp = HarvestTimeUtil.loadLastHarvestTime(oaiTargetDto);        
            int number= oaiIngest(job , datestamp);
            OaiJobCache.finishJob(job, number,false);//No error
            return number;
        }
        catch(Exception e) {
            log.error("Oai delta harvest did not complete succesfull:"+oaiTargetName);
            job.setCompletedTime(System.currentTimeMillis());
            OaiJobCache.finishJob(job, 0,true);//Error                        
            throw new Exception(e);
        }
    }


    public static Integer oaiIngestFull(String oaiTargetName) throws Exception {
        OaiTargetDto oaiTargetDto = ServiceConfig.getOaiTargets().get(oaiTargetName);   
        OaiTargetJob job = createNewJob(oaiTargetDto);

        //register job
        OaiJobCache.addNewJob(job);            

        try {

            int number= oaiIngest(job , null);        
            OaiJobCache.finishJob(job, number,false);//No error
            return number;
        }
        catch(Exception e) {
            log.error("Oai full harvest did not complete succesfull:"+oaiTargetName);
            OaiJobCache.finishJob(job, 0, true);//Error
            job.setCompletedTime(System.currentTimeMillis());                                    
            throw new Exception(e);
        }
    }

    public static List<OaiJobDto> getJobs() throws Exception {    
        List<OaiJobDto> running=OaiJobCache.getRunningJobsMostRecentFirst();
        List<OaiJobDto> completed=OaiJobCache.getCompletedJobsMostRecentFirst();
        List<OaiJobDto> result = new ArrayList<OaiJobDto>();
        result.addAll(running);
        result.addAll(completed);
        return result;
    }


    /*
     * If from is null it will harvest everything.
     * Format for from is yyyy-MM-dd as this is only one supported by COPs/Cumulus.
     * Will be changed later when more OAI targets comes.
     *  
     */
    public static Integer oaiIngest(OaiTargetJob job, String from) throws Exception {

        //In the OAI spec, the from parameter can be both yyyy-MM-dd or full UTC timestamp (2021-10-09T09:42:03Z)        
        //But COP only supports the short version. So when this is called use short format
        //Dirty but quick solution fix. Best would be if COP could fix it

        OaiTargetDto oaiTargetDto = job.getDto();

        if (from != null && oaiTargetDto.getUrl().indexOf("kb.dk/cop/")> 0) {
            from = from.substring(0,10);               
        }

        String recordBase=oaiTargetDto.getRecordBase();

        DsStorageApi dsAPI = getDsStorageApiClient();        
        OaiHarvestClient client = new OaiHarvestClient(job,from);
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
                    dsAPI.recordPost(dsRecord);
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

    public static synchronized OaiTargetJob createNewJob(OaiTargetDto dto) throws Exception{          
        long id = System.currentTimeMillis();
        Thread.sleep(1); // So next ID is different.
        OaiTargetJob  job = new OaiTargetJob(id, dto);                
        return job;                
    }

    private static DsStorageApi getDsStorageApiClient() {
        ApiClient apiClient = new ApiClient();
        apiClient.setHost(ServiceConfig.getDsHost());
        apiClient.setPort(ServiceConfig.getDsPort());
        apiClient.setBasePath(ServiceConfig.getDsBasePath());
        DsStorageApi dsAPI = new DsStorageApi(apiClient);
        return dsAPI;
    }


}
