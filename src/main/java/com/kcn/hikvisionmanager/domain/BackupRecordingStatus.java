package com.kcn.hikvisionmanager.domain;

public enum BackupRecordingStatus {
    QUEUED,      // Waiting to download
    DOWNLOADING, // Currently downloading
    COMPLETED,   // Successfully downloaded
    FAILED       // Download failed
}