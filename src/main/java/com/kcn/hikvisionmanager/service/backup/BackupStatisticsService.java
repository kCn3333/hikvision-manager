package com.kcn.hikvisionmanager.service.backup;

import com.kcn.hikvisionmanager.entity.BackupJobEntity;
import com.kcn.hikvisionmanager.domain.BackupJobStatus;
import com.kcn.hikvisionmanager.exception.BackupNotFoundException;
import com.kcn.hikvisionmanager.repository.BackupJobRepository;
import com.kcn.hikvisionmanager.repository.BackupRecordingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.kcn.hikvisionmanager.util.BackupStatsUtils.*;

/**
 * Service for backup statistics and reporting
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BackupStatisticsService {

    private final BackupJobRepository jobRepository;
    private final BackupRecordingRepository recordingRepository;

    /**
     * Get overall backup statistics
     */
    public Map<String, Object> getOverallStatistics() {
        Map<String, Object> stats = new HashMap<>();

        long totalBackups = jobRepository.count();
        long completedBackups = jobRepository.countCompletedBackups();
        long failedBackups = jobRepository.findByStatus(BackupJobStatus.FAILED).size();

        Long totalSize = jobRepository.getTotalBackupSize();
        if (totalSize == null) totalSize = 0L;

        long totalRecordings = recordingRepository.count();

        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        List<BackupJobEntity> recentBackups = jobRepository.findByDateRange(weekAgo, LocalDateTime.now());

        double successRate = calculateSuccessRate(completedBackups, totalBackups);

        long averageBackupSize = completedBackups > 0 ? totalSize / completedBackups : 0;

        stats.put("totalBackups", totalBackups);
        stats.put("completedBackups", completedBackups);
        stats.put("failedBackups", failedBackups);
        stats.put("successRate", successRate);
        stats.put("totalSizeBytes", totalSize);
        stats.put("totalSizeFormatted", formatBytes(totalSize));
        stats.put("totalRecordings", totalRecordings);
        stats.put("recentBackupsCount", recentBackups.size());
        stats.put("averageBackupSize", averageBackupSize);
        stats.put("averageBackupSizeFormatted", formatBytes(averageBackupSize));

        return stats;
    }


    /**
     * Get statistics for specific config
     */
    public Map<String, Object> getConfigStatistics(String configId) {
        Map<String, Object> stats = new HashMap<>();

        List<BackupJobEntity> jobs = jobRepository.findByConfigId(configId);
        if (jobs.isEmpty()) {
            throw new BackupNotFoundException("No backup jobs found for config: " + configId);
        }

        long completed = jobs.stream()
                .filter(j -> j.getStatus() == BackupJobStatus.COMPLETED)
                .count();

        long failed = jobs.stream()
                .filter(j -> j.getStatus() == BackupJobStatus.FAILED)
                .count();

        long totalSize = jobs.stream()
                .filter(j -> j.getStatus() == BackupJobStatus.COMPLETED)
                .mapToLong(j -> j.getTotalSizeBytes() != null ? j.getTotalSizeBytes() : 0)
                .sum();

        int totalRecordings = jobs.stream()
                .filter(j -> j.getStatus() == BackupJobStatus.COMPLETED)
                .mapToInt(BackupJobEntity::getCompletedRecordings)
                .sum();

        stats.put("configId", configId);
        stats.put("totalBackups", jobs.size());
        stats.put("completedBackups", completed);
        stats.put("failedBackups", failed);
        stats.put("successRate", calculateSuccessRate(completed, jobs.size()));
        stats.put("totalSizeBytes", totalSize);
        stats.put("totalSizeFormatted", formatBytes(totalSize));
        stats.put("totalRecordings", totalRecordings);
        stats.put("averageRecordingsPerBackup",
                completed > 0 ? totalRecordings / (double) completed : 0);

        return stats;
    }

    /**
     * Get detailed job statistics
     */
    public Map<String, Object> getJobStatistics(String jobId) {

        BackupJobEntity job = jobRepository.findById(jobId)
                .orElseThrow(() -> new BackupNotFoundException("Backup job not found: " + jobId));

        Map<String, Object> stats = new HashMap<>();

        // Basic info
        stats.put("jobId", job.getId());
        stats.put("configId", job.getConfigId());
        stats.put("status", job.getStatus());
        stats.put("startedAt", job.getStartedAt());
        stats.put("completedAt", job.getCompletedAt());
        stats.put("searchStartTime", job.getSearchStartTime());
        stats.put("searchEndTime", job.getSearchEndTime());

        // Recordings info
        stats.put("totalRecordings", job.getTotalRecordings());
        stats.put("completedRecordings", job.getCompletedRecordings());
        stats.put("failedRecordings", job.getFailedRecordings());
        stats.put("pendingRecordings",
                job.getTotalRecordings() - job.getCompletedRecordings() - job.getFailedRecordings());

        // Size info
        stats.put("totalSizeBytes", job.getTotalSizeBytes());
        stats.put("totalSizeFormatted", formatBytes(job.getTotalSizeBytes() != null ? job.getTotalSizeBytes() : 0));
        stats.put("averageFileSize",
                job.getCompletedRecordings() > 0 && job.getTotalSizeBytes() != null
                        ? job.getTotalSizeBytes() / job.getCompletedRecordings()
                        : 0);

        // Duration
        if (job.getCompletedAt() != null) {
            long durationMinutes = java.time.Duration.between(
                    job.getStartedAt(), job.getCompletedAt()).toMinutes();
            stats.put("durationMinutes", durationMinutes);
            stats.put("durationFormatted", formatDuration(durationMinutes));
        }

        stats.put("backupDirectory", job.getBackupDirectory());
        stats.put("errorMessage", job.getErrorMessage());

        return stats;
    }


}