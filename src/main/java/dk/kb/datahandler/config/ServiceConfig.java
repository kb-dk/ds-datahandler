package dk.kb.datahandler.config;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.datahandler.model.v1.OaiTargetDto;
import dk.kb.util.yaml.YAML;

/**
 * Sample configuration class using the Singleton pattern.
 * This should work well for most projects with non-dynamic properties.
 */
public class ServiceConfig {

    private static final Logger log = LoggerFactory.getLogger(ServiceConfig.class);
    
    private static final HashMap<String, OaiTargetDto> oaiTargets = new HashMap<String, OaiTargetDto>();
   
    /**
     * Besides parsing of YAML files using SnakeYAML, the YAML helper class provides convenience
     * methods like {@code getInteger("someKey", defaultValue)} and {@code getSubMap("config.sub1.sub2")}.
     */
    private static YAML serviceConfig;

    /**
     * Initialized the configuration from the provided configFile.
     * This should normally be called from {@link dk.kb.datahandler.webservice.ContextListener} as
     * part of web server initialization of the container.
     * @param configFile the configuration to load.
     * @throws IOException if the configuration could not be loaded or parsed.
     */
    public static synchronized void initialize(String configFile) throws IOException {
        serviceConfig = YAML.resolveLayeredConfigs(configFile);
        loadOaiTargets();
    }

  
    /**
     * Direct access to the backing YAML-class is used for configurations with more flexible content
     * and/or if the service developer prefers key-based property access.
     * @see #getHelloLines() for alternative.
     * @return the backing YAML-handler for the configuration.
     */
    public static YAML getConfig() {
        if (serviceConfig == null) {
            throw new IllegalStateException("The configuration should have been loaded, but was not");
        }
        return serviceConfig;
    }

    public static HashMap<String, OaiTargetDto> getOaiTargets() {
        return oaiTargets;
    }

    
    private static void loadOaiTargets() {
        List<YAML> targets = serviceConfig.getYAMLList("config.oai_targets");
        //Load updtateStategy for each
        for (YAML target: targets) {
            String name = target.getString("name");
            String url = target.getString("url");
            String set = target.getString("set");
            String description = target.getString("description");
                           
            OaiTargetDto oaiTarget = new OaiTargetDto();
            oaiTarget.setName(name);
            oaiTarget.setUrl(url);
            oaiTarget.set(set);
            oaiTarget.setDecription(description);            
            oaiTargets.put(name, oaiTarget);
            
            log.info("Load OAI target from yaml:"+description);
        }
        
        //log.info("Allowed bases loaded from config. Number of bases:"+allowedBases.size());
        
    }

    
    
}
