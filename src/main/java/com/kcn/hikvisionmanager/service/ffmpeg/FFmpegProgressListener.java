package com.kcn.hikvisionmanager.service.ffmpeg;

import com.kcn.hikvisionmanager.domain.DownloadJob;
import com.kcn.hikvisionmanager.domain.DownloadStatus;
import com.kcn.hikvisionmanager.events.publishers.RecordingDownloadPublisher;
import com.kcn.hikvisionmanager.repository.DownloadJobRepository;
import com.kcn.hikvisionmanager.service.ProgressListener;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.time.LocalDateTime;

@Slf4j
@AllArgsConstructor
public class FFmpegProgressListener implements ProgressListener {

        DownloadJob job;
        DownloadJobRepository repository;
        RecordingDownloadPublisher publisher;

        @Override
        public void onProgress(long downloadedBytes) {
            job.setProgressPercent(Math.toIntExact(job.getTotalBytes() / downloadedBytes * 100));
            job.setDownloadedBytes(downloadedBytes);
            job.setEta(null);
            repository.save(job);
        }

        @Override
        public void onComplete(Path filePath) {
            job.setStatus(DownloadStatus.COMPLETED);
            job.setCompletedAt(LocalDateTime.now());
            job.setProgressPercent(100);

            try {
                job.setActualFileSizeBytes(java.nio.file.Files.size(filePath));
            } catch (Exception e) {
                log.warn("Failed to get file size: {}", e.getMessage());
            }

            repository.save(job);
            log.info("✅ Download completed: {} (Job: {})", job.getFileName(), job.getJobId());
            if(job.isBackupJob())
                publisher.publishDownloadCompleted(job.getRecordingId(), job.getBatchId(), job.getActualFileSizeBytes());
        }

        @Override
        public void onError(String error) {
            job.setStatus(DownloadStatus.FAILED);
            job.setErrorMessage(error);
            repository.save(job);
            log.error("❌ Download failed: {} - {}", job.getJobId(), error);
            if(job.isBackupJob())
                publisher.publishDownloadFailed(job.getRecordingId(), job.getBatchId(), job.getActualFileSizeBytes(), job.getErrorMessage());
        }

}
