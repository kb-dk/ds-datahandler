package dk.kb.datahandler.storage;

import dk.kb.datahandler.model.v1.CategoryDto;
import dk.kb.datahandler.model.v1.JobDto;
import dk.kb.datahandler.model.v1.JobStatusDto;
import dk.kb.datahandler.model.v1.TypeDto;

import java.sql.*;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
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

    private static final String GET_JOB_QUERY = """
        SELECT 
            * 
        FROM 
            jobs 
        WHERE 
            id = ?
    """;

    private static final String GET_JOBS_QUERY = """
        SELECT 
            * 
        FROM 
            jobs 
        WHERE 
            category LIKE ? 
          AND 
            status LIKE ? 
        ORDER BY 
            start_time
    """;

    public JobStorage() throws SQLException {
        super();
    }

    /**
     * Creates an entry for a running job
     * @param jobDto
     * @return id UUID for inserted row
     * @throws SQLException
     */
    public UUID createJob(JobDto jobDto) throws SQLException{
        UUID id = UUID.randomUUID();
        try (PreparedStatement stmt = connection.prepareStatement(INSERT_JOB_QUERY)) {
            stmt.setObject(1, id);
            stmt.setString(2, jobDto.getType().name());
            stmt.setString(3, jobDto.getCategory().name());
            stmt.setString(4, jobDto.getSource());
            stmt.setString(5, jobDto.getCreatedBy());
            stmt.setString(6, jobDto.getJobStatus().name());
            stmt.setTimestamp(7, jobDto.getModifiedTimeFrom() == null ?
                    null : Timestamp.from(jobDto.getModifiedTimeFrom()));
            stmt.setTimestamp(8, jobDto.getStartTime() == null ?
                    null : Timestamp.from(jobDto.getStartTime()));
            stmt.executeUpdate();
            return id;
        }
    }

    /**
     * Return a job
     * @param id UUID primary key on job
     * @return job with matching id
     * @throws SQLException
     */
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

    /**
     * Check if there is a running job matching category and source
     * @param categoryDto what category the job is
     * @param source where is the data harvest/index/upload from
     * @return boolean if there is a running job matching category and source
     * @throws SQLException
     */
    public boolean hasRunningJob(CategoryDto categoryDto, String source) throws SQLException {

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

    /**
     * List the number of jobs in the database
     *
     * @param categoryDto limit to a specific category
     * @param jobStatusDto limit to a job status
     * @return A list of jobs
     * @throws SQLException
     */
    public List<JobDto> getJobs(CategoryDto categoryDto, JobStatusDto jobStatusDto) throws SQLException {
        List<JobDto> jobs = new ArrayList<>();
        try(PreparedStatement stmt = connection.prepareStatement(GET_JOBS_QUERY)) {
            stmt.setString(1, categoryDto == null ? "%" : categoryDto.name());
            stmt.setString(2, jobStatusDto == null ? "%" : jobStatusDto.name());
            try (ResultSet result = stmt.executeQuery()) {
                while (result.next()) {
                    jobs.add(createJobDtoFromResult(result));
                }
            }
        }
        return jobs;
    }

    /**
     * Update job matching id
     * @param modifiedJobDto
     * @return how many rows was affected by the update
     * @throws SQLException
     */
    public int updateJob(JobDto modifiedJobDto) throws SQLException {
        try(PreparedStatement stmt = connection.prepareStatement(UPDATE_JOB_QUERY)) {
            stmt.setString(1, modifiedJobDto.getJobStatus().name());
            stmt.setObject(2, modifiedJobDto.getErrorCorrelationId());
            stmt.setString(3, modifiedJobDto.getMessage());
            stmt.setTimestamp(4, modifiedJobDto.getEndTime() == null ?
                    null : Timestamp.from(modifiedJobDto.getEndTime()));
            stmt.setObject(5, modifiedJobDto.getNumberOfRecords());
            stmt.setTimestamp(6, modifiedJobDto.getModifiedTimeFrom() == null ?
                    null : Timestamp.from(modifiedJobDto.getModifiedTimeFrom()));
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
        jobDto.setModifiedTimeFrom(createInstantFromTimeStamp(result,"modified_time_from"));
        jobDto.setStartTime(createInstantFromTimeStamp(result, "start_time"));
        jobDto.setEndTime(createInstantFromTimeStamp(result,"end_time"));
        jobDto.setNumberOfRecords(result.getObject("number_of_records", Integer.class));
        jobDto.setRestartValue(createInstantFromTimeStamp(result,"restart_value"));
        return jobDto;
    }

    private Instant createInstantFromTimeStamp(ResultSet result, String column) throws SQLException {
        OffsetDateTime offsetDateTime = result.getObject(column, OffsetDateTime.class);
        if (offsetDateTime != null) {
            return offsetDateTime.toInstant();
        }
        return null;
    }


}
