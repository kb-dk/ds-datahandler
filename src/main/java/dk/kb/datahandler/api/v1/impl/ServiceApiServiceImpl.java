package dk.kb.datahandler.api.v1.impl;

import dk.kb.datahandler.api.v1.ServiceApi;
import dk.kb.datahandler.facade.DsDatahandlerFacade;
import dk.kb.datahandler.model.v1.OaiJobDto;
import dk.kb.datahandler.model.v1.StatusDto;
import dk.kb.util.BuildInfoManager;
import dk.kb.util.webservice.ImplBase;
import dk.kb.util.webservice.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * ds-datahandler
 *
 * <p># Ds-datahandler(Digitale Samlinger) by the Royal Danish Library.   ## Notice OAI-PMH harvest is just the first implementation feature in ds-datahandler. More features will implemented later.  ## OAI harvest  Ds-datahandler can harvest OAI from different OAI-PMH targets and ingest the metadata for the records into ds-storage.  Having the metadata in ds-storage makes access much easier and faster for other applications. The metadata for each record will be UTF-8 encoded before ingested into ds-storage. Invalid XML encoding characters will be replaced or removed.      ## OAI targets configuration The project yaml-file contains the configuration for each OAI-PMH target.  **The below table describes the parameters to configure a OAI-PMH target**      | Property         | Description                                                                                               | | ---------------- | ----------------------------------------------------------------------------------------------------------| | name             | The name used to specify when starting a new import                                                       | | url              | The base url to OAI-PMH service                                                                           | | set              | Parameter to the OAI-server if the server has multiple collections (optional)                             | | metadataPrefix   | Parameter to the OAI-server to specify the format for the metadata. Options depend on the OAI target.     | | user             | User if the OAI-server require basic authentication (optional)                                            | | password         | Password if the OAI-server require basic authentication (optional)                                        |     | recordBase       | The recordbase when sending records from this OAI target to ds-storage.                                   |   |                  | The recordbase must be configured in DS-storage. ID will be {recordbase}:{id in OAI record}               | | description      | Human text to give further description if needed                                                          |   ## Full import and delta import For each OAI target configured you can request a full import or a delta import from the OAI collection. A full import will harvest all records from to OAI target. A delta import will only import records with a datestamp later than the last record recieved in either an earlier full import or delta import.            ## Storing last harvest datestamp  The yaml property file specificed a folder on the filesystem to store the last datestamps for each OAI target. The file will only contain a single with an UTC timestamp identical to the last record successfully send ds-storage from that OAI target.   ## Configure the yaml property file Besides all the OAI targets it must also definere the property for the folder to store the datestamps. Also the properties host,port,baseurl to the ds-storage server to submit the records to.    ## More about the OAI-PMH protocol See: http://www.openarchives.org/OAI/openarchivesprotocol.html 
 *
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
     * Health check + list of running and past jobs
     * 
     * @return <ul>
      *   <li>code = 200, message = "Health check + List of jobs both running and completed since server start. Sorted by most recent jobs first. Still running jobs first", response = StatusDto.class</li>
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
        List<OaiJobDto> jobs = null;
        try {
            jobs = DsDatahandlerFacade.getJobs();
        } catch (Exception e) {
            log.warn("status(): Unable to get jobs", e);
        }
        try {
            return new StatusDto()
                    .application(BuildInfoManager.getName())
                    .version(BuildInfoManager.getVersion())
                    .build(BuildInfoManager.getBuildTime())
                    .java(System.getProperty("java.version"))
                    .heap(Runtime.getRuntime().maxMemory()/1048576L)
                    .server(host)
                    .health("ok")
                    .jobs(jobs);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

}
