package com.kcn.hikvisionmanager.service.ffmpeg;

import com.kcn.hikvisionmanager.events.model.CameraRestartInitiatedEvent;
import com.kcn.hikvisionmanager.service.ProgressListener;
import com.kcn.hikvisionmanager.util.ProgressCalculator;
import com.kcn.hikvisionmanager.domain.DownloadJob;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for executing FFmpeg downloads from camera RTSP stream
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FFmpegDownoladService {

    // Pattern to extract time from FFmpeg output: time=00:02:15.67
    private static final Pattern TIME_PATTERN = Pattern.compile("time=(\\d{2}:\\d{2}:\\d{2}\\.\\d{2})");

    private final FFmpegCommandBuilder commandBuilder;

    // Grace period tracking for camera restart (same as other services)
    private volatile LocalDateTime restartGraceUntil = null;

    @PostConstruct
    public void checkFFmpegAvailability() {
        String version = getFFmpegVersion();
        if (version == null) {
            log.warn("⚠️ FFmpeg not found or not accessible in PATH — downloads will fail!");
        } else {
            log.info("✅ FFmpeg detected and available (version: {})", version);
        }
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
        log.info("⏸️ [{}] FFmpeg operations paused for {} seconds due to camera restart (until: {})",
                Thread.currentThread().getName(),
                event.getGracePeriodSeconds(),
                restartGraceUntil);
    }

    /**
     * Download recording from RTSP stream using FFmpeg.
     * Automatically waits during camera restart grace period.
     *
     * @param job Download job with recording info
     * @param listener Progress listener
     * @param timeoutMinutes Maximum time allowed for download
     */
    public void downloadRecording(DownloadJob job, ProgressListener listener, int timeoutMinutes) {

        Process process = null;

        try {
            // CRITICAL: Wait if camera is restarting
            waitIfCameraRestarting();

            // Ensure output directory exists
            Files.createDirectories(job.getFilePath().getParent());

            // Build FFmpeg command
            List<String> command = commandBuilder.buildFFmpegDownloadCommand(job.getRtspUrl(), job.getFilePath());

            long startTime = System.currentTimeMillis();

            log.info("🎬 Starting FFmpeg download: {}", job.getFileName());
            log.debug("FFmpeg command: {}", String.join(" ", command));

            // Start FFmpeg process
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            process = pb.start();

            // Create executor for timeout handling
            try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
                final Process finalProcess = process;

                // Submit output reading task
                Future<?> outputFuture = executor.submit(() ->
                        readFFmpegOutput(finalProcess, job.getDuration(), job.getTotalBytes(), listener));

                try {
                    // Wait for completion with timeout
                    outputFuture.get(timeoutMinutes, TimeUnit.MINUTES);

                    // Wait for process to complete
                    int exitCode = process.waitFor();

                    if (exitCode == 0) {

                        long endTime = System.currentTimeMillis();
                        long durationSeconds = (endTime - startTime) / 1000;
                        long durationMinutes = durationSeconds / 60;

                        log.info("✅ FFmpeg download completed successfully: {}", job.getFilePath());
                        log.info("⌛ Download completed successfully in {} min {} sec ({} MB)",
                                durationMinutes, durationSeconds % 60,
                                job.getTotalBytes() / (1024 * 1024));
                        listener.onComplete(job.getFilePath());
                    } else {
                        String error = String.format("FFmpeg process failed with exit code: %d", exitCode);
                        log.error("❌ {}", error);
                        listener.onError(error);
                    }

                } catch (TimeoutException e) {
                    String error = String.format("Download timeout (exceeded %d minutes)", timeoutMinutes);
                    log.error("⏰ {}", error);
                    process.destroyForcibly();
                    listener.onError(error);

                } finally {
                    executor.shutdownNow();
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("⚠️ FFmpeg download interrupted during camera restart wait");
            listener.onError("Download interrupted during camera restart");

            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }

        } catch (Exception e) {
            log.error("❌ FFmpeg execution failed: {}", e.getMessage(), e);
            listener.onError("FFmpeg execution failed: " + e.getMessage());

            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
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
            log.info("⏳ [{}] Camera restart in progress, FFmpeg waiting {} seconds until {}",
                    Thread.currentThread().getName(),
                    secondsToWait,
                    restartGraceUntil);

            // Sleep until grace period ends
            Thread.sleep(secondsToWait * 1000L);

            // Reset grace period after waiting
            restartGraceUntil = null;
            log.info("✅ [{}] Camera restart grace period ended, resuming FFmpeg operations",
                    Thread.currentThread().getName());
        } else {
            // Grace period already expired
            restartGraceUntil = null;
        }
    }



    /**
     * Read and parse FFmpeg output for progress tracking
     */
    private void readFFmpegOutput(Process process, String totalDuration, long totalBytes, ProgressListener listener) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {

            String line;
            int lastReportedProgress = -1;

            while ((line = reader.readLine()) != null) {

                // Log important lines
                if (line.contains("error") || line.contains("Error")) {
                    // Ignore connection reset at the end of the stream (that's normal)
                    if (line.contains("Error number -10054")) {
                        log.trace("FFmpeg: Connection closed by camera (normal)");
                        continue;
                    }

                    log.warn("FFmpeg warning/error: {}", line);
                }

                // Extract time and calculate progress
                Matcher matcher = TIME_PATTERN.matcher(line);
                if (matcher.find()) {
                    String currentTime = matcher.group(1);

                    // Calculate progress with size estimation
                    ProgressCalculator.ProgressInfo progress =
                            ProgressCalculator.calculate(
                                    currentTime,
                                    totalDuration,
                                    ProgressCalculator.formatBytes(totalBytes)
                            );

                    // Report progress only if changed by at least 1%
                    if (progress.getProgressPercent() > lastReportedProgress) {
                        lastReportedProgress = progress.getProgressPercent();

                        // Log every 10%
                        if (lastReportedProgress % 10 == 0) {
                            log.debug("📊 Progress: {}% ({} / {}, ETA: {})",
                                    lastReportedProgress,
                                    progress.getDownloadedSize(),
                                    progress.getTotalSize(),
                                    progress.getEta());
                        }

                        listener.onProgress(progress.getDownloadedBytes());
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error reading FFmpeg output: {}", e.getMessage());
        }
    }


    /**
     * Check FFmpeg version - returns version number or null if not available
     */
    public String getFFmpegVersion() {
        try {
            ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-version");
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String firstLine = reader.readLine();

            int exitCode = process.waitFor();

            if (exitCode == 0 && firstLine != null) {
                //  (ex. 5.0, 6.1.1, 8.0)
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+\\.\\d+(?:\\.\\d+)?)");
                java.util.regex.Matcher matcher = pattern.matcher(firstLine);

                if (matcher.find()) {
                    return matcher.group(1);
                }
            }
            return null;
        } catch (Exception e) {
            log.warn("FFmpeg not found: {}", e.getMessage());
            return null;
        }
    }
}