package com.kcn.hikvisionmanager.service.backup;

import com.kcn.hikvisionmanager.config.BackupConfig;
import com.kcn.hikvisionmanager.domain.*;
import com.kcn.hikvisionmanager.dto.RecordingItemDTO;
import com.kcn.hikvisionmanager.dto.RecordingSearchRequestDTO;
import com.kcn.hikvisionmanager.dto.RecordingSearchResultDTO;
import com.kcn.hikvisionmanager.entity.BackupConfigurationEntity;
import com.kcn.hikvisionmanager.entity.BackupJobEntity;
import com.kcn.hikvisionmanager.entity.BackupRecordingEntity;
import com.kcn.hikvisionmanager.exception.BackupExecutionException;
import com.kcn.hikvisionmanager.exception.StorageException;
import com.kcn.hikvisionmanager.repository.BackupConfigurationRepository;
import com.kcn.hikvisionmanager.repository.BackupJobRepository;
import com.kcn.hikvisionmanager.repository.BackupRecordingRepository;
import com.kcn.hikvisionmanager.service.download.BatchDownloadService;
import com.kcn.hikvisionmanager.service.RecordingService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.kcn.hikvisionmanager.util.FileNameUtils.*;
import static com.kcn.hikvisionmanager.util.ProgressCalculator.formatBytes;

