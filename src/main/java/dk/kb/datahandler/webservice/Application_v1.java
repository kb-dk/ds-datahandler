package dk.kb.datahandler.webservice;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import dk.kb.datahandler.api.v1.impl.DsDatahandlerApiServiceImpl;
import dk.kb.datahandler.api.v1.impl.ServiceApiServiceImpl;
import dk.kb.datahandler.config.ServiceConfig;
import dk.kb.util.webservice.OpenApiResource;
import dk.kb.util.webservice.exception.ServiceExceptionMapper;


public class Application_v1 extends javax.ws.rs.core.Application {

    @Override
    public Set<Class<?>> getClasses() {
        OpenApiResource.setConfig(ServiceConfig.getConfig());

        return new HashSet<>(Arrays.asList(
                CustomJacksonJsonProvider.class,
                DsDatahandlerApiServiceImpl.class,
                ServiceApiServiceImpl.class,
                ServiceExceptionMapper.class,
                OpenApiResource.class
        ));
    }
}
