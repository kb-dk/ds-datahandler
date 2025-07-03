package dk.kb.datahandler.storage;

import dk.kb.datahandler.model.v1.JobDto;
import dk.kb.datahandler.model.v1.JobStatusDto;
import dk.kb.datahandler.model.v1.JobTypeDto;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class JobStorage extends BasicStorage {
    private static final String INSERT_JOB_QUERY = """
        INSERT INTO jobs ( 
            id,
            name,
            type,
            createdBy,
            status,
            errorCorrelationId,
            message,
            mTimeFrom,
            startTime,
            numberOfRecords,
            restartValue
        )
        VALUES (
            ?,?,?,?,?,?,?,?,?,?,?
        )
    """;

    private static final String UPDATE_JOB_QUERY = """
        UPDATE jobs SET
            status = ?,
            errorCorrelationId = ?,
            message = ?,
            endTime = ?,
            numberOfRecords = ?,
            restartValue = ? 
        WHERE 
            id = ?
        """;

    private static final String GET_JOB_QUERY = "SELECT * FROM jobs WHERE id=?";

    public JobStorage() throws SQLException {
        super();
    }

    public UUID createJob(JobDto jobDto) throws SQLException{
        UUID id = UUID.randomUUID();
        try (PreparedStatement stmt = connection.prepareStatement(INSERT_JOB_QUERY)) {
            stmt.setObject(1, id);
            stmt.setString(2, jobDto.getJobName());
            stmt.setString(3, jobDto.getJobType().name());
            stmt.setString(4, jobDto.getCreatedBy());
            stmt.setString(5, jobDto.getJobStatus().name());
            stmt.setObject(6, jobDto.getErrorCorrelationId());
            stmt.setString(7, jobDto.getMessage());
            stmt.setInt(8, jobDto.getmTimeFrom());
            stmt.setTimestamp(9, Timestamp.from(jobDto.getStartTime().toInstant()));
            stmt.setObject(10, jobDto.getNumberOfRecords());
            stmt.setObject(11, jobDto.getRestartValue());
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

    public List<JobDto> getJobs(String status, String type) {
        return null;
    }

    public int updateJob(JobDto modifiedJobDto) throws SQLException {
        try(PreparedStatement stmt = connection.prepareStatement(UPDATE_JOB_QUERY)) {
            stmt.setString(1, modifiedJobDto.getJobStatus().name());
            stmt.setObject(2, modifiedJobDto.getErrorCorrelationId());
            stmt.setString(3, modifiedJobDto.getMessage());
            stmt.setTimestamp(4, Timestamp.from(modifiedJobDto.getEndTime().toInstant()));
            stmt.setInt(5, modifiedJobDto.getNumberOfRecords());
            stmt.setInt(6, modifiedJobDto.getRestartValue());
            stmt.setObject(7, modifiedJobDto.getId());
            return stmt.executeUpdate();
        }
    }

    private JobDto createJobDtoFromResult(ResultSet result) throws SQLException {
        JobDto jobDto = new JobDto();
        jobDto.setId(result.getObject("id", UUID.class));
        jobDto.setJobName(result.getString("name"));
        jobDto.setJobType(JobTypeDto.valueOf(result.getString("type")));
        jobDto.setJobStatus(JobStatusDto.valueOf(result.getString("status")));
        jobDto.setCreatedBy(result.getString("createdBy"));
        jobDto.setErrorCorrelationId(result.getObject("errorCorrelationId", UUID.class));
        jobDto.setMessage(result.getString("message"));
        jobDto.setmTimeFrom(result.getInt("mTimeFrom"));
        jobDto.setStartTime(result.getObject("startTime", OffsetDateTime.class));
        jobDto.setEndTime(result.getObject("endTime", OffsetDateTime.class));
        jobDto.setNumberOfRecords(result.getObject("numberOfRecords", Integer.class));
        jobDto.setRestartValue(result.getObject("restartValue", Integer.class));
        return jobDto;
    }
}
