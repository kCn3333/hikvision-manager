package com.kcn.hikvisionmanager.service.download;

import com.kcn.hikvisionmanager.domain.DownloadJob;
import com.kcn.hikvisionmanager.domain.DownloadStatus;
import com.kcn.hikvisionmanager.events.publishers.RecordingDownloadPublisher;
import com.kcn.hikvisionmanager.repository.DownloadJobRepository;
import com.kcn.hikvisionmanager.service.ProgressListener;
import com.kcn.hikvisionmanager.util.ProgressCalculator;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Objects;

@Slf4j
@AllArgsConstructor
public class HttpProgressListener implements ProgressListener {

    private final DownloadJob job;
    private final DownloadJobRepository repository;
    private final RecordingDownloadPublisher publisher;
    private final long startTime;

    public HttpProgressListener(DownloadJob job, DownloadJobRepository repository,
                                RecordingDownloadPublisher publisher) {

        this.job = Objects.requireNonNull(job, "Job cannot be null");
        this.repository = Objects.requireNonNull(repository, "Repository cannot be null");
        this.publisher = Objects.requireNonNull(publisher, "Publisher cannot be null");
        this.startTime = System.currentTimeMillis();

        job.setDownloadSpeed(0.0);
        job.setCurrentTime("00:00:00");
        job.setEta("Calculating...");
        repository.save(job);
    }

    @Override
    public void onProgress(long downloadedBytes) {
        try {
            job.setProgressPercent(calculateProgressPercent(downloadedBytes));
            job.setDownloadedBytes(downloadedBytes);
            job.setEta(calculateEta(downloadedBytes));
            job.setCurrentTime(formatCurrentTime());
            job.setDownloadSpeed(calculateDownloadSpeedMbps(downloadedBytes));
            repository.save(job);

            log.debug("📊 HTTP Progress for job {}: {}% ({}/{}) ETA: {}",
                    job.getJobId(), job.getProgressPercent(),
                    ProgressCalculator.formatBytes(downloadedBytes),
                    ProgressCalculator.formatBytes(job.getTotalBytes()),
                    job.getEta());
        } catch (Exception e) {
            log.error("Failed to update HTTP progress for job {}: {}", job.getJobId(), e.getMessage());
        }
    }

    @Override
    public void onComplete(Path filePath) {
        try {
            job.setStatus(DownloadStatus.COMPLETED);
            job.setCompletedAt(LocalDateTime.now());
            job.setProgressPercent(100);
            job.setCurrentTime(formatCurrentTime());
            job.setEta("Completed");
            job.setDownloadSpeed(0.0);

            try {
                long fileSize = Files.size(filePath);
                job.setActualFileSizeBytes(fileSize);
                log.debug("📁 Actual file size for job {}: {}",
                        job.getJobId(), ProgressCalculator.formatBytes(fileSize));
            } catch (Exception e) {
                log.warn("Failed to get file size for job {}: {}", job.getJobId(), e.getMessage());
            }

            long downloadTime = System.currentTimeMillis() - startTime;
            double averageSpeed = calculateAverageSpeedMbps(job.getDownloadedBytes(), downloadTime);

            log.info("✅ HTTP Download completed: {} (Job: {}) in {}, Avg Speed: {} Mbps",
                    job.getFileName(), job.getJobId(), formatDuration(downloadTime), averageSpeed);

            if (job.isBackupJob()) {
                publisher.publishDownloadCompleted(job.getRecordingId(), job.getBatchId(), job.getActualFileSizeBytes());
            }
            repository.save(job);

        } catch (Exception e) {
            log.error("Failed to complete HTTP download job {}: {}", job.getJobId(), e.getMessage());
        }
    }


    @Override
    public void onError(String error) {
        try {
            job.setStatus(DownloadStatus.FAILED);
            job.setErrorMessage(error);
            job.setCurrentTime(formatCurrentTime());
            job.setEta("Failed");
            job.setDownloadSpeed(0.0);

            log.error("❌ HTTP Download failed: {} - {}", job.getJobId(), error);

            if (job.isBackupJob()) {
                publisher.publishDownloadFailed(job.getRecordingId(), job.getBatchId(), job.getActualFileSizeBytes(), job.getErrorMessage());
            }

            repository.save(job);

        } catch (Exception e) {
            log.error("Failed to mark HTTP download job {} as failed: {}", job.getJobId(), e.getMessage());
        }
    }


