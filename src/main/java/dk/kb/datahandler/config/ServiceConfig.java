package dk.kb.datahandler.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.datahandler.model.v1.OaiTargetDto;
import dk.kb.datahandler.model.v1.OaiTargetDto.DateStampFormatEnum;
import dk.kb.datahandler.util.HarvestTimeUtil;
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
    private static String solrUpdateUrl = null;
    private static String solrQueryUrl = null;
    private static String dsPresentUrl = null;
    private static int solrBatchSize=100;
    private static String preservicaUrl = null;
    private static String preservicaUser = null;
    private static String preservicaPassword = null;
    private static int preservicaKeepAliveSeconds = 600;

    
    /**
     * Besides parsing of YAML files using SnakeYAML, the YAML helper class provides convenience
     * methods like {@code getInteger("someKey", defaultValue)} and {@code getSubMap("sub1.sub2")}.
     */
    private static YAML serviceConfig;

    /**
     * Initialized the configuration from the provided configFile.
     * This should normally be called from {@link dk.kb.datahandler.webservice.ContextListener} as
     * part of web server initialization of the container.
     * @param configFiles the configuration to load.
     * @throws IOException if the configuration could not be loaded or parsed.
     */
    public static synchronized void initialize(String... configFiles) throws IOException {
        serviceConfig = YAML.resolveLayeredConfigs(configFiles);
        loadOaiTargets();
        
        oaiTimestampFolder= serviceConfig.getString("timestamps.folder");
        dsStorageUrl = serviceConfig.getString("storage.url");
        solrUpdateUrl = serviceConfig.getString("solr.updateUrl");
        solrQueryUrl = serviceConfig.getString("solr.queryUrl");
        solrBatchSize=  serviceConfig.getInteger("solr.batchSize");
        dsPresentUrl = serviceConfig.getString("present.url");

        preservicaUrl = serviceConfig.getString("preservica.baseUrl");
        preservicaUser = serviceConfig.getString("preservica.user");
        preservicaPassword = serviceConfig.getString("preservica.password");
        preservicaKeepAliveSeconds = serviceConfig.getInteger("preservica.keepAliveSeconds", 840); // Defaulting to 14 minuts

        log.info("Initialised from config: '{}' with the following values: solrUpdateUrl: '{}', solrQueryUrl: '{}', " +
                "solrBatchSize: '{}', dsStorageUrl: '{}', dsPresentUrl: '{}'",
                configFiles, solrUpdateUrl, solrQueryUrl, solrBatchSize, dsStorageUrl, dsPresentUrl);

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
     * @return the backing YAML-handler for the configuration.
     */
    public static YAML getConfig() {
        if (serviceConfig == null) {
            throw new IllegalStateException("The configuration should have been loaded, but was not");
        }
        return serviceConfig;
    }

    public static int getSolrBatchSize() {
    	return solrBatchSize;
    }

    public static String getSolrUpdateUrl() {
        return solrUpdateUrl;
    }
    public static String getSolrQueryUrl() {
        return solrQueryUrl;
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

    public static String getPreservicaUrl() {
        return preservicaUrl;
    }

    public static String getPreservicaUser() {
        return preservicaUser;
    }

    public static String getPreservicaPassword() {
        return preservicaPassword;
    }

    public static int getPreservicaKeepAliveSeconds() {
        return preservicaKeepAliveSeconds;
    }

    private static void loadOaiTargets() {
        List<YAML> targets = serviceConfig.getYAMLList("oaiTargets");
        for (YAML target: targets) {
            String name = target.getString("name");
            String url = target.getString("url");
            String set = target.getString("set",null);
            String datasource = target.getString("datasource");
            String metadataPrefix = target.getString("metadataPrefix");
            String description = target.getString("description");
            String user=target.getString("user",null);
            String password=target.getString("password",null);
            String filterStr = target.getString("filter","direct");
            Boolean dayOnly = target.getBoolean("dayOnly",Boolean.FALSE);
            String startDay = target.getString("startDay",null);            
            String dateStampFormat = target.getString("dateStampFormat","date");
            if (dayOnly) { //startDay must be defined for dayOnly strategy                        	      
                boolean validStartDay=HarvestTimeUtil.validateDayFormat(startDay);
                if (!validStartDay) {
                    throw new IllegalArgumentException("Failed to parse 'start_day' for OAI target with 'day_only' strategy. start_day="+startDay +" OAI target="+name);
                }            	
            }
            
            OaiTargetDto.FilterEnum filter;
            try {
                filter = OaiTargetDto.FilterEnum.fromValue(filterStr);
            } catch (IllegalArgumentException e) {
                log.error("Filter '{}' for target name '{}' not supported. Supported filters are: {}",
                        filterStr, name, Arrays.toString(OaiTargetDto.FilterEnum.values()));
                throw e;
            }

            OaiTargetDto oaiTarget = new OaiTargetDto();
            oaiTarget.setName(name);
            oaiTarget.setUrl(url);       
            oaiTarget.setSet(set);
            oaiTarget.setMetadataprefix(metadataPrefix);
            oaiTarget.setUsername(user);
            oaiTarget.setPassword(password);
            oaiTarget.setDatasource(datasource);
            oaiTarget.setDecription(description);
            oaiTarget.setFilter(filter);
            oaiTarget.setDayOnly(dayOnly);
            oaiTarget.setStartDay(startDay);
            try {
              oaiTarget.setDateStampFormat(DateStampFormatEnum.fromValue(dateStampFormat));
            }
            catch(Exception e) {
                log.warn("dateStampFormat not 'day' or 'date':"+dateStampFormat);
            }
                    
            oaiTargets.put(name, oaiTarget);
            
            log.info("Load OAI target from yaml:"+name);
        }
        
        log.info("Number of OAI targets loaded:"+oaiTargets.size());        
    }

    
}
