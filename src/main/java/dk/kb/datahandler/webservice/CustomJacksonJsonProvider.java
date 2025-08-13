package dk.kb.datahandler.webservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import javax.ws.rs.ext.Provider;

@Provider
public class CustomJacksonJsonProvider extends JacksonJsonProvider {

    /**
     * We add our own JacksonJsonProvider to register Java Time 8
     * And we disable WRITE_DATES_AS_TIMESTAMPS so Jackson serialize Java Time in standard ISO-8601 string representation
     * https://fasterxml.github.io/jackson-modules-java8/javadoc/datetime/2.9/com/fasterxml/jackson/datatype/jsr310/JavaTimeModule.html
     */
    public CustomJacksonJsonProvider() {
        super();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        setMapper(objectMapper);
    }
}
