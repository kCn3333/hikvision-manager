package com.kcn.hikvisionmanager.service.backup;

import com.kcn.hikvisionmanager.entity.BackupConfigurationEntity;
import com.kcn.hikvisionmanager.entity.BackupJobEntity;
import com.kcn.hikvisionmanager.dto.BackupConfigDTO;
import com.kcn.hikvisionmanager.dto.BackupJobDTO;
import com.kcn.hikvisionmanager.exception.BackupNotFoundException;
import com.kcn.hikvisionmanager.mapper.BackupConfigMapper;
import com.kcn.hikvisionmanager.mapper.BackupJobMapper;
import com.kcn.hikvisionmanager.repository.BackupConfigurationRepository;
import com.kcn.hikvisionmanager.repository.BackupJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BackupService {

    private final BackupConfigurationRepository configRepository;
    private final BackupJobRepository jobRepository;
    private final BackupConfigMapper configMapper;
    private final BackupJobMapper jobMapper;
    private final BackupExecutor backupExecutor;

    /**
     * Create a new backup configuration
     */
    @Transactional
    public BackupConfigDTO createBackupConfig(BackupConfigDTO dto) {
        // Validation: ensure camera exists (this can be delegated to another service)
//        if (!cameraExists(dto.getCameraId())) {
//            throw new IllegalArgumentException("Camera with ID " + dto.getCameraId() + " does not exist");
//        }

        BackupConfigurationEntity entity = configMapper.toEntity(dto);
        BackupConfigurationEntity saved = configRepository.save(entity);

        log.info("🆕 Created new backup configuration: {}", entity.getName());
        return configMapper.toDTO(saved);
    }

    /**
     * Trigger immediate backup execution
     */
    @Transactional
    public String triggerBackup(String configId) {
        BackupConfigurationEntity config = configRepository.findById(configId)
                .orElseThrow(() -> new BackupNotFoundException("Backup configuration not found: " + configId));

        log.info("🔔 Triggering manual backup for config {}", config.getId());
        return backupExecutor.executeBackup(config);
    }

    /**
     * Get all backup configurations
     */
    @Transactional(readOnly = true)
    public List<BackupConfigDTO> getAllBackupConfigs() {
        return configRepository.findAll().stream()
                .map(configMapper::toDTO)
                .toList();
    }

    /**
     * Get a single backup configuration by ID
     */
    @Transactional(readOnly = true)
    public BackupConfigDTO getBackupConfig(String id) {
        return configRepository.findById(id)
                .map(configMapper::toDTO)
                .orElseThrow(() -> new BackupNotFoundException("Backup configuration not found: " + id));
    }

    /**
     * Update existing configuration
     */
    @Transactional
    public BackupConfigDTO updateBackupConfig(String id, BackupConfigDTO dto) {
        BackupConfigurationEntity existing = configRepository.findById(id)
                .orElseThrow(() -> new BackupNotFoundException("Backup configuration not found: " + id));

        existing.setName(dto.getName());
        existing.setCameraId(dto.getCameraId());
        existing.setRetentionDays(dto.getRetentionDays());
        existing.setEnabled(dto.isEnabled());
        existing.setCronExpression(configMapper.generateCron(dto));

        configRepository.save(existing);
        log.info("✏️ Updated backup configuration: {}", id);

        return configMapper.toDTO(existing);
    }

    /**
     * Delete backup configuration
     */
    @Transactional
    public void deleteBackupConfig(String id) {
        if (!configRepository.existsById(id)) {
            throw new BackupNotFoundException("Backup configuration not found: " + id);
        }
        configRepository.deleteById(id);
        log.info("🗑️ Deleted backup configuration: {}", id);
    }

    /**
     * Get all jobs for specific backup
     */
    @Transactional(readOnly = true)
    public List<BackupJobDTO> getBackupJobs(String id) {
        if (!configRepository.existsById(id)) {
            throw new BackupNotFoundException("Backup configuration not found: " + id);
        }

        List<BackupJobEntity> jobs = jobRepository.findByConfigIdOrderByStartedAtDesc(id);
        return jobs.stream()
                .map(jobMapper::toDTO)
                .toList();

    }

    /**
     * Get all jobs for all backups
     */
    @Transactional(readOnly = true)
    public List<BackupJobDTO> getAllBackupJobs() {
        List<BackupJobEntity> jobs = jobRepository.findAllByOrderByStartedAtDesc();
        return jobs.stream()
                .map(jobMapper::toDTO)
                .toList();
    }
    @Transactional(readOnly = true)
    public Page<BackupJobDTO> getAllBackupJobs(Pageable pageable) {
        Page<BackupJobEntity> jobsPage = jobRepository.findAllByOrderByStartedAtDesc(pageable);
        return jobsPage.map(jobMapper::toDTO);
    }


    private boolean cameraExists(String cameraId) {
        // TODO: check camera existence via CameraService or repository
        return true;
    }



}
