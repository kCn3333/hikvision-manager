package com.kcn.hikvisionmanager.service;

import java.nio.file.Path;

/**
 * Listener interface for FFmpeg download progress
 */
public interface ProgressListener {

    /**
     * Called when download progress is updated
     *
     * @param downloadedBytes already downloaded bytes
     */
    void onProgress(long downloadedBytes);

    /**
     * Called when download completes successfully
     *
     * @param filePath Path to downloaded file
     */
    void onComplete(Path filePath);

    /**
     * Called when download fails
     *
     * @param error Error message
     */
    void onError(String error);
}