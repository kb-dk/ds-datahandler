package dk.kb.datahandler.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private static String oaiTimestampFolder=null;
    private static String dsStorageUrl = null;
    private static String solrUrl = null;
    private static String dsPresentUrl = null;
    
    
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
        
        oaiTimestampFolder= serviceConfig.getString("config.timestamps.folder");
        dsStorageUrl = serviceConfig.getString("config.storage.url");
        solrUrl = serviceConfig.getString("config.solr.url");
        dsPresentUrl = serviceConfig.getString("config.present.url");
        
        Path folderPath = Paths.get(oaiTimestampFolder);
        if (Files.exists(folderPath)) {            
            log.info("Oai timestamp folder:"+oaiTimestampFolder);
        }
        else {
            log.info("Oai timestamp folder not found:"+oaiTimestampFolder +" .Creating new folder:"+oaiTimestampFolder);
            Files.createDirectories(Paths.get(oaiTimestampFolder));
        }
        
        
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


    public static String getSolrUrl() {
        return solrUrl;
    }
    
    public static String getDsPresentUrl() {
        return dsPresentUrl;
    }

    public static String getDsStorageUrl() {
        return dsStorageUrl;
    }

    public static String getOaiTimestampFolder() {
        return oaiTimestampFolder;
    }
    
    public static HashMap<String, OaiTargetDto> getOaiTargets() {
        return oaiTargets;
    }

    
    private static void loadOaiTargets() {
        List<YAML> targets = serviceConfig.getYAMLList("config.oai_targets");
        for (YAML target: targets) {
            String name = target.getString("name");
            String url = target.getString("url");
            String set = target.getString("set",null);
            String origin = target.getString("origin");
            String metadataPrefix = target.getString("metadataPrefix");
            String description = target.getString("description");
            String user=target.getString("user",null);
            String password=target.getString("password",null);
                        
            OaiTargetDto oaiTarget = new OaiTargetDto();
            oaiTarget.setName(name);
            oaiTarget.setUrl(url);       
            oaiTarget.setSet(set);
            oaiTarget.setMetadataprefix(metadataPrefix);
            oaiTarget.setUsername(user);
            oaiTarget.setPassword(password);
            oaiTarget.setOrigin(origin);
            oaiTarget.setDecription(description);            
            oaiTargets.put(name, oaiTarget);
            
            log.info("Load OAI target from yaml:"+name);
        }
        
        log.info("Number of OAI targets loaded:"+oaiTargets.size());        
    }

    
}
