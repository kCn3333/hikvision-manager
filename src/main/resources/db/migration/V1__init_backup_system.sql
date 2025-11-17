-- ============================================
--  INITIAL DATABASE SCHEMA FOR BACKUP SYSTEM
-- ============================================

-- ============================================
-- TABLE: backup_configurations
-- ============================================
CREATE TABLE IF NOT EXISTS backup_configurations (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    camera_id VARCHAR(50) NOT NULL,
    enabled BOOLEAN NOT NULL,
    cron_expression VARCHAR(100) NOT NULL,
    retention_days INTEGER NOT NULL,
    time_range_strategy VARCHAR(20),
    backup_path VARCHAR(500) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    last_run_at TIMESTAMP(6),
    next_run_at TIMESTAMP(6)
);

-- ============================================
-- TABLE: backup_jobs
-- ============================================
CREATE TABLE IF NOT EXISTS backup_jobs (
    id VARCHAR(255) PRIMARY KEY,
    config_id VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMP(6) NOT NULL,
    completed_at TIMESTAMP(6),
    search_start_time TIMESTAMP(6) NOT NULL,
    search_end_time TIMESTAMP(6) NOT NULL,
    total_recordings INTEGER NOT NULL,
    completed_recordings INTEGER,
    failed_recordings INTEGER,
    total_size_bytes BIGINT,
    backup_directory VARCHAR(500) NOT NULL,
    error_message TEXT,
    CONSTRAINT fk_backup_jobs_config
        FOREIGN KEY (config_id)
        REFERENCES backup_configurations(id)
        ON DELETE CASCADE
);

-- ============================================
-- TABLE: backup_recordings
-- ============================================
CREATE TABLE IF NOT EXISTS backup_recordings (
    id BIGSERIAL PRIMARY KEY,
    backup_job_id VARCHAR(255) NOT NULL,
    recording_id VARCHAR(100) NOT NULL,
    track_id VARCHAR(10) NOT NULL,
    start_time TIMESTAMP(6) NOT NULL,
    end_time TIMESTAMP(6) NOT NULL,
    duration VARCHAR(20),
    file_name VARCHAR(255) NOT NULL,
    file_size_bytes BIGINT,
    status VARCHAR(20) NOT NULL,
    error_message TEXT,
    downloaded_at TIMESTAMP(6),
    CONSTRAINT fk_backup_recordings_job
        FOREIGN KEY (backup_job_id)
        REFERENCES backup_jobs(id)
        ON DELETE CASCADE
);

-- ============================================
-- INDEXES FOR PERFORMANCE
-- ============================================
CREATE INDEX IF NOT EXISTS idx_backup_jobs_config_id
    ON backup_jobs(config_id);

CREATE INDEX IF NOT EXISTS idx_backup_recordings_job_id
    ON backup_recordings(backup_job_id);

CREATE INDEX IF NOT EXISTS idx_backup_recordings_recording_id
    ON backup_recordings(recording_id);
