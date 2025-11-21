package com.kcn.hikvisionmanager.client;

import com.kcn.hikvisionmanager.config.CameraConfig;
import com.kcn.hikvisionmanager.events.model.CameraRestartInitiatedEvent;
import com.kcn.hikvisionmanager.exception.CameraRequestException;
import com.kcn.hikvisionmanager.exception.CameraUnauthorizedException;
import com.kcn.hikvisionmanager.service.ProgressListener;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Service responsible for streaming video downloads from Hikvision camera via HTTP.
 * Handles large file downloads with progress tracking and atomic file operations.
 * Automatically pauses operations during camera restart grace period.
 */
@Slf4j
@Component
public class HttpDownloadClient {

    private final CloseableHttpClient httpClient;
    private final CameraConfig cameraConfig;

    // Grace period tracking for camera restart
    private volatile LocalDateTime restartGraceUntil = null;

    public HttpDownloadClient(CloseableHttpClient httpClient, CameraConfig cameraConfig) {
        this.httpClient = httpClient;
        this.cameraConfig = cameraConfig;
        log.info("✅ HttpDownloadClient initialized for camera {}:{}",
                cameraConfig.getIp(), cameraConfig.getPort());
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
        log.info("⏸️ [{}] HTTP download client paused for {} seconds due to camera restart (until: {})",
                Thread.currentThread().getName(),
                event.getGracePeriodSeconds(),
                restartGraceUntil);
    }

    /**
     * Downloads video recording from camera using HTTP GET with XML payload.
     * Streams response directly to file with progress tracking and atomic file operations.
     * Automatically waits during camera restart grace period.
     *
     * @param url ISAPI download endpoint (e.g., /ISAPI/ContentMgmt/download)
     * @param xmlPayload XML body containing playbackURI and time range
     * @param outputPath Target file path for downloaded video
     * @param progressListener Listener for tracking download progress
     * @param timeoutMinutes Maximum download duration in minutes
     * @throws IOException If network error, timeout, or file operation fails
     * @throws CameraUnauthorizedException If authentication fails (401/403)
     * @throws CameraRequestException If HTTP request fails with 4xx/5xx status
     */
    public void executeDownloadStream(
            String url,
            String xmlPayload,
            Path outputPath,
            ProgressListener progressListener,
            int timeoutMinutes) throws IOException {

        // CRITICAL: Wait if camera is restarting before initiating HTTP request
        try {
            waitIfCameraRestarting();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Download interrupted during camera restart wait", e);
        }

        log.debug("🎬 [{}] Starting HTTP download stream to: {}",
                Thread.currentThread().getName(), outputPath.getFileName());
        log.debug("Download URL: {}", url);

        // Prepare HTTP GET request with XML payload
        HttpGet httpGet = new HttpGet(url);
        httpGet.setEntity(new StringEntity(xmlPayload, ContentType.APPLICATION_XML));

        // Extended timeout for large video files
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(HttpClientConfig.CONNECT_TIMEOUT_SECONDS))
                .setResponseTimeout(Timeout.ofMinutes(timeoutMinutes))
                .build();
        httpGet.setConfig(requestConfig);

        // Create temporary file for atomic write operation
        Path tempFile = Files.createTempFile(
                outputPath.getParent(),
                "download_",
                ".tmp"
        );

