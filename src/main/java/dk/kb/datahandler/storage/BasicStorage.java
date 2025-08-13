package dk.kb.datahandler.storage;


import dk.kb.datahandler.config.ServiceConfig;
import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.webservice.exception.ServiceException;
import org.apache.commons.dbcp2.BasicDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BasicStorage implements AutoCloseable {
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

        log.info("DsStorage initialized with driverName = '{}', driverURL = '{}', connectionPoolSize = '{}'", driverName, driverUrl,connectionPoolSize);
    }

    public BasicStorage() throws SQLException {
        connection = dataSource.getConnection();
    }

    public void commit() throws SQLException {
        connection.commit();
    }

    public void rollback() {
        try {
            connection.rollback();
        } catch (Exception e) {
            // nothing to do here
        }
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

    @FunctionalInterface
    public interface StorageAction<T, S extends BasicStorage> {
        T process(S storage) throws Exception;
    }

    public static <T, S extends BasicStorage> T performStorageAction(
            String actionID,
            Callable<S> storageFactory,
            StorageAction<T, S> action
    ) {
        long start = System.currentTimeMillis();
        try (S storage = storageFactory.call()) {
            T result;
            try {
                result = action.process(storage);
            } catch (Exception e) {
                log.warn("Exception performing action '{}'. Initiating rollback", actionID, e);
                storage.rollback();
                throw e;
            }

            try {
                storage.commit();
            } catch (SQLException e) {
                log.error("Exception committing after action '{}'", actionID, e);
                throw new InternalServiceException(e);
            }

            log.debug("Storage method '{}' SQL time in millis: {}", actionID, (System.currentTimeMillis() - start));
            return result;
        } catch (ServiceException e) {
            log.error("Exception performing action '{}'", actionID, e);
            throw e;
        } catch (Exception e) {
            log.error("Exception performing action '{}'", actionID, e);
            throw new InternalServiceException(e);
        }
    }
}
