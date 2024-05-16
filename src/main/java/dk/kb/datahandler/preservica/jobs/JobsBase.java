package dk.kb.datahandler.preservica.jobs;

import dk.kb.datahandler.config.ServiceConfig;
import dk.kb.datahandler.preservica.client.DsPreservicaClient;

import java.io.IOException;

public abstract class JobsBase {

    public static DsPreservicaClient getPreservicaClient() throws IOException {
        
        String preservicaUrl = ServiceConfig.getConfig().getString("preservica.baseUrl");
        String user = ServiceConfig.getConfig().getString("preservica.user");
        String password =  ServiceConfig.getConfig().getString("preservica.password");

        DsPreservicaClient client = new DsPreservicaClient(preservicaUrl, user, password, 600);
        return client;
    }
}
