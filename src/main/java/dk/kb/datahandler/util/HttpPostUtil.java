package dk.kb.datahandler.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;

/**
 *  Util class for doing POST request with an InputStream so it is not loaded into memory
 *  Can it be done easier with apacheIO ?  
 */

public class HttpPostUtil {
        
    
    /**
     * Takes an InputStream and feeds it to a HTTP post request
     * 
     * 
     * @param conn Connected data will be posted to
     * @param input Stream having json data
     * @param contentType. Example :  'application/json'
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
            
            byte[] data = new byte[1024];
            int read = 0;
            while ((read = input.read(data, 0, data.length)) != -1) {
                out.write(data, 0, read);
            }

            input.close();
            out.flush();
            out.close();
        }
        
        try (InputStream is = conn.getInputStream()){              
          StringBuilder buf = new StringBuilder();
          BufferedReader in = new BufferedReader(new InputStreamReader(is,"UTF-8"));
          String inputLine;
          while ((inputLine = in.readLine()) != null) {
            buf.append(inputLine);
          }
          in.close();

          return buf.toString();
        }
    }
}

