package dk.kb.datahandler.webservice;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import dk.kb.datahandler.api.v1.impl.DsDatahandlerApiServiceImpl;


public class Application_v1 extends javax.ws.rs.core.Application {

    @Override
    public Set<Class<?>> getClasses() {
        return new HashSet<>(Arrays.asList(
                JacksonJsonProvider.class,
                DsDatahandlerApiServiceImpl.class,
                ServiceExceptionMapper.class
        ));
    }


}
