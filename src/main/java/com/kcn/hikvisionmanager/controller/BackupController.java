package com.kcn.hikvisionmanager.controller;

import com.kcn.hikvisionmanager.dto.BackupConfigDTO;
import com.kcn.hikvisionmanager.dto.BackupJobDTO;
import com.kcn.hikvisionmanager.service.backup.BackupService;
import com.kcn.hikvisionmanager.service.backup.BackupStatisticsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for backup management
 */
@Validated
@RestController
@RequestMapping("/api/backups")
@RequiredArgsConstructor
@Slf4j
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class BackupController {

    private final BackupStatisticsService statisticsService;
    private final BackupService backupService;


    // ========== Configuration Management ==========

    /**
     * Create a new backup configuration
     * POST /api/backups/config
     */
    @PostMapping("/config")
    public ResponseEntity<BackupConfigDTO> createConfig(
            @Valid @RequestBody BackupConfigDTO config) {

        log.info("🌐 API: POST /api/backups/config - name: {}", config.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(backupService.createBackupConfig(config));
    }

    /**
     * Get all backup configurations
     * GET /api/backups/config
     */
    @GetMapping("/config")
    public ResponseEntity<List<BackupConfigDTO>> getAllConfigs() {
        log.debug("🌐 API: GET /api/backups/config");
        return ResponseEntity.ok(backupService.getAllBackupConfigs());
    }

    /**
     * Get a specific backup configuration
     * GET /api/backups/config/{id}
     */
    @GetMapping("/config/{id}")
    public ResponseEntity<BackupConfigDTO> getConfig(@PathVariable String id) {
        log.debug("🌐 API: GET /api/backups/config/{}", id);
        return ResponseEntity.ok(backupService.getBackupConfig(id));
    }

    /**
     * Update backup configuration
     * PUT /api/backups/config/{id}
     */
    @PutMapping("/config/{id}")
    public ResponseEntity<BackupConfigDTO> updateConfig(
            @PathVariable String id,
            @Valid @RequestBody BackupConfigDTO config) {

        log.debug("🌐 API: PUT /api/backups/config/{}", id);
        return ResponseEntity.ok(backupService.updateBackupConfig(id, config));
    }

    /**
     * Delete backup configuration
     * DELETE /api/backups/config/{id}
     */
    @DeleteMapping("/config/{id}")
    public ResponseEntity<String> deleteConfig(@PathVariable String id) {
        log.debug("🌐 API: DELETE /api/backups/config/{}", id);
        backupService.deleteBackupConfig(id);
        return ResponseEntity.ok("Backup configuration deleted successfully");
    }

    // ========== Backup Execution ==========
    /**
     * Trigger backup manually
     * POST /api/backups/execute/{configId}
     */
    @PostMapping("/execute/{configId}")
    public ResponseEntity<Map<String, String>> executeBackup(@PathVariable String configId) {
        String batchId = backupService.triggerBackup(configId);
        return ResponseEntity.ok(Map.of("batchId", batchId));
    }

    // ========== Job History ==========

    /**
     * Get all backup jobs (for all configurations) with pagination
     * GET /api/backups/jobs?page=0&size=10&sort=startedAt,desc
     */
    @GetMapping("/jobs")
    public ResponseEntity<Page<BackupJobDTO>> getAllBackupJobs(
            @PageableDefault(size = 10, sort = "startedAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.debug("🌐 API: GET /api/backups/jobs - page: {}, size: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        Page<BackupJobDTO> jobsPage = backupService.getAllBackupJobs(pageable);
        return ResponseEntity.ok(jobsPage);
    }

    /**
     * Get all backup jobs for specific configuration
     * GET /api/backups/{configId}/jobs
     */
    @GetMapping("/{configId}/jobs")
    public ResponseEntity<List<BackupJobDTO>> getBackupJobs(@PathVariable String id) {
        log.debug("🌐 API: GET /api/backups/jobs/{}", id);

        return ResponseEntity.ok(backupService.getBackupJobs(id));
    }

//    /**
//     * Get recordings for a specific backup job
//     * GET /api/backups/jobs/{id}/recordings
//     */
//    @GetMapping("/jobs/{id}/recordings")
//    public ResponseEntity<List<BackupRecordingEntity>> getJobRecordings(@PathVariable String id) {
//        log.info("🌐 API: GET /api/backups/jobs/{}/recordings", id);
//
//       return ResponseEntity.ok(recordings);
//    }


}