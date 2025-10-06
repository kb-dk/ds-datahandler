package dk.kb.datahandler.webservice;

import dk.kb.datahandler.api.v1.impl.DsDatahandlerApiServiceImpl;
import dk.kb.datahandler.api.v1.impl.ServiceApiServiceImpl;
import dk.kb.datahandler.config.ServiceConfig;
import dk.kb.shared.webservice.CustomJacksonJsonProvider;
import dk.kb.util.webservice.OpenApiResource;
import dk.kb.util.webservice.exception.ServiceExceptionMapper;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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
