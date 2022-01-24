package dk.kb.datahandler.api.v1.impl;

import dk.kb.datahandler.api.v1.*;

import dk.kb.datahandler.model.v1.ErrorDto;
import java.io.File;
import dk.kb.datahandler.model.v1.HelloReplyDto;
import dk.kb.datahandler.model.v1.OaiTargetDto;
import dk.kb.datahandler.webservice.exception.InternalServiceException;
import dk.kb.datahandler.webservice.exception.ServiceException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


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
        // TODO Auto-generated method stub
        
        try { 
            int numberIngested= DsDatahandlerFacade.oaiIngestFull(oaiTarget);        
            return numberIngested;
        
        } catch (Exception e){
            throw handleException(e);
        }
        
        
        
        
    }

    @Override
    public List<OaiTargetDto> getOaiTargetsConfiguration() {
        
        //TODO configure in YAML
        
        List<OaiTargetDto> oaiTargetList = new ArrayList<OaiTargetDto>();
       
        OaiTargetDto billedeSamling = new OaiTargetDto();
        billedeSamling.setName("Billede samling");
        billedeSamling.setUrl("http://www5.kb.dk/cop/oai/?metadataPrefix=mods&set=oai:kb.dk:images:billed:2010:okt:billeder");
        oaiTargetList.add(billedeSamling);

        return oaiTargetList;
    }
   

    /**
     * Request a Hello World message, for testing purposes
     * 
     * @param alternateHello: Optional alternative to using the word &#39;Hello&#39; in the reply
     * 
     * @return <ul>
      *   <li>code = 200, message = "A JSON structure containing a Hello World message", response = HelloReplyDto.class</li>
      *   </ul>
      * @throws ServiceException when other http codes should be returned
      *
      * @implNote return will always produce a HTTP 200 code. Throw ServiceException if you need to return other codes
     */
    @Override
    public HelloReplyDto getGreeting(String alternateHello) throws ServiceException {
        // TODO: Implement...
    
        
        try { 
            HelloReplyDto response = new HelloReplyDto();
        response.setMessage("f8Her5li");
        return response;
        } catch (Exception e){
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
