CREATE TABLE IF NOT EXISTS jobs (
    id UUID PRIMARY KEY,
    type VARCHAR,
    category VARCHAR,
    source VARCHAR,
    created_by VARCHAR,
    status VARCHAR,
    error_correlation_id UUID,
    message TEXT,
    modified_time_from TIMESTAMP WITH TIME ZONE NULL,
    start_time TIMESTAMP WITH TIME ZONE,
    end_time TIMESTAMP WITH TIME ZONE,
    number_of_records INTEGER,
    restart_value TIMESTAMP WITH TIME ZONE NULL
)