/**
 * Core service for executing and orchestration backup jobs.
 * Handles backup lifecycle: search, download, finalization, and retention.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BackupExecutor {

    private final RecordingService recordingService;
    private final BatchDownloadService batchDownloadService;
    private final BackupConfigurationRepository backupConfigurationRepository;
    private final BackupJobRepository backupJobRepository;
    private final BackupRecordingRepository backupRecordingRepository;
    private final BackupConfig backupConfig;


    /**
     * Execute backup for the given configuration.
     *
     * @param config Backup configuration
     * @return Backup job ID
     * @throws BackupExecutionException if backup execution fails
     */
    @Transactional
    public String executeBackup(BackupConfigurationEntity config) {
        log.debug("🚀 Starting backup execution for config: {} ({})", config.getId(), config.getName());

        String backupJobId = "backup-" + UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        try {
            // Get time range strategy (with default for backward compatibility)
            BackupTimeRangeStrategy strategy = config.getTimeRangeStrategy() != null
                    ? config.getTimeRangeStrategy()
                    : backupConfig.getDefaultStrategy();

            // Calculate date range based on strategy
            BackupDateRange backupDateRange = calculateDateRange(strategy, now);
            log.debug("📅 Backup time range: {} to {} (strategy: {})",
                    backupDateRange.start(), backupDateRange.end(), strategy);

            // Validate backup directory
            String backupDir = buildBackupDirectory(config.getBackupPath(), backupDateRange.start());
            validateBackupDirectory(backupDir);

            // Create backup job in DB
            BackupJobEntity backupJob = createBackupJob(
                    backupJobId, config.getId(), now, backupDateRange, backupDir);

            // Search for recordings
            RecordingSearchResultDTO searchResult = searchRecordings(
                    backupDateRange.start(), backupDateRange.end());

            if (searchResult.getRecordings().isEmpty()) {
                return handleNoRecordingsFound(backupJob, backupDateRange);
            }

            // Warn if max limit reached
            if (searchResult.getRecordings().size() >= backupConfig.getMaxRecordingsPerBackup()) {
                log.warn("⚠️ Recording limit reached: {} recordings found (limit: {}). " +
                                "Some recordings may not be backed up.",
                        searchResult.getRecordings().size(), backupConfig.getMaxRecordingsPerBackup());
            }

            // Prepare backup recordings
            prepareBackupRecordings(backupJobId, searchResult.getRecordings());

            // Start batch download
            startBatchDownload(backupJob, searchResult.getRecordings(), backupDir);

            // Update config last run time
            updateConfigLastRun(config, now);

            return backupJobId;

        } catch (BackupExecutionException e) {
            throw e; // Re-throw custom exceptions
        } catch (Exception e) {
            handleBackupFailure(backupJobId, e);
            throw new BackupExecutionException(
                    "Backup execution failed: " + e.getMessage(),
                    e,
                    backupJobId,
                    BackupJobStatus.FAILED
            );
        }
    }

    /**
     * Calculate date range based on backup strategy.
     *
     * @param strategy Time range strategy
     * @param now Current date/time
     * @return BackupDateRange with start and end times
     */
    private BackupDateRange calculateDateRange(BackupTimeRangeStrategy strategy, LocalDateTime now) {
        return switch (strategy) {
            case LAST_HOUR -> new BackupDateRange(
                    now.minusHours(1),
                    now
            );
            case LAST_24_HOURS -> new BackupDateRange(
                    now.minusHours(24),
                    now
            );
            case PREVIOUS_DAY -> new BackupDateRange(
                    now.minusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0),
                    now.minusDays(1).withHour(23).withMinute(59).withSecond(59).withNano(999999999)
            );
        };
    }

    /**
     * Validate backup directory exists and is writable.
     */
    private void validateBackupDirectory(String backupDir) {
        try {
            Path path = Paths.get(backupDir);

            // Create directory if it doesn't exist
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                log.debug("✅ Created backup directory: {}", backupDir);
            }

            // Check if writable
            if (!Files.isWritable(path)) {
                throw new StorageException("Backup directory is not writable: " + backupDir);
            }

        } catch (Exception e) {
            throw new StorageException("Failed to validate backup directory: " + backupDir, e);
        }
    }

    /**
     * Create backup job entity in database.
     */
    private BackupJobEntity createBackupJob(
            String backupJobId,
            String configId,
            LocalDateTime now,
            BackupDateRange backupDateRange,
            String backupDir) {

        BackupJobEntity backupJob = BackupJobEntity.builder()
                .id(backupJobId)
                .configId(configId)
                .status(BackupJobStatus.STARTED)
                .startedAt(now)
                .searchStartTime(backupDateRange.start())
                .searchEndTime(backupDateRange.end())
                .totalRecordings(0)
                .completedRecordings(0)
                .failedRecordings(0)
                .totalSizeBytes(0L)
                .backupDirectory(backupDir)
                .build();

        backupJobRepository.save(backupJob);
        log.info("✅ Backup job created in DB: {}", backupJobId);

        return backupJob;
    }

    /**
     * Handle case when no recordings are found.
     */
    private String handleNoRecordingsFound(BackupJobEntity backupJob, BackupDateRange backupDateRange) {
        log.warn("⚠️ No recordings found for backup period: {} to {}",
                backupDateRange.start(), backupDateRange.end());

        backupJob.setStatus(BackupJobStatus.COMPLETED);
        backupJob.setCompletedAt(LocalDateTime.now());
        backupJobRepository.save(backupJob);

        return backupJob.getId();
    }

    /**
     * Prepare backup recording entries in database.
     */
    private void prepareBackupRecordings(String backupJobId, List<RecordingItemDTO> recordings) {
        log.debug("📊 Found {} recordings for backup", recordings.size());

        for (RecordingItemDTO recording : recordings) {
            BackupRecordingEntity backupRecordingEntity = BackupRecordingEntity.builder()
                    .backupJobId(backupJobId)
                    .recordingId(recording.getRecordingId())
                    .trackId(recording.getTrackId())
                    .startTime(recording.getStartTime())
                    .endTime(recording.getEndTime())
                    .duration(recording.getDuration())
                    .fileName(generateFileName(recording))
                    .status(BackupRecordingStatus.QUEUED)
                    .build();

            backupRecordingRepository.save(backupRecordingEntity);
        }

        log.debug("✅ Created {} backup recording entries in DB", recordings.size());
    }

    /**
     * Start batch download process.
     */
    private void startBatchDownload(
            BackupJobEntity backupJob,
            List<RecordingItemDTO> recordings,
            String backupDir) {

        backupJob.setStatus(BackupJobStatus.DOWNLOADING);
        backupJob.setTotalRecordings(recordings.size());
        backupJobRepository.save(backupJob);

        Path backupPath = Paths.get(backupDir);
        String batchId = batchDownloadService.startBatchDownload(
                recordings,
                backupPath,
                backupJob.getId()
        );

        log.info("🚀 Batch download started: {} (files will be saved to {})", batchId, backupDir);
    }

    /**
     * Update configuration's last run timestamp.
     */
    private void updateConfigLastRun(BackupConfigurationEntity config, LocalDateTime now) {
        config.setLastRunAt(now);
        backupConfigurationRepository.save(config);
    }

    /**
     * Handle backup execution failure.
     */
    private void handleBackupFailure(String backupJobId, Exception e) {
        log.error("❌ Backup execution failed for job {}: {}", backupJobId, e.getMessage(), e);

        BackupJobEntity job = backupJobRepository.findById(backupJobId).orElse(null);
        if (job != null) {
            job.setStatus(BackupJobStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            job.setCompletedAt(LocalDateTime.now());
            backupJobRepository.save(job);
        }
    }

    /**
     * Finalize backup - update stats and apply retention.
     *
     * @param backupJobId Backup job ID
     */
    @Transactional
    public void finalizeBackup(String backupJobId) {
        BackupJobEntity backupJob = backupJobRepository.findById(backupJobId).orElse(null);

        if (backupJob == null) {
            log.warn("⚠️ BackupJob not found: {}", backupJobId);
            return;
        }

        List<BackupRecordingEntity> recordings = backupRecordingRepository
                .findByBackupJobId(backupJobId);

        long completed = recordings.stream()
                .filter(r -> r.getStatus() == BackupRecordingStatus.COMPLETED)
                .count();

        long failed = recordings.stream()
                .filter(r -> r.getStatus() == BackupRecordingStatus.FAILED)
                .count();

        long totalSize = recordings.stream()
                .filter(r -> r.getStatus() == BackupRecordingStatus.COMPLETED)
                .filter(r -> r.getFileSizeBytes() != null)
                .mapToLong(BackupRecordingEntity::getFileSizeBytes)
                .sum();

        // Update backup job
        backupJob.setStatus(BackupJobStatus.COMPLETED);
        backupJob.setCompletedAt(LocalDateTime.now());
        backupJob.setCompletedRecordings((int) completed);
        backupJob.setFailedRecordings((int) failed);
        backupJob.setTotalSizeBytes(totalSize);
        backupJobRepository.save(backupJob);

        log.info("🎉 Backup finalized: {} ({}/{} recordings, {} total)",
                backupJobId, completed, backupJob.getTotalRecordings(),
                formatBytes(totalSize));

        // Apply retention policy
        applyRetentionPolicy(backupJob.getConfigId());
    }

    /**
     * Apply retention policy - delete old backups.
     *
     * @param configId Configuration ID
     */
    @Transactional
    public void applyRetentionPolicy(String configId) {
        BackupConfigurationEntity config = backupConfigurationRepository.findById(configId)
                .orElseThrow(() -> new IllegalArgumentException("Config not found: " + configId));

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(config.getRetentionDays());

        List<BackupJobEntity> oldJobs = backupJobRepository
                .findByConfigIdOrderByStartedAtDesc(configId).stream()
                .filter(job -> job.getStartedAt().isBefore(cutoffDate))
                .toList();

        if (oldJobs.isEmpty()) {
            log.debug("No old backups to delete for config: {}", configId);
            return;
        }

        log.info("🗑️ Applying retention policy: deleting {} old backups (older than {} days)",
                oldJobs.size(), config.getRetentionDays());

        for (BackupJobEntity oldJob : oldJobs) {
            deleteOldBackup(oldJob);
        }
    }

    /**
     * Delete old backup job and its files.
     */
    private void deleteOldBackup(BackupJobEntity oldJob) {
        try {
            // Delete files from disk
            Path backupDir = Paths.get(oldJob.getBackupDirectory());
            if (Files.exists(backupDir)) {
                deleteDirectory(backupDir);
                log.info("🗑️ Deleted backup directory: {}", backupDir);
            }

            // Delete from DB (cascade will delete recordings)
            backupJobRepository.delete(oldJob);
            log.info("🗑️ Deleted backup job from DB: {}", oldJob.getId());

        } catch (Exception e) {
            log.error("Failed to delete old backup {}: {}", oldJob.getId(), e.getMessage(), e);
            // Don't throw - continue with other deletions
        }
    }

    /**
     * Search for recordings in time range.
     *
     * @param startTime Start time
     * @param endTime End time
     * @return Search results
     */
    private RecordingSearchResultDTO searchRecordings(LocalDateTime startTime, LocalDateTime endTime) {
        RecordingSearchRequestDTO request = RecordingSearchRequestDTO.builder()
                .startTime(startTime)
                .endTime(endTime)
                .page(1)
                .pageSize(backupConfig.getMaxRecordingsPerBackup())
                .build();

        return recordingService.searchRecordings(request);
    }
}