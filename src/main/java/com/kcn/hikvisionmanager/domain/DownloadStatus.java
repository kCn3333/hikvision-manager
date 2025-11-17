package com.kcn.hikvisionmanager.domain;

public enum DownloadStatus {
    QUEUED,       // Waiting in the queue
    DOWNLOADING,  // FFmpeg process running
    COMPLETED,    // Download finished successfully
    FAILED,       // Download failed (error or timeout)
    CANCELLED     // Cancelled by user
}