package dk.kb.datahandler.storage;

import dk.kb.datahandler.model.v1.CategoryDto;
import dk.kb.datahandler.model.v1.JobDto;
import dk.kb.datahandler.model.v1.JobStatusDto;
import dk.kb.datahandler.model.v1.TypeDto;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class JobStorage extends BasicStorage {
    private static final String INSERT_JOB_QUERY = """
        INSERT INTO jobs ( 
            id,
            type,
            category,
            source,
            created_by,
            status,
            modified_time_from,
            start_time
        )
        VALUES (
            ?,?,?,?,?,?,?,?
        )
    """;

    private static final String UPDATE_JOB_QUERY = """
        UPDATE jobs SET
            status = ?,
            error_correlation_id = ?,
            message = ?,
            end_time = ?,
            number_of_records = ?,
            restart_value = ? 
        WHERE 
            id = ?
    """;

    private static final String GET_JOBS_BY_CATEGORY_AND_SOURCE_AND_STATUS = """
        SELECT
            id
        FROM
            jobs
        WHERE
            category = ?
          AND
            source = ?
          AND
            status = ?
    """;

    private static final String GET_JOB_QUERY = "SELECT * FROM jobs WHERE id=?";

    public JobStorage() throws SQLException {
        super();
    }

    public UUID createJob(JobDto jobDto) throws SQLException{
        UUID id = UUID.randomUUID();
        try (PreparedStatement stmt = connection.prepareStatement(INSERT_JOB_QUERY)) {
            stmt.setObject(1, id);
            stmt.setString(2, jobDto.getType().name());
            stmt.setString(3, jobDto.getCategory().name());
            stmt.setString(4, jobDto.getSource());
            stmt.setString(5, jobDto.getCreatedBy());
            stmt.setString(6, jobDto.getJobStatus().name());
            stmt.setObject(7, jobDto.getModifiedTimeFrom());
            stmt.setObject(8, jobDto.getStartTime());
            stmt.executeUpdate();
            return id;
        }
    }

    public JobDto getJob(UUID id) throws SQLException {
        try(PreparedStatement stmt = connection.prepareStatement(GET_JOB_QUERY)) {
            stmt.setString(1, id.toString());
            try (ResultSet result = stmt.executeQuery()) {
                if (!result.next()) {
                    return null;
                }
                JobDto jobDto = createJobDtoFromResult(result);
                return jobDto;
            }
        }
    }

    public boolean hasRunningJob(CategoryDto categoryDto, String source) throws SQLException {

        ResultSet res1 = connection.prepareStatement("SELECT * from jobs").executeQuery();
        try(PreparedStatement stmt = connection.prepareStatement(GET_JOBS_BY_CATEGORY_AND_SOURCE_AND_STATUS)) {
            stmt.setString(1, categoryDto.name());
            stmt.setString(2, source);
            stmt.setString(3, JobStatusDto.RUNNING.name());
            try (ResultSet result = stmt.executeQuery()) {
                if (!result.next()) {
                    return false;
                }
                return true;
            }
        }
    }

    public List<JobDto> getJobs(String status, String type) {
        return null;
    }

    public int updateJob(JobDto modifiedJobDto) throws SQLException {
        try(PreparedStatement stmt = connection.prepareStatement(UPDATE_JOB_QUERY)) {
            stmt.setString(1, modifiedJobDto.getJobStatus().name());
            stmt.setObject(2, modifiedJobDto.getErrorCorrelationId());
            stmt.setString(3, modifiedJobDto.getMessage());
            stmt.setObject(4, modifiedJobDto.getEndTime());
            stmt.setInt(5, modifiedJobDto.getNumberOfRecords());
            stmt.setInt(6, modifiedJobDto.getRestartValue());
            stmt.setObject(7, modifiedJobDto.getId());
            return stmt.executeUpdate();
        }
    }

    private JobDto createJobDtoFromResult(ResultSet result) throws SQLException {
        JobDto jobDto = new JobDto();
        jobDto.setId(result.getObject("id", UUID.class));
        jobDto.setType(TypeDto.valueOf(result.getString("type")));
        jobDto.setCategory(CategoryDto.valueOf(result.getString("category")));
        jobDto.setSource(result.getString("source"));
        jobDto.setJobStatus(JobStatusDto.valueOf(result.getString("status")));
        jobDto.setCreatedBy(result.getString("created_by"));
        jobDto.setErrorCorrelationId(result.getObject("error_correlation_id", UUID.class));
        jobDto.setMessage(result.getString("message"));
        jobDto.setModifiedTimeFrom(result.getObject("modified_time_from", Instant.class));
        jobDto.setStartTime(result.getObject("start_time", Instant.class));
        jobDto.setEndTime(result.getObject("end_time", Instant.class));
        jobDto.setNumberOfRecords(result.getObject("number_of_records", Integer.class));
        jobDto.setRestartValue(result.getObject("restart_value", Integer.class));
        return jobDto;
    }
}
