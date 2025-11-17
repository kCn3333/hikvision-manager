package com.kcn.hikvisionmanager.service.download;

import com.kcn.hikvisionmanager.config.DownloadConfig;
import com.kcn.hikvisionmanager.domain.DownloadJob;
import com.kcn.hikvisionmanager.domain.DownloadStatus;
import com.kcn.hikvisionmanager.events.publishers.RecordingDownloadPublisher;
import com.kcn.hikvisionmanager.repository.DownloadJobRepository;
import com.kcn.hikvisionmanager.service.ProgressListener;
import com.kcn.hikvisionmanager.service.ffmpeg.FFmpegDownoladService;
import com.kcn.hikvisionmanager.service.ffmpeg.FFmpegProgressListener;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.Executor;

/**
 * Queue manager for download jobs
 * Handles asynchronous execution with camera concurrency control
 */
@Component
@Slf4j

public class DownloadJobQueue {

    private final FFmpegDownoladService ffmpegDownloadService;
    private final CameraDownloadSemaphore cameraSemaphore;
    private final DownloadJobRepository repository;
    private final HttpDownloadService httpDownloadService;
    private final DownloadConfig config;
    private final RecordingDownloadPublisher publisher;

    @Qualifier("cameraTaskExecutor")
    private final Executor taskExecutor;

    public DownloadJobQueue(FFmpegDownoladService ffmpegDownloadService, CameraDownloadSemaphore cameraSemaphore, DownloadJobRepository repository, HttpDownloadService httpDownloadService, DownloadConfig config, RecordingDownloadPublisher publisher, @Qualifier("cameraTaskExecutor") Executor taskExecutor) {
        this.ffmpegDownloadService = ffmpegDownloadService;
        this.cameraSemaphore = cameraSemaphore;
        this.repository = repository;
        this.httpDownloadService = httpDownloadService;
        this.config = config;
        this.publisher = publisher;
        this.taskExecutor = taskExecutor;
    }

    @PostConstruct
    public void init() {
        String method = config.isHttpDownload() ? "HTTP (fast download)" : "FFmpeg (transcode)";
        log.info("✅ Download job queue initialized using {} method", method);
    }


    /**
     * Submit download job to queue
     */
    public void submit(DownloadJob job) {
        log.debug("📥 Submitting download job to queue: {}", job.getJobId());

        taskExecutor.execute(() -> executeDownload(job));
    }

    /**
     * Execute download job (runs in separate thread)
     */
    private void executeDownload(DownloadJob job) {
        try {
            // Wait for camera to be available
            cameraSemaphore.acquire();

            // Update status to DOWNLOADING
            job.setStatus(DownloadStatus.DOWNLOADING);
            job.setStartedAt(LocalDateTime.now());
            repository.save(job);

            log.info("\uD83D\uDE80 [{}] Starting download: {} (Job: {}, Method: {})",
                    Thread.currentThread().getName(),
                    job.getFileName(),
                    job.getJobId(),
                    config.getMethod().toUpperCase());

            if(job.isBackupJob())
                publisher.publishDownloadStarted(job.getRecordingId(), job.getBatchId());

            // ✅
            ProgressListener listener;
            if (config.isHttpDownload()) {
                listener = new HttpProgressListener(job, repository, publisher);
                // HTTP download
                httpDownloadService.downloadRecording(job, listener, config.getTimeoutMinutes());
            } else {
                listener = new FFmpegProgressListener(job, repository, publisher);
                // FFmpeg
                ffmpegDownloadService.downloadRecording(job, listener, config.getTimeoutMinutes());

            }

        } catch (InterruptedException e) {
            log.warn("⚠️ Download interrupted: {}", job.getJobId());
            job.setStatus(DownloadStatus.CANCELLED);
            job.setErrorMessage("Download interrupted");
            repository.save(job);
            if(job.isBackupJob())
                publisher.publishDownloadFailed(job.getRecordingId(),job.getBatchId(),job.getActualFileSizeBytes(),job.getErrorMessage());
            Thread.currentThread().interrupt();

        } catch (Exception e) {
            log.error("❌ Download failed: {}", job.getJobId(), e);
            job.setStatus(DownloadStatus.FAILED);
            job.setErrorMessage("Unexpected error: " + e.getMessage());
            repository.save(job);
            if(job.isBackupJob())
                publisher.publishDownloadFailed(job.getRecordingId(),job.getBatchId(),job.getActualFileSizeBytes(),job.getErrorMessage());
        } finally {
            // Always release semaphore
            cameraSemaphore.release();
        }
    }

}