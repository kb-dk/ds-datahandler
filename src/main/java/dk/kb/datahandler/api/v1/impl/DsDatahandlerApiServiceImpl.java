package dk.kb.datahandler.api.v1.impl;

import dk.kb.datahandler.api.v1.DsDatahandlerApi;
import dk.kb.datahandler.config.ServiceConfig;
import dk.kb.datahandler.facade.DsDatahandlerFacade;
import dk.kb.datahandler.model.v1.IndexTypeDto;
import dk.kb.datahandler.model.v1.OaiTargetDto;
import dk.kb.util.webservice.ImplBase;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * ds-datahandler
 *
 * <p>ds-datahandler by the Royal Danish Library 
 *
 */
public class DsDatahandlerApiServiceImpl extends ImplBase implements DsDatahandlerApi {
    private Logger log = LoggerFactory.getLogger(this.toString());


    /* How to access the various web contexts. See https://cxf.apache.org/docs/jax-rs-basics.html#JAX-RSBasics-Contextannotations */

    @Context
    private transient UriInfo uriInfo;

    @Context
    private transient SecurityContext securityContext;

    @Context
    private transient HttpHeaders httpHeaders;

    @Context
    private transient Providers providers;

    @Context
    private transient Request request;

    // Disabled as it is always null? TODO: Investigate when it can be not-null, then re-enable with type
    //@Context
    //private transient ContextResolver contextResolver;

    @Context
    private transient HttpServletRequest httpServletRequest;

    @Context
    private transient HttpServletResponse httpServletResponse;

    @Context
    private transient ServletContext servletContext;

    @Context
    private transient ServletConfig servletConfig;

    @Context
    private transient MessageContext messageContext;


    @Override
    public Integer oaiIngestFull(String oaiTarget){
        log.debug("oaiIngestFull(oaiTarget='{}') called with call details: {}", oaiTarget, getCallDetails());
        try {
            int numberIngested= DsDatahandlerFacade.oaiIngestFull(oaiTarget);
            return numberIngested;

        } catch (Exception e){
            throw handleException(e);
        }

    }
    
    @Override
    public Integer oaiIngestDelta(String oaiTarget){
        log.debug("oaiIngestDelta(oaiTarget='{}') called with call details: {}", oaiTarget, getCallDetails());
        try {
            int numberIngested = DsDatahandlerFacade.oaiIngestDelta(oaiTarget);
            return numberIngested;

        } catch (Exception e){
            throw handleException(e);
        }


    }

    @Override
    public List<OaiTargetDto> getOaiTargetsConfiguration() {
        try {            
            log.debug("getOaiTargetsConfiguration() called with call details: {}", getCallDetails());
            //return as list. Internal they are saved in HashMap since they are retrieved by name when harvesting.
            List<OaiTargetDto> targets = new ArrayList<OaiTargetDto>();
            HashMap<String, OaiTargetDto> oaiTargets = ServiceConfig.getOaiTargets();            
            for (String target : oaiTargets.keySet()) {
                targets.add(oaiTargets.get(target));                               
            }

            return  targets;
        } catch (Exception e){
            throw handleException(e);
        }
    }

    /**
     * Update manifestations for records in an origin. This endpoint is used with origins originating from Preservica 7.
     * @param origin to update records in.
     * @param mTimeFrom time to update records from.
     * @return the amount of records that have been updated.
     */
    @Override
    public Long updatePreservicaManifestation(String origin, Long mTimeFrom) {
        try {
            return DsDatahandlerFacade.updateManifestationForRecords(origin, mTimeFrom);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ArrayList<String> importFromZip(String origin, Attachment fileNameDetail) {
        log.debug("importFromZip(origin='{}', ...) called with call details: {}", origin, getCallDetails());
        try {
            InputStream is = fileNameDetail.getDataHandler().getInputStream();
            return DsDatahandlerFacade.ingestFromZipfile(origin,is);
        }  catch (Exception e){
            throw handleException(e);
        }
    }

    @Override
    public String indexSolr(@NotNull String origin, Long mTimeFrom, IndexTypeDto type) {
        log.debug("indexSolr(origin='{}', ...) called with call details: {}", origin, getCallDetails());
        try {
            switch (type){
                case FULL:
                    return DsDatahandlerFacade.indexSolrFull(origin,mTimeFrom);
                case DELTA:
                    return DsDatahandlerFacade.indexSolrDelta(origin);
                default:
                    log.error("No indexing type has been selected. Indexing cannot continue without knowing which records to index.");
                    return "No indexing type has been selected. Indexing cannot continue without knowing which records to index.";
            }

        }  catch (Exception e){
            throw handleException(e);
        }
    }


}
