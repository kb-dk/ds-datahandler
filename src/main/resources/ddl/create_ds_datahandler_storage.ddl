CREATE TABLE IF NOT EXISTS jobs (
    id UUID PRIMARY KEY,
    type VARCHAR,
    category VARCHAR,
    source VARCHAR,
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

