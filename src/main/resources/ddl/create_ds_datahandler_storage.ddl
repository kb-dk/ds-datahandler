CREATE TABLE IF NOT EXISTS jobs (
    id UUID,
    name VARCHAR,
    type VARCHAR,
    createdBy VARCHAR,
    status VARCHAR,
    errorCorrelationId UUID,
    message TEXT,
    mTimeFrom BIGINT,
    startTime TIMESTAMP WITH TIME ZONE,
    endTime TIMESTAMP WITH TIME ZONE,
    numberOfRecords BIGINT,
    restartValue BIGINT
)