    /**
     * Metoda pomocnicza do aktualizacji postępu na podstawie pobranych bajtów
     */
    public void updateFromDownloadedBytes(long downloadedBytes) {
        if (job.getTotalBytes()!=0) {
            int percent = (int) ((downloadedBytes * 100) / job.getTotalBytes());
            String eta = calculateEta(downloadedBytes);
            double speedMbps = calculateDownloadSpeedMbps(downloadedBytes);

            updateJobProgress(percent, downloadedBytes, eta, speedMbps);
        } else {
            // Gdy nie znamy totalBytes, pokazujemy tylko pobrane bajty i speed
            double speedMbps = calculateDownloadSpeedMbps(downloadedBytes);
            updateJobProgress(0, downloadedBytes, "Unknown", speedMbps);
        }
    }

    /**
     * Aktualizuje job z wszystkimi polami
     */
    private void updateJobProgress(int percent, long downloadedBytes, String eta, double speedMbps) {
        job.setProgressPercent(percent);
        job.setDownloadedBytes(downloadedBytes);
        job.setEta(eta);
        job.setCurrentTime(formatCurrentTime());
        job.setDownloadSpeed(speedMbps);
        repository.save(job);

        log.debug("📊 HTTP Progress for job {}: {}% ({}/{}) {} ETA: {} Speed: {} Mbps",
                job.getJobId(), percent,
                ProgressCalculator.formatBytes(downloadedBytes),
                ProgressCalculator.formatBytes(job.getTotalBytes()),
                job.getCurrentTime(),
                eta,
                String.format("%.2f", speedMbps));
    }

    /**
     * Oblicza prędkość pobierania w Mbps z dokładnością do 2 miejsc po przecinku
     */
    private double calculateDownloadSpeedMbps(long downloadedBytes) {
        long elapsedTime = System.currentTimeMillis() - startTime;
        if (elapsedTime <= 0) {
            return 0.0;
        }

        // bytes per second
        double bytesPerSecond = downloadedBytes / (elapsedTime / 1000.0);
        // convert to Mbps (1 byte = 8 bits, 1 Mbps = 1,000,000 bits)
        double speedMbps = (bytesPerSecond * 8) / 1_000_000.0;

        // Zaokrąglenie do 2 miejsc po przecinku
        return Math.round(speedMbps * 100.0) / 100.0;
    }

    /**
     * Oblicza średnią prędkość pobierania w Mbps
     */
    private double calculateAverageSpeedMbps(long totalDownloadedBytes, long totalTimeMillis) {
        if (totalTimeMillis <= 0) {
            return 0.0;
        }

        double bytesPerSecond = totalDownloadedBytes / (totalTimeMillis / 1000.0);
        double speedMbps = (bytesPerSecond * 8) / 1_000_000.0;

        return Math.round(speedMbps * 100.0) / 100.0;
    }
    /**
     * Oblicza ETA na podstawie prędkości pobierania
     */
    private String calculateEta(long downloadedBytes) {
        if (job.getTotalBytes() <= 0 || downloadedBytes <= 0) {
            return "Unknown";
        }

        long elapsedTime = System.currentTimeMillis() - startTime;
        if (elapsedTime <= 0) {
            return "Unknown";
        }

        double bytesPerSecond = downloadedBytes / (elapsedTime / 1000.0);
        if (bytesPerSecond <= 0) {
            return "Unknown";
        }

        long remainingBytes = job.getTotalBytes() - downloadedBytes;
        long etaSeconds = (long) (remainingBytes / bytesPerSecond);

        return formatEta(etaSeconds);
    }

    /**
     * Formatuje aktualny czas trwania pobierania
     */
    private String formatCurrentTime() {
        long elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000;
        return formatTimeFromSeconds(elapsedSeconds);
    }

    /**
     * Konwertuje sekundy na format HH:MM:SS
     */
    private String formatTimeFromSeconds(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private String formatEta(long seconds) {
        if (seconds < 0) return "Unknown";
        if (seconds < 60) return seconds + "s";
        else if (seconds < 3600) return String.format("%dm %ds", seconds / 60, seconds % 60);
        else return String.format("%dh %dm", seconds / 3600, (seconds % 3600) / 60);
    }

    private String formatDuration(long milliseconds) {
        long seconds = milliseconds / 1000;
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            return String.format("%dm %ds", seconds / 60, seconds % 60);
        } else {
            return String.format("%dh %dm", seconds / 3600, (seconds % 3600) / 60);
        }
    }
    /**
     * Oblicza procent postępu pobierania
     */
    private int calculateProgressPercent(long downloadedBytes) {
        if (job.getTotalBytes() <= 0) {
            return 0; // Nie można obliczyć procentów bez totalBytes
        }

        // Oblicz procent, ale nie przekraczaj 100%
        double percent = (downloadedBytes * 100.0) / job.getTotalBytes();
        int progress = (int) Math.min(percent, 100);

        // Zabezpieczenie przed ujemnymi wartościami
        return Math.max(progress, 0);
    }

}