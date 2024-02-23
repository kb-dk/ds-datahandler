package dk.kb.datahandler.api.v1.impl;

import dk.kb.datahandler.api.v1.ServiceApi;
import dk.kb.datahandler.facade.DsDatahandlerFacade;
import dk.kb.datahandler.model.v1.OaiJobDto;
import dk.kb.datahandler.model.v1.StatusDto;
import dk.kb.util.BuildInfoManager;
import dk.kb.util.webservice.ImplBase;
import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.webservice.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

/**
 * Meta endpoints.
 */
public class ServiceApiServiceImpl extends ImplBase implements ServiceApi {
    private final Logger log = LoggerFactory.getLogger(this.toString());



    /**
     * Ping the server to check if the server is reachable.
     * 
     * @return <ul>
      *   <li>code = 200, message = "OK", response = String.class</li>
      *   </ul>
      * @throws ServiceException when other http codes should be returned
      *
      * @implNote return will always produce a HTTP 200 code. Throw ServiceException if you need to return other codes
     */
    @Override
    public String ping() throws ServiceException {
        log.debug("ping() called with call details: {}", getCallDetails());
        return "Pong";
    }

    /**
     * Health check
     * 
     * @return <ul>
      *   <li>code = 200, message = "Health check", response = StatusDto.class</li>
      *   <li>code = 500, message = "Internal Error", response = String.class</li>
      *   </ul>
      * @throws ServiceException when other http codes should be returned
      *
      * @implNote return will always produce a HTTP 200 code. Throw ServiceException if you need to return other codes
     */
    @Override
    public StatusDto status() throws ServiceException {
        log.debug("status() called with call details: {}", getCallDetails());
        String host = "N/A";
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            log.warn("Exception resolving hostname", e);
        }
        try {
            return new StatusDto()
                    .application(BuildInfoManager.getName())
                    .version(BuildInfoManager.getVersion())
                    .build(BuildInfoManager.getBuildTime())
                    .java(System.getProperty("java.version"))
                    .heap(Runtime.getRuntime().maxMemory()/1048576L)
                    .server(host)
                    .gitCommitChecksum(BuildInfoManager.getGitCommitChecksum())
                    .gitBranch(BuildInfoManager.getGitBranch())
                    .gitClosestTag(BuildInfoManager.getGitClosestTag())
                    .gitCurrentTag(BuildInfoManager.getGitCurrentTag())
                    .gitCommitTime(BuildInfoManager.getGitCommitTime())
                    .health("ok");
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    /**
     * List of jobs both running and completed since server start. Sorted by most recent jobs first. Still running jobs first. <br>
     * 
     * @return List running and completed OAI jobs. 
     * @throws ServiceException If jobs could not be loaded. Should not happen.
     * 
    */
    @Override
    public List<OaiJobDto> jobs() throws ServiceException {
        
        List<OaiJobDto> jobs = null;
        try {
            jobs = DsDatahandlerFacade.getJobs();
        } catch (Exception e) {
            log.warn("status(): Unable to get jobs", e);
            throw new InternalServiceException("Unable to load jobs",e);
        }      
        return jobs;        
    }
    
}
