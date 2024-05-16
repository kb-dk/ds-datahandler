package dk.kb.datahandler.preservica.jobs;

import dk.kb.datahandler.config.ServiceConfig;
import dk.kb.datahandler.preservica.client.DsPreservicaClient;

/**
 * JobsBase which should be extended with Jobs running against the Preservica Client.
 */
public abstract class JobsBase {

    /**
     * Get Preservica Client for use
     * @return the {@link DsPreservicaClient}.
     */
    public static DsPreservicaClient getPreservicaClient() {
        
        String preservicaUrl = ServiceConfig.getConfig().getString("preservica.baseUrl");
        String user = ServiceConfig.getConfig().getString("preservica.user");
        String password =  ServiceConfig.getConfig().getString("preservica.password");

        DsPreservicaClient client = new DsPreservicaClient(preservicaUrl, user, password, 600);
        return client;
    }
}
