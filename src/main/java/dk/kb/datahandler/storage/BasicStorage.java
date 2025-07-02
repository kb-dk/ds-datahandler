package dk.kb.datahandler.storage;


import dk.kb.datahandler.config.ServiceConfig;
import org.apache.commons.dbcp2.BasicDataSource;

import java.sql.Connection;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicStorage implements AutoCloseable {
    private static Logger log = LoggerFactory.getLogger(BasicStorage.class);

    protected Connection connection;
    private static BasicDataSource dataSource;

    public static void initialize(String driverName, String driverUrl, String userName, String password) {

        int connectionPoolSize = ServiceConfig.getConnectionPoolSize();

        dataSource = new BasicDataSource();
        dataSource.setDriverClassName(driverName);
        dataSource.setUsername(userName);
        dataSource.setPassword(password);
        dataSource.setUrl(driverUrl);
        dataSource.setDefaultReadOnly(false);
        dataSource.setDefaultAutoCommit(false);
        dataSource.setMaxOpenPreparedStatements(connectionPoolSize);

        log.info("DsStorage initialized with driverName='{}', driverURL='{}', connectionPoolSize='{}' ", driverName, driverUrl,connectionPoolSize);
    }

    public BasicStorage() throws SQLException {
        connection = dataSource.getConnection();
    }

    @Override
    public void close() {
        // Make sure connection is closed
        try {
            connection.close();
        } catch (Exception e) {
            //Nothing to do
        }
    }
}
