package dk.kb.datahandler.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *  Util class for doing POST request with an InputStream so it is not loaded into memory
 *  Can it be done easier with apacheIO ?  
 */

public class HttpPostUtil {
    private static final Logger log = LoggerFactory.getLogger(HttpPostUtil .class);
    
    /**
     * Takes an InputStream and feeds it to a HTTP post request
     * 
     * 
     * @param conn Connected data will be posted to
     * @param input Stream having json data
     * @param contentType Example :  'application/json'
     * @return Response string from server.
     * @throws IOException
     */      
    public static String callPost(HttpURLConnection conn, InputStream input, String contentType) throws IOException {
        conn.setRequestProperty("Content-Type", contentType);
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(30 * 1000); //30 secs
        conn.setReadTimeout(60 * 1000); // 10 minutes 600 secs. Maybe large load needs to be delivered.
        conn.setUseCaches(false);
        conn.setDoOutput(true);
        
        try (input ; OutputStream out = conn.getOutputStream()){
            long copiedBytes = IOUtils.copyLarge(input, out);
             log.debug("Stream bytes read:"+copiedBytes);
            out.flush();           
        }
       
        try (InputStream is = conn.getInputStream()) {
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Exception posting to " + conn.getURL(), e);
        }
        try (InputStream err = conn.getErrorStream()) {
            return IOUtils.toString(err, StandardCharsets.UTF_8);
        }
        
          
    }
}

