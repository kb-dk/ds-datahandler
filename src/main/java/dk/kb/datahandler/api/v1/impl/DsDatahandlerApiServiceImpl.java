package dk.kb.datahandler.api.v1.impl;

import dk.kb.datahandler.api.v1.*;
import dk.kb.datahandler.api.v1.DsDatahandlerApi;
import dk.kb.datahandler.config.ServiceConfig;
import dk.kb.datahandler.model.v1.ErrorDto;
import dk.kb.datahandler.model.v1.OaiJobDto;

import java.io.File;
import dk.kb.datahandler.model.v1.OaiTargetDto;
import dk.kb.datahandler.oai.OaiJobCache;
import dk.kb.datahandler.oai.OaiTargetJob;
import dk.kb.datahandler.webservice.exception.InternalServiceException;
import dk.kb.datahandler.webservice.exception.ServiceException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dk.kb.datahandler.facade.DsDatahandlerFacade;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.io.File;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Providers;
import javax.ws.rs.core.MediaType;
import org.apache.cxf.jaxrs.model.wadl.Description;
import org.apache.cxf.jaxrs.model.wadl.DocTarget;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.multipart.*;

import io.swagger.annotations.Api;

/**
 * ds-datahandler
 *
 * <p>ds-datahandler by the Royal Danish Library 
 *
 */
public class DsDatahandlerApiServiceImpl implements DsDatahandlerApi {
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

        try { 
            int numberIngested= DsDatahandlerFacade.oaiIngestFull(oaiTarget);        
            return numberIngested;

        } catch (Exception e){
            throw handleException(e);
        }

    }

    /**
     * Method to check that service is reachable
     * @return string "pong" if server is reachable
     */
    @Override
    public String ping() {
        return "Pong";
    }


    public Integer oaiIngestDelta(String oaiTarget){
        try { 
            int numberIngested= DsDatahandlerFacade.oaiIngestDelta(oaiTarget);        
            return numberIngested;

        } catch (Exception e){
            throw handleException(e);
        }


    }

    @Override
    public List<OaiTargetDto> getOaiTargetsConfiguration() {        
        try {            

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


    @Override
    public List<OaiJobDto>  getJobsList(){
        try {
            return DsDatahandlerFacade.getJobs();    

        } catch (Exception e){
            throw handleException(e);
        }        
    }

    
    @Override
    public ArrayList<String> importFromZip(String recordBase, Attachment fileNameDetail) {
      
      try {   
         InputStream is = fileNameDetail.getDataHandler().getInputStream();
         return DsDatahandlerFacade.ingestFromZipfile(recordBase,is);    
      }  catch (Exception e){
         throw handleException(e);
       }
    }

    

    /**
     * This method simply converts any Exception into a Service exception
     * @param e: Any kind of exception
     * @return A ServiceException
     * @see dk.kb.datahandler.webservice.ServiceExceptionMapper
     */
    private ServiceException handleException(Exception e) {
        if (e instanceof ServiceException) {
            return (ServiceException) e; // Do nothing - this is a declared ServiceException from within module.
        } else {// Unforseen exception (should not happen). Wrap in internal service exception
            log.error("ServiceException(HTTP 500):", e); //You probably want to log this.
            return new InternalServiceException(e.getMessage());
        }
    }





}
