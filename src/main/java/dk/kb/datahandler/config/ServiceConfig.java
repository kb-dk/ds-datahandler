package dk.kb.datahandler.config;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.datahandler.model.v1.OaiTargetDto;
import dk.kb.datahandler.model.v1.OaiTargetDto.DateStampFormatEnum;
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
    /**
     * Url to solr write collection with updateHandler added as URL path. Most likely the url has "/update" appened.
     * To get the URL for the write collection without the updateHanler appended use this: {@link #solrWriteCollectionUrl}.
     */
    private static String solrUpdateUrl = null;
    /**
     * Solr write collection. This contains the URL to the write collection in solr. If an updateHandler gets appended the path should resemble {@link #solrUpdateUrl}.
     */
    private static String solrWriteCollectionUrl = null;
    private static String solrQueryUrl = null;
    private static String dsPresentUrl = null;
    private static int solrBatchSize=100;
    private static String preservicaUrl = null;
    private static String preservicaUser = null;
    private static String preservicaPassword = null;
    private static int preservicaRetryTimes = 5;
    private static int preservicaRetrySeconds = 600;
    private static int preservicaThreads = 5;

    private static int preservicaKeepAliveSeconds = 600;
    private static int oaiRetryTimes = 5;
    private static int oaiRetrySeconds = 600;

    
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
        solrUpdateUrl = createSolrUpdateUrl();
        solrWriteCollectionUrl = serviceConfig.getString("solr.update.url");
        solrQueryUrl = serviceConfig.getString("solr.queryUrl");
        solrBatchSize=  serviceConfig.getInteger("solr.batchSize");
        dsPresentUrl = serviceConfig.getString("present.url");

        preservicaUrl = serviceConfig.getString("preservica.baseUrl");
        preservicaUser = serviceConfig.getString("preservica.user");
        preservicaPassword = serviceConfig.getString("preservica.password");
        preservicaKeepAliveSeconds = serviceConfig.getInteger("preservica.keepAliveSeconds", 600); // Defaulting to 10 minuts. Has to be smaller than retrySeconds.
        preservicaRetrySeconds = serviceConfig.getInteger("preservica.retrySeconds", 900); //Defaulting to 15 minuts.
        preservicaRetryTimes = serviceConfig.getInteger("preservica.retryTimes", 5); // Defaulting to 5 tries.
        preservicaThreads = serviceConfig.getInteger("preservica.threads", 5); // Defaulting to five threads.

        oaiRetryTimes = serviceConfig.getInteger("oaiConfig.retryTimes", 5); // Defaulting to 5 retries
        oaiRetrySeconds = serviceConfig.getInteger("oaiConfig.retrySeconds", 600); // Defaulting to 10 minuts

        log.info("Initialised from config: '{}' with the following values: solrUpdateUrl: '{}', solrQueryUrl: '{}', " +
                "solrBatchSize: '{}', dsStorageUrl: '{}', dsPresentUrl: '{}', oaiRetryTimes: '{}', oaiRetrySeconds: '{}'.",
                configFiles, solrUpdateUrl, solrQueryUrl, solrBatchSize, dsStorageUrl, dsPresentUrl, oaiRetryTimes, oaiRetrySeconds);

        Path folderPath = Paths.get(oaiTimestampFolder);
        if (Files.exists(folderPath)) {            
            log.info("Oai timestamp folder:"+oaiTimestampFolder);
        }
        else {
            log.info("Oai timestamp folder not found:"+oaiTimestampFolder +" .Creating new folder:"+oaiTimestampFolder);
            Files.createDirectories(Paths.get(oaiTimestampFolder));
        }

        if (preservicaRetrySeconds < preservicaKeepAliveSeconds){
            log.error("The preservica client might be kept alive with invalid access token as preservicaRetrySeconds is less than preservicaKeepAliveSeconds");
        }
        
        
    }

    /**
     * Combine configuration solr.update.url and solr.update.requestHandler to a solrUpdateUrl.
     * @return a string representing a URL to a solr request handler for updating the index.
     */
    private static String createSolrUpdateUrl() {
        try {
            return new URIBuilder(serviceConfig.getString("solr.update.url") + serviceConfig.getString("solr.update.requestHandler"))
                    .build().toURL().toString();
        } catch (MalformedURLException | URISyntaxException e) {
            log.warn("Error creating solr update url from URL: '{}' and requestHandler: '{}'",
                    serviceConfig.getString("solr.update.url"), serviceConfig.getString("solr.update.requestHandler") );
            throw new RuntimeException(e);
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

    /**
     * Get URL to solr write collection with updateHandler added as URL path. Most likely the url has "/update" appened.
     * To get the URL for the write collection without the updateHanler appended use this: {@link #solrWriteCollectionUrl}.
     */
    public static String getSolrUpdateUrl() {
        return solrUpdateUrl;
    }

    public static String getSolrWriteCollectionUrl() {
        return solrWriteCollectionUrl;
    }

    public static void setSolrWriteCollectionUrl(String solrWriteCollectionUrl) {
        ServiceConfig.solrWriteCollectionUrl = solrWriteCollectionUrl;
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

    public static int getOaiRetryTimes() {
        return oaiRetryTimes;
    }

    public static int getOaiRetrySeconds() {
        return oaiRetrySeconds;
    }

    public static int getPreservicaRetryTimes() {
        return preservicaRetryTimes;
    }

    public static int getPreservicaRetrySeconds() {
        return preservicaRetrySeconds;
    }

    public static int getPreservicaThreads() {
        return preservicaThreads;
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
            String dateStampFormat = target.getString("dateStampFormat","date");            
            String fragmentServiceUrl = target.getString("fragmentServiceUrl",null);

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
            try {
              oaiTarget.setDateStampFormat(DateStampFormatEnum.fromValue(dateStampFormat));
            }
            catch(Exception e) {
                log.warn("dateStampFormat not 'day' or 'date':"+dateStampFormat);
            }
            oaiTarget.setFragmentServiceUrl(fragmentServiceUrl);
                    
            oaiTargets.put(name, oaiTarget);
            
            log.info("Load OAI target from yaml:"+name);
        }
        
        log.info("Number of OAI targets loaded:"+oaiTargets.size());        
    }

    
}
