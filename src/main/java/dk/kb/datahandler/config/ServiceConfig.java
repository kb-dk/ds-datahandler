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

    private static String kalturaUrl=null;
    private static Integer kalturaPartnerId =null;
    private static String kalturaUserId = null;
    private static String kalturaToken = null;
    private static String kalturaTokenId = null;
    private static String kalturaAdminSecret = null;
    private static int kalturaFlavourParamIdVideo=0;
    private static int kalturaFlavourParamIdAudio=0;
    
    private static String streamPathDomsRadioTv=null;
    private static String streamPathPreservicaTv=null;
    private static String streamPathPreservicaRadio=null;
    
    
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

        kalturaUrl = ServiceConfig.getConfig().getString("kaltura.url");
        kalturaPartnerId = ServiceConfig.getConfig().getInteger("kaltura.partnerId");
        kalturaUserId = ServiceConfig.getConfig().getString("kaltura.userId");
        kalturaToken = ServiceConfig.getConfig().getString("kaltura.token");
        kalturaTokenId = ServiceConfig.getConfig().getString("kaltura.tokenId");
        //Do not use kaltura adminsecret, use token and tokenId instead.
        kalturaAdminSecret= ServiceConfig.getConfig().getString("kaltura.adminSecret"); //Must not be shared or exposed. Use token,tokenId.
        kalturaFlavourParamIdVideo = ServiceConfig.getConfig().getInteger("kaltura.flavourParamIdVideo");
        kalturaFlavourParamIdAudio = ServiceConfig.getConfig().getInteger("kaltura.flavourParamIdAudio");
        
        streamPathDomsRadioTv=ServiceConfig.getConfig().getString("streams.domsRadioTvPath");   
        streamPathPreservicaTv=ServiceConfig.getConfig().getString("streams.preservicaTvPath");
        streamPathPreservicaRadio=ServiceConfig.getConfig().getString("streams.preservicaRadioPath");   
                 
        oaiRetryTimes = serviceConfig.getInteger("oaiSettings.retryTimes", 5); // Defaulting to 5 retries
        oaiRetrySeconds = serviceConfig.getInteger("oaiSettings.retrySeconds", 600); // Defaulting to 10 minuts

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

    public static String getKalturaUrl() {
        return kalturaUrl;
    }

    public static Integer getKalturaPartnerId() {
        return kalturaPartnerId;
    }

    public static String getKalturaUserId() {
        return kalturaUserId;
    }

    public static String getKalturaToken() {
        return kalturaToken;
    }

    public static String getKalturaTokenId() {
        return kalturaTokenId;
    }
   
    
    public static int getKalturaFlavourParamIdVideo() {
        return kalturaFlavourParamIdVideo;
    }
  
    public static int getKalturaFlavourParamIdAudio() {
        return kalturaFlavourParamIdAudio;
    }

    public static YAML getServiceConfig() {
        return serviceConfig;
    }

    public static String getKalturaAdminSecret() {
        return kalturaAdminSecret;
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

    public static String getStreamPathDomsRadioTv() {
        return streamPathDomsRadioTv;
    }

    public static String getStreamPathPreservicaTv() {
        return streamPathPreservicaTv;
    }

    public static String getStreamPathPreservicaRadio() {
        return streamPathPreservicaRadio;
    }

    public static  String getDBDriver() {
        String dbDriver= serviceConfig.getString("db.driver");
        return dbDriver;
    }

    public static  String getDBUrl() {
        String dbUrl= serviceConfig.getString("db.url");
        return dbUrl;
    }

    public static  String getDBUserName() {
        String dbUserName= serviceConfig.getString("db.username");
        return dbUserName;
    }

    public static  String getDBPassword() {
        String dbPassword= serviceConfig.getString("db.password");
        return dbPassword;
    }

    public static int getConnectionPoolSize() {
        int connectionPoolSize= serviceConfig.getInteger("db.connectionPoolSize",10); //Default 10
        return connectionPoolSize;
    }
    
}
