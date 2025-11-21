package com.kcn.hikvisionmanager.service.download;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.kcn.hikvisionmanager.client.HttpDownloadClient;
import com.kcn.hikvisionmanager.domain.DownloadJob;
import com.kcn.hikvisionmanager.dto.xml.request.RecordingDownloadRequestXml;
import com.kcn.hikvisionmanager.events.model.CameraRestartInitiatedEvent;
import com.kcn.hikvisionmanager.exception.CameraOfflineException;
import com.kcn.hikvisionmanager.exception.CameraRequestException;
import com.kcn.hikvisionmanager.service.CameraManagementService;
import com.kcn.hikvisionmanager.service.CameraUrlBuilder;
import com.kcn.hikvisionmanager.service.ProgressListener;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Service for downloading recordings via HTTP (ISAPI ContentMgmt/download)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HttpDownloadService {

    private final HttpDownloadClient downloadClient;
    private final CameraUrlBuilder urlBuilder;
    private final XmlMapper xmlMapper;
    private final CameraManagementService managementService;

    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final int RETRY_DELAY_SECONDS = 12;

    // Grace period tracking for camera restart (same as CameraService)
    private volatile LocalDateTime restartGraceUntil = null;

    @PostConstruct
    public void init() {
        log.info("✅ HttpDownloadService initialized (ISAPI HTTP download)");
    }

    /**
     * Event listener that handles camera restart initialization.
     * Sets grace period during which download attempts will wait.
     *
     * @param event Camera restart event containing grace period duration
     */
    @EventListener
    public void onCameraRestart(CameraRestartInitiatedEvent event) {
        restartGraceUntil = event.getOccurredAt().plusSeconds(event.getGracePeriodSeconds());
        log.debug("⏸️ [{}] Download operations paused for {} seconds due to camera restart (until: {})",
                Thread.currentThread().getName(),
                event.getGracePeriodSeconds(),
                restartGraceUntil);
    }

    /**
     * Download recording from camera via HTTP with progress tracking.
     * Automatically waits during camera restart grace period.
     *
     * @param job            Download job with recording info
     * @param listener       Progress listener (reused from FFmpeg)
     * @param timeoutMinutes Maximum time allowed for download
     */
    public void downloadRecording(
            DownloadJob job,
            ProgressListener listener,
            int timeoutMinutes) {

        log.debug("🚀 [{}] Starting HTTP download: {}", Thread.currentThread().getName(), job.getFileName());

        int attempt = 0;
        Exception lastException = null;

        while (attempt < MAX_RETRY_ATTEMPTS) {
            attempt++;

            try {
                if (attempt > 1) {
                    log.info("🔄 Retry attempt {}/{}", attempt, MAX_RETRY_ATTEMPTS);
                    Thread.sleep(RETRY_DELAY_SECONDS * 1000L);
                }

                // CRITICAL: Wait if camera is restarting
                waitIfCameraRestarting();

                executeDownload(job, listener, timeoutMinutes);

                // Success - exit retry loop
                return;

            } catch (CameraOfflineException | CameraRequestException e) {
                lastException = e;
                log.warn("⚠️ Download attempt {}/{} failed: {}",
                        attempt, MAX_RETRY_ATTEMPTS, e.getMessage());

                // On last attempt, try camera restart
                if (attempt == MAX_RETRY_ATTEMPTS - 1) {
                    log.warn("🔄 Last retry attempt - restarting camera...");
                    tryRestartCamera();
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("⚠️ Download interrupted during retry");
                listener.onError("Download interrupted");
                return;

            } catch (Exception e) {
                // Unexpected error - fail immediately
                log.error("❌ Unexpected download error: {}", e.getMessage(), e);
                listener.onError("Unexpected error: " + e.getMessage());
                return;
            }
        }

        // All retries exhausted
        String errorMsg = String.format(
                "Download failed after %d attempts: %s",
                MAX_RETRY_ATTEMPTS,
                lastException != null ? lastException.getMessage() : "Unknown error"
        );
        log.error("❌ {}", errorMsg);
        listener.onError(errorMsg);
    }

    /**
     * Waits if camera is currently in restart grace period.
     * Blocks the calling thread until grace period expires.
     *
     * @throws InterruptedException if thread is interrupted while waiting
     */
    private void waitIfCameraRestarting() throws InterruptedException {
        if (restartGraceUntil == null) {
            return; // No restart in progress
        }

        LocalDateTime now = LocalDateTime.now();

        // Check if grace period is still active
        if (now.isBefore(restartGraceUntil)) {
            long secondsToWait = Duration.between(now, restartGraceUntil).getSeconds();
            log.info("⏳ [{}] Camera restart in progress, waiting {} seconds until {}",
                    Thread.currentThread().getName(),
                    secondsToWait,
                    restartGraceUntil);

            // Sleep until grace period ends
            Thread.sleep(secondsToWait * 1000L);

            // Reset grace period after waiting
            restartGraceUntil = null;
            log.info("✅ [{}] Camera restart grace period ended, resuming download operations",
                    Thread.currentThread().getName());
        } else {
            // Grace period already expired
            restartGraceUntil = null;
        }
    }

    /**
     * Execute single download attempt
     */
    private void executeDownload(
            DownloadJob job,
            ProgressListener listener,
            int timeoutMinutes) throws IOException {

        // Ensure output directory exists
        Files.createDirectories(job.getFilePath().getParent());

        // Build XML payload
        String xmlPayload = buildDownloadRequestXml(job.getRtspUrl());

        // Build download URL
        String downloadUrl = urlBuilder.buildDownloadUrl();

        log.debug("📄 Download payload: {}", xmlPayload);

        // Progress tracking state
        final long startTimeMs = System.currentTimeMillis();
        final long[] lastUpdateTime = {startTimeMs};
        final long[] lastDownloadedBytes = {0};

        // Execute HTTP download with streaming
        downloadClient.executeDownloadStream(
                downloadUrl,
                xmlPayload,
                job.getFilePath(),
                listener,
                timeoutMinutes
        );
    }

    /**
     * Build XML payload for download request
     * <p>
     * Format:
     * <?xml version='1.0' encoding='utf8'?>
     * <downloadRequest>
     * <playbackURI>rtsp://192.168.0.64/Streaming/tracks/101/?starttime=...&endtime=...</playbackURI>
     * </downloadRequest>
     */
    private String buildDownloadRequestXml(String playbackUrl) {
        RecordingDownloadRequestXml request = new RecordingDownloadRequestXml(playbackUrl);
        try {
            return xmlMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize download request XML", e);
            // Fallback to manual XML
            return createManualXml(playbackUrl);
        }
    }

    private String createManualXml(String playbackUrl) {
        return "<?xml version='1.0' encoding='utf8'?>\n" +
                "<downloadRequest>\n" +
                "    <playbackURI>" + playbackUrl.replace("&", "&amp;") + "</playbackURI>\n" +
                "</downloadRequest>";
    }

    /**
     * Format duration in human-readable format (e.g., "2m 30s")
     */
    private String formatDuration(long milliseconds) {
        Duration duration = Duration.ofMillis(milliseconds);
        long minutes = duration.toMinutes();
        long seconds = duration.minusMinutes(minutes).getSeconds();

        if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }

    /**
     * Try to restart camera and wait for grace period.
     * This ensures the camera has time to fully restart before retry attempts.
     */
    private void tryRestartCamera() {
        try {
            log.info("⚠️ Initiating camera restart...");
            managementService.restartCamera();

            // Wait for the restart grace period to be established and complete
            // Small delay to ensure event is processed
            Thread.sleep(1500);

            // Now wait for the full grace period
            waitIfCameraRestarting();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("⚠️ Interrupted while waiting for camera restart");
        }
    }
}