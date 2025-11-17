package com.kcn.hikvisionmanager.service.download;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.kcn.hikvisionmanager.client.HttpDownloadClient;
import com.kcn.hikvisionmanager.domain.DownloadJob;
import com.kcn.hikvisionmanager.dto.xml.request.RecordingDownloadRequestXml;
import com.kcn.hikvisionmanager.exception.CameraOfflineException;
import com.kcn.hikvisionmanager.exception.CameraRequestException;
import com.kcn.hikvisionmanager.service.CameraUrlBuilder;
import com.kcn.hikvisionmanager.service.ProgressListener;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;

/**
 * Service for downloading recordings via HTTP (ISAPI ContentMgmt/download)
 *
 **/
@Service
@RequiredArgsConstructor
@Slf4j
public class HttpDownloadService {

    private final HttpDownloadClient downloadClient;
    private final CameraUrlBuilder urlBuilder;
    private final XmlMapper xmlMapper;

    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final int RETRY_DELAY_SECONDS = 5;

    @PostConstruct
    public void init() {
        log.info("✅ HttpDownloadClient initialized (ISAPI HTTP download)");
    }

    /**
     * Download recording from camera via HTTP with progress tracking
     *
     * @param job Download job with recording info
     * @param listener Progress listener (reused from FFmpeg)
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

                    // Wait for camera to come back online
                    try {
                        Thread.sleep(30000); // 30s wait after restart
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
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
     *
     * Format:
     * <?xml version='1.0' encoding='utf8'?>
     * <downloadRequest>
     *     <playbackURI>rtsp://192.168.0.64/Streaming/tracks/101/?starttime=...&endtime=...</playbackURI>
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
     * Try to restart camera (silent failure if restart fails)
     */
    private void tryRestartCamera() {
        try {
            log.info("🔄 Attempting camera restart...");
//            String restartUrl = urlBuilder.buildRestartUrl();
//
//            // Note: Restart endpoint typically returns 200 but camera will disconnect
//            isapiClient.executePut(restartUrl, "", String.class);
//
//            log.info("📡 Camera restart command sent successfully");

        } catch (Exception e) {
            log.warn("⚠️ Failed to restart camera: {} (continuing anyway)", e.getMessage());
        }
    }
}
