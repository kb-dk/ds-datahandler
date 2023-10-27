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
        
    public static String callPost(HttpURLConnection conn, InputStream input) throws IOException {
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(30 * 1000);
        conn.setReadTimeout(30 * 1000);
        conn.setUseCaches(false);

        if (input != null) {
            conn.setDoOutput(true);

            OutputStream out = conn.getOutputStream();

            byte[] data = new byte[1024];
            int read = 0;
            while ((read = input.read(data, 0, data.length)) != -1) {
                out.write(data, 0, read);
            }

            input.close();
            out.flush();
            out.close();
        } else {
            conn.connect();
        }

        InputStream is = null;

        try {
            is = conn.getInputStream();
        } catch (IOException e) {
            is = conn.getErrorStream();
        }

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

