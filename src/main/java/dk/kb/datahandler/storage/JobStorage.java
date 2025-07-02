package dk.kb.datahandler.storage;

import dk.kb.datahandler.model.v1.JobDto;
import dk.kb.datahandler.model.v1.JobStatusDto;
import dk.kb.datahandler.model.v1.JobTypeDto;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

public class JobStorage extends BasicStorage {
    private static final String INSERT_JOB_QUERY = """
            INSERT INTO jobs ( 
             id, name, type, createdBy,
             status, errorCorrelationId, message,
             mTimeFrom, startTime, endTime, numberOfRecords,
             restartValue
            )
            VALUES
            (?,?,?,?,?,?,?,?,?,?,?,?)
            """;
    private static final String GET_JOB_QUERY = "SELECT * FROM jobs WHERE id=?;";

    public JobStorage() throws SQLException {
        super();
    }

    public UUID createJob(JobDto job) throws SQLException{
        UUID id = UUID.randomUUID();
        try(PreparedStatement stmt = connection.prepareStatement(INSERT_JOB_QUERY)) {
            stmt.setObject(1,id);
            stmt.setString(2,job.getJobName());
            stmt.setString(3,job.getJobType().name());
            stmt.setString(4,job.getCreatedBy());
            stmt.setString(5,job.getJobStatus().name());
            stmt.setObject(6,job.getErrorCorrelationId());
            stmt.setString(7,job.getMessage());
            stmt.setInt(8,job.getmTimeFrom());
            stmt.setTimestamp(9,new Timestamp(job.getStartTime().getTime()));
            stmt.setTimestamp(10,new Timestamp(job.getEndTime().getTime()));
            stmt.setInt(11,job.getNumberOfRecords());
            stmt.setInt(12,job.getRestartValue());
            stmt.executeUpdate();
            return id;
        }
    };

    public void updateJobStatus(String id, String status) {

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

    private JobDto createJobDtoFromResult(ResultSet result) throws SQLException {
        JobDto jobDto = new JobDto();
        jobDto.setId(result.getObject("id",UUID.class));
        jobDto.setJobName(result.getString("name"));
        jobDto.setJobType(JobTypeDto.valueOf(result.getString("type")));
        jobDto.setJobStatus(JobStatusDto.valueOf(result.getString("status")));
        jobDto.setCreatedBy(result.getString("createdBy"));
        jobDto.setErrorCorrelationId(result.getObject("errorCorrelationId",UUID.class));
        jobDto.setMessage(result.getString("message"));
        jobDto.setmTimeFrom(result.getInt("mTimeFrom"));
        jobDto.setStartTime(result.getDate("startTime"));
        jobDto.setEndTime(result.getDate("endTime"));
        jobDto.setNumberOfRecords(result.getInt("numberOfRecords"));
        jobDto.setRestartValue(result.getInt("restartValue"));
        return jobDto;
    }


}
