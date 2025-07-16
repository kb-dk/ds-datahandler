package dk.kb.datahandler.webservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import javax.ws.rs.ext.Provider;

@Provider
public class CustomJacksonJsonProvider extends JacksonJsonProvider {

    public CustomJacksonJsonProvider() {
        super();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        setMapper(objectMapper);
    }
}
