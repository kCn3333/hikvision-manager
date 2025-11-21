-- ============================================
--  ADD CAMERAS TABLE
-- ============================================

CREATE TABLE IF NOT EXISTS cameras (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    ip VARCHAR(50) NOT NULL,
    port INTEGER NOT NULL,
    username VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    stream_main_url VARCHAR(500),
    stream_sub_url VARCHAR(500),
    firmware_version VARCHAR(100),
    model VARCHAR(100),
    connected BOOLEAN NOT NULL DEFAULT false
);

-- Optional: Index for faster lookups by IP
CREATE INDEX IF NOT EXISTS idx_cameras_ip ON cameras(ip);