        try {
            httpClient.execute(httpGet, response -> {
                int statusCode = response.getCode();
                log.debug("Download response status: {}", statusCode);

                // Handle authentication errors
                if (statusCode == 401 || statusCode == 403) {
                    throw new CameraUnauthorizedException(
                            "Unauthorized download request to camera " + cameraConfig.getIp());
                }

                // Handle HTTP errors
                if (statusCode >= 400) {
                    String errorBody = EntityUtils.toString(response.getEntity());
                    throw new CameraRequestException(
                            "Download request failed with status " + statusCode + ": " + errorBody);
                }

                // Get expected file size from Content-Length header
                long totalBytes = response.getEntity().getContentLength();
                log.debug("📦 Expected file size: {} MB ({} bytes)",
                        totalBytes / (1024 * 1024), totalBytes);

                if (totalBytes <= 0) {
                    log.warn("⚠️ Content-Length not provided by camera, progress will be estimated");
                }

                // Stream content to temporary file with progress tracking
                streamContentToFile(response.getEntity().getContent(), tempFile,
                        progressListener, totalBytes);

                return null;
            });

            // Move temporary file to final destination (atomic operation)
            Files.move(tempFile, outputPath, StandardCopyOption.REPLACE_EXISTING);
            log.debug("📁 File moved to final location: {}", outputPath);

        } catch (Exception e) {
            // Cleanup temporary file on any error
            cleanupTempFile(tempFile);
            throw e;
        }
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
            log.info("⏳ [{}] HTTP download client waiting {} seconds for camera restart (until: {})",
                    Thread.currentThread().getName(),
                    secondsToWait,
                    restartGraceUntil);

            // Sleep until grace period ends
            Thread.sleep(secondsToWait * 1000L);

            // Reset grace period after waiting
            restartGraceUntil = null;
            log.info("✅ [{}] Camera restart grace period ended, resuming HTTP downloads",
                    Thread.currentThread().getName());
        } else {
            // Grace period already expired
            restartGraceUntil = null;
        }
    }

    /**
     * Streams HTTP response content to file with buffered I/O and progress tracking.
     * Reports progress at regular intervals during download.
     *
     * @param inputStream Source stream from HTTP response
     * @param targetFile Temporary file to write content to
     * @param progressListener Listener for progress updates
     * @param totalBytes Expected total file size (for progress calculation)
     * @throws IOException If streaming or file write fails
     */
    private void streamContentToFile(
            InputStream inputStream,
            Path targetFile,
            ProgressListener progressListener,
            long totalBytes) throws IOException {

        try (InputStream bufferedInput = new BufferedInputStream(
                inputStream, HttpClientConfig.STREAM_BUFFER_SIZE);
             BufferedOutputStream bufferedOutput = new BufferedOutputStream(
                     Files.newOutputStream(targetFile), HttpClientConfig.STREAM_BUFFER_SIZE)) {

            byte[] buffer = new byte[HttpClientConfig.CHUNK_SIZE];
            long downloadedBytes = 0;
            int bytesRead;
            long lastReportedBytes = 0;

            // Read and write in chunks, reporting progress periodically
            while ((bytesRead = bufferedInput.read(buffer)) != -1) {
                bufferedOutput.write(buffer, 0, bytesRead);
                downloadedBytes += bytesRead;

                // Report progress every ~100KB to avoid excessive updates
                if (downloadedBytes - lastReportedBytes >= HttpClientConfig.PROGRESS_REPORT_INTERVAL) {
                    progressListener.onProgress(downloadedBytes);
                    lastReportedBytes = downloadedBytes;
                }
            }

            // Final progress notification
            progressListener.onComplete(targetFile);

            log.info("✅ [{}] Download stream completed: {} MB downloaded",
                    Thread.currentThread().getName(),
                    downloadedBytes / (1024 * 1024));

        } catch (IOException e) {
            log.error("❌ Error during file streaming: {}", e.getMessage());
            progressListener.onError(e.getMessage());
            throw new CameraRequestException("Failed to stream download content", e);
        }
    }

    /**
     * Safely deletes temporary file if it exists.
     * Logs warning if cleanup fails but doesn't throw exception.
     *
     * @param tempFile Temporary file to delete
     */
    private void cleanupTempFile(Path tempFile) {
        try {
            if (Files.exists(tempFile)) {
                Files.delete(tempFile);
                log.debug("🗑️ Cleaned up temp file after error");
            }
        } catch (IOException cleanupError) {
            log.warn("Failed to cleanup temp file: {}", cleanupError.getMessage());
        }
    }
}