package com.kcn.hikvisionmanager.service.download;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;

/**
 * Semaphore to limit concurrent downloads from camera
 * Hikvision cameras typically support only 1 concurrent RTSP download
 */
@Component
@Slf4j
public class CameraDownloadSemaphore {

    private final Semaphore semaphore;

    public CameraDownloadSemaphore() {
        // Only 1 concurrent download from the camera
        this.semaphore = new Semaphore(1);
    }

    /**
     * Acquire a download slot (blocking)
     */
    public void acquire() throws InterruptedException {
        log.debug("⏳ Waiting for camera download slot...");
        semaphore.acquire();
        log.debug("✅ Camera download slot acquired");
    }

    /**
     * Release download slot
     */
    public void release() {
        semaphore.release();
        log.debug("🔓 Camera download slot released");
    }

    /**
     * Check if the camera is busy
     */
    public boolean isBusy() {
        return semaphore.availablePermits() == 0;
    }
}
