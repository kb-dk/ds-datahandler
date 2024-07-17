package dk.kb.datahandler.webservice;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.InetAddress;
import java.net.MalformedURLException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import dk.kb.datahandler.config.ServiceConfig;
import dk.kb.datahandler.preservica.client.DsPreservicaClient;
import dk.kb.util.BuildInfoManager;
import dk.kb.util.Files;
import dk.kb.util.Resolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;

/**
 * Listener to handle the various setups and configuration sanity checks that can be carried out at when the
 * context is deployed/initalized.
 */

public class ContextListener implements ServletContextListener {
    private final Logger log = LoggerFactory.getLogger(getClass());

    public static final String LOGBACK_ENV = "java:/comp/env/ds-datahandler-logback-config";
    public static final String CONFIG_ENV = "java:/comp/env/application-config";

    /**
     * On context initialisation this
     * i) Initialises the logging framework (logback).
     * ii) Initialises the configuration class.
     * @param sce context provided by the web server upon initialization.
     * @throws java.lang.RuntimeException if anything at all goes wrong.
     */
    @Override
    public void contextInitialized(ServletContextEvent sce) {
    	// Workaround for logback problem. This should be called before any logging takes place
        initLogging();
        BuildInfoManager.loadBuildInfo("ds-datahandler.build.properties");
    	try {
            RuntimeMXBean mxBean = ManagementFactory.getRuntimeMXBean();
            if (mxBean.getInputArguments().stream().noneMatch(arg -> arg.startsWith("-Xmx"))) {
                log.warn("Xmx is not specified. In stage or production this is almost always an error");
            }

            log.info("Initializing service {} {} build {} using Java {} with max heap {}MB on machine {}. The service " +
                            "has been build from git branch: '{}' with commit checksum: '{}' and newest commit was made " +
                            "at: '{}'. The service has the git tag: '{}' and is closest to the following git tag: '{}'.",
                     BuildInfoManager.getName(), BuildInfoManager.getVersion(), BuildInfoManager.getBuildTime(),
                     System.getProperty("java.version"), Runtime.getRuntime().maxMemory()/1048576,
                    InetAddress.getLocalHost().getHostName(), BuildInfoManager.getGitBranch(),
                    BuildInfoManager.getGitCommitChecksum(), BuildInfoManager.getGitCommitTime(),
                    BuildInfoManager.getGitCurrentTag(), BuildInfoManager.getGitClosestTag());
            InitialContext ctx = new InitialContext();
            String configFile = (String) ctx.lookup("java:/comp/env/application-config");
            //TODO this should not refer to something in template. Should we perhaps use reflection here?
            ServiceConfig.initialize(configFile);
            log.info("Initialized Preservica Client to the following URL: '{}' for the user: '{}'",
                    ServiceConfig.getPreservicaUrl(), ServiceConfig.getPreservicaUser());
        } catch (NamingException e) {
            throw new RuntimeException("Failed to lookup settings", e);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load settings", e);        } 
        log.info("Service initialized.");
    }


    /**
     * For unfathomable reasons, logback 1.4.11 does not support the construction
     * <pre>
     * &lt;configuration scan="true" scanPeriod="5 minutes"&gt;
     *     &lt;insertFromJNDI env-entry-name="java:comp/env/ds-image-logback-config" as="logbackConfiguration"/&gt;
     *     &lt;include file="${logbackConfiguration}"/&gt;
     * &lt;/configuration&gt;    
     * </pre>
     * as the JNDI injection is performed <strong>after</strong> the {@code include}.
     * <p>
     * The workaround is to programatically perform the same environment lookup and reconfigure logback to use
     * the right logback setup file.
     * <p>
     * To complicate matters further, logback require included files to encapsulate the concrete setup in
     * {@code <included>} instead of {@code <configuration>} so in order to stay backwards compatible (and forward
     * compatible as the JNDI-problem is probably solved at some point in the future), a tiny configuration is
     * created at runtime, including the real configuration.
     */
    private void initLogging() {
        try {
            if (Resolver.resolveURL("logback-test.xml") != null) {
                log.info("Logback config 'logback-test.xml' found. Running in test mode");
                return;
            }

        } catch (Exception e) {
            // We might want to skip this logging as it will log to the unconfigured logback at this point
            log.debug("Logback config 'logback-test.xml' not found. Attempting explicit logback configuration");
        }

        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        String logbackFile = null;
        try {
            InitialContext ctx = new InitialContext();

            // Resolve the configured setup for logback
            logbackFile = (String) ctx.lookup(LOGBACK_ENV);

            // Check for logback setup file existence (throws an exception if it fails)
            Resolver.resolveURL(logbackFile);

            // Create a temporary file that includes the real config file (see JavaDoc for this method for details)
            File redirect = createRedirect(logbackFile);

            // https://dennis-xlc.gitbooks.io/the-logback-manual/content/en/chapter-3-configuration/configuration-in-logback/invoking-joranconfigurator-directly.html
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(context);
            context.reset();
            configurator.doConfigure(redirect);

            log.info("Successfully reconfigured logback with '{}'", logbackFile);
        } catch (NamingException e) {
            log.warn("Failed to lookup logback config file from context '{}'. Continuing", LOGBACK_ENV, e);
        } catch (JoranException e) {
            StatusPrinter.printInCaseOfErrorsOrWarnings(context);
            throw new RuntimeException("Failed to configure logger from '" + logbackFile + "'", e);
        } catch (MalformedURLException | FileNotFoundException e) {
            log.warn("Resolved '{}' to path '{}', which does not exist. Continuing", LOGBACK_ENV, logbackFile);
        }
    }

    /**
     * Create a logback redirection file that points to the true logback configuration.
     * See {@link #initLogging()} for details.
     * @param logbackFile the real configuration for logback as a file on the local filesystem.
     * @return a logback config that includes {@code logbackFile}.
     */
    private static File createRedirect(String logbackFile) {
        File redirectFile;
        try {
            redirectFile = File.createTempFile("logback-loader_", ".xml");
        } catch (IOException e) {
            throw new RuntimeException(
                    "Unable to create temporary file for logback configuration of '" + logbackFile + "'", e);
        }
        redirectFile.deleteOnExit();

        try {
            String redirectContent =
                    "<configuration>\n" +
                            "<include file=\"" + logbackFile + "\"/>\n" +
                            "</configuration>\n";
            Files.saveString(redirectContent, redirectFile);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Unable to save redirect logback for '" + logbackFile + "' to temp file '" + redirectFile + "'", e);
        }

        return redirectFile;
    }

    
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        log.debug("Service destroyed");
    }

}
