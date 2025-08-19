CREATE TABLE IF NOT EXISTS jobs (
    id UUID PRIMARY KEY,
    type VARCHAR NOT NULL,
    category VARCHAR NOT NULL,
    source VARCHAR NULL,
    created_by VARCHAR NOT NULL,
    status VARCHAR NOT NULL,
    error_correlation_id UUID NULL,
    message TEXT NULL,
    modified_time_from TIMESTAMP WITH TIME ZONE NULL,
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP WITH TIME ZONE NULL,
    number_of_records INTEGER NULL,
    restart_value TIMESTAMP WITH TIME ZONE NULL
)

