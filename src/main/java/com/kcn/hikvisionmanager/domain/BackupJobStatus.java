package com.kcn.hikvisionmanager.domain;

public enum BackupJobStatus {
    STARTED,      // Backup job created
    SEARCHING,    // Searching for recordings
    DOWNLOADING,  // Downloads in progress
    COMPLETED,    // All done successfully
    FAILED,       // Failed with errors
    CANCELLED     // Cancelled by user
}
