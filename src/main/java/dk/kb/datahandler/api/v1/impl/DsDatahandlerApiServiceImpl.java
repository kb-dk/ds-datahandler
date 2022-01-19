package dk.kb.datahandler.api.v1.impl;

import dk.kb.datahandler.api.v1.*;
import dk.kb.datahandler.model.v1.BookDto;
import dk.kb.datahandler.model.v1.ErrorDto;
import java.io.File;
import dk.kb.datahandler.model.v1.HelloReplyDto;
import java.util.List;
import java.util.Map;

import dk.kb.datahandler.webservice.exception.ServiceException;
import dk.kb.datahandler.webservice.exception.InternalServiceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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


    /**
     * Add or update a single book
     * 
     * @param BookDto: Add or update a single book
     * 
     * @return <ul>
      *   <li>code = 200, message = "If the book was added successfully", response = BookDto.class</li>
      *   </ul>
      * @throws ServiceException when other http codes should be returned
      *
      * @implNote return will always produce a HTTP 200 code. Throw ServiceException if you need to return other codes
     */
    @Override
    public BookDto addBook(BookDto bookDto) throws ServiceException {
        // TODO: Implement...
    
        
        try { 
            BookDto response = new BookDto();
        response.setId("JGH5Gy");
        response.setTitle("Io9qw");
        response.setPages(591741973);
        return response;
        } catch (Exception e){
            throw handleException(e);
        }
    
    }

    /**
     * Perform some image processing and return the result as an image
     * 
     * @param image: The image to use as source for the colorization
     * 
     * @param method: The algorithm used to colorize the image
     * 
     * @param intensity: The intensity of the colorization
     * 
     * @return <ul>
      *   <li>code = 200, message = "The colorized image", response = String.class</li>
      *   </ul>
      * @throws ServiceException when other http codes should be returned
      *
      * @implNote return will always produce a HTTP 200 code. Throw ServiceException if you need to return other codes
     */
    @Override
    public javax.ws.rs.core.StreamingOutput colorize( Attachment imageDetail, String method, Double intensity) throws ServiceException {
        // TODO: Implement...
    
        
        try { 
            String filename = "somefile";
            // Show download link in Swagger UI, inline when opened directly in browser
            // https://github.com/swagger-api/swagger-ui/issues/3832
            httpServletResponse.setHeader("Content-Disposition", "inline; swaggerDownload=\"attachment\"; filename=\"" + filename + "\"");

            // Show inline in Swagger UI, inline when opened directly in browser
            // httpServletResponse.setHeader("Content-Disposition", "inline; filename=\"" + filename + "\"");

            // Show download link in Swagger UI, download dialog when opened directly in browser
            // httpServletResponse.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

            return output -> output.write("Magic".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e){
            throw handleException(e);
        }
    
    }

    /**
     * Deletes metadata for a single book
     * 
     * @param id: The ID for the book to delete
     * 
     * @return <ul>
      *   <li>code = 200, message = "OK", response = String.class</li>
      *   <li>code = 404, message = "Not found"</li>
      *   </ul>
      * @throws ServiceException when other http codes should be returned
      *
      * @implNote return will always produce a HTTP 200 code. Throw ServiceException if you need to return other codes
     */
    @Override
    public String deleteBook(String id) throws ServiceException {
        // TODO: Implement...
    
        
        try { 
            String response = "Iy174Y";
        return response;
        } catch (Exception e){
            throw handleException(e);
        }
    
    }

    /**
     * Sample OpenAPI definition for a service that constructs a PDF and delivers it
     * 
     * @param id: The ID of the article to process
     * 
     * @return <ul>
      *   <li>code = 200, message = "OK", response = String.class</li>
      *   <li>code = 404, message = "Article ID is unknown", response = ErrorDto.class</li>
      *   </ul>
      * @throws ServiceException when other http codes should be returned
      *
      * @implNote return will always produce a HTTP 200 code. Throw ServiceException if you need to return other codes
     */
    @Override
    public javax.ws.rs.core.StreamingOutput getArticle(String id) throws ServiceException {
        // TODO: Implement...
    
        
        try { 
            String filename = "somefile";
            // Show download link in Swagger UI, inline when opened directly in browser
            // https://github.com/swagger-api/swagger-ui/issues/3832
            httpServletResponse.setHeader("Content-Disposition", "inline; swaggerDownload=\"attachment\"; filename=\"" + filename + "\"");

            // Show inline in Swagger UI, inline when opened directly in browser
            // httpServletResponse.setHeader("Content-Disposition", "inline; filename=\"" + filename + "\"");

            // Show download link in Swagger UI, download dialog when opened directly in browser
            // httpServletResponse.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

            return output -> output.write("Magic".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e){
            throw handleException(e);
        }
    
    }

    /**
     * Retrieves metadata for a single book
     * 
     * @param id: The ID for the book to retrieve
     * 
     * @return <ul>
      *   <li>code = 200, message = "OK", response = BookDto.class</li>
      *   <li>code = 404, message = "Not found"</li>
      *   </ul>
      * @throws ServiceException when other http codes should be returned
      *
      * @implNote return will always produce a HTTP 200 code. Throw ServiceException if you need to return other codes
     */
    @Override
    public BookDto getBook(String id) throws ServiceException {
        // TODO: Implement...
    
        
        try { 
            BookDto response = new BookDto();
        response.setId("mMfo5");
        response.setTitle("cy8v4t4Uus");
        response.setPages(-500666798);
        return response;
        } catch (Exception e){
            throw handleException(e);
        }
    
    }

    /**
     * Delivers metadata on books
     * 
     * @param query: Search query for the books
     * 
     * @param max: The maximum number of books to return
     * 
     * @param format: The delivery format. This can also be specified using headers, as seen in the Responses section. If both headers and format are specified, format takes precedence.  * JSONL: Newline separated single-line JSON representations of Documents * JSON: Valid JSON in the form of a single array of Documents * CSV: Comma separated, missing values represented with nothing, strings encapsulated in quotes 
     * 
     * @return <ul>
      *   <li>code = 200, message = "OK", response = String.class</li>
      *   <li>code = 400, message = "Bad request"</li>
      *   </ul>
      * @throws ServiceException when other http codes should be returned
      *
      * @implNote return will always produce a HTTP 200 code. Throw ServiceException if you need to return other codes
     */
    @Override
    public javax.ws.rs.core.StreamingOutput getBooks(String query, Long max, String format) throws ServiceException {
        // TODO: Implement...
    
        
        try { 
            String filename = "somefile";
            // Show download link in Swagger UI, inline when opened directly in browser
            // https://github.com/swagger-api/swagger-ui/issues/3832
            httpServletResponse.setHeader("Content-Disposition", "inline; swaggerDownload=\"attachment\"; filename=\"" + filename + "\"");

            // Show inline in Swagger UI, inline when opened directly in browser
            // httpServletResponse.setHeader("Content-Disposition", "inline; filename=\"" + filename + "\"");

            // Show download link in Swagger UI, download dialog when opened directly in browser
            // httpServletResponse.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

            return output -> output.write("Magic".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e){
            throw handleException(e);
        }
    
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
     * Ping the server to check if the server is reachable.
     * 
     * @return <ul>
      *   <li>code = 200, message = "OK", response = String.class</li>
      *   <li>code = 406, message = "Not Acceptable", response = ErrorDto.class</li>
      *   <li>code = 500, message = "Internal Error", response = String.class</li>
      *   </ul>
      * @throws ServiceException when other http codes should be returned
      *
      * @implNote return will always produce a HTTP 200 code. Throw ServiceException if you need to return other codes
     */
    @Override
    public String ping() throws ServiceException {
        // TODO: Implement...
    
        
        try { 
            String response = "k74YDV";
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
