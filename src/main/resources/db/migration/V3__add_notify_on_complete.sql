-- Migration: Add notification support to backup configurations
-- Version: V3
-- Description: Adds notify_on_complete column to enable/disable notifications per backup

-- Add column with default value
ALTER TABLE backup_configurations
ADD COLUMN notify_on_complete BOOLEAN NOT NULL DEFAULT false;

-- Ensure all existing records have false (for safety, though DEFAULT handles this)
UPDATE backup_configurations
SET notify_on_complete = false
WHERE notify_on_complete IS NULL;

