package dk.kb.datahandler.util;

import dk.kb.util.Resolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/*
 * When running in Jetty mode, it needs to set up the database. This class can not in test packages or it can not be loaded
 * 
 */
public class H2DbUtil {
    protected static final String CREATE_TABLES_DDL_FILE = "ddl/create_ds_datahandler_storage.ddl";
    
    private static final Logger log = LoggerFactory.getLogger(H2DbUtil.class);
    
    public static void createEmptyH2DBFromDDL(String url, String driver, String username, String password) throws Exception {
        try {
            Class.forName(driver); // load the driver
        } catch (ClassNotFoundException e) {
            throw new SQLException(e);
        }

        try (Connection connection = DriverManager.getConnection(url,username,password)){
            File file = getFile(CREATE_TABLES_DDL_FILE);
            log.info("Running DDL script: {}", file.getAbsolutePath());

            if (!file.exists()) {
                log.error("DDL script not found: {}", file.getAbsolutePath());
                throw new RuntimeException("DDL Script file not found:" + file.getAbsolutePath());
            }

            connection.createStatement().execute("RUNSCRIPT FROM '" + file.getAbsolutePath() + "'");
            connection.createStatement().execute("SHUTDOWN");
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    //Use KB-util to resolve file. 
    protected static File getFile(String resource) {
       return Resolver.getPathFromClasspath(resource).toFile(); 
    }


}
