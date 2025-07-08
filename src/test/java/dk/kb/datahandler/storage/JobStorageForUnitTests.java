package dk.kb.datahandler.storage;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class JobStorageForUnitTests extends JobStorage {

    @Override
    public void clearTables() throws SQLException {
        try(PreparedStatement stmt = connection.prepareStatement("DELETE FROM JOBS")) {
            stmt.executeUpdate();
            commit();
        }
    }

    public JobStorageForUnitTests() throws SQLException {
        super();
    }
}
