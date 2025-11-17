package com.kcn.hikvisionmanager.controller;

import com.kcn.hikvisionmanager.service.backup.BackupStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST Controller for backup statistics and reports
 */
@RestController
@RequestMapping("/api/backups/statistics")
@RequiredArgsConstructor
@Slf4j
public class BackupStatisticsController {

    private final BackupStatisticsService statisticsService;

    /**
     * Get overall backup statistics
     * GET /api/backups/statistics
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getOverallStatistics() {
        log.debug("🌐 API: GET /api/backups/statistics");
        return ResponseEntity.ok(statisticsService.getOverallStatistics());
    }

    /**
     * Get statistics for a specific configuration
     * GET /api/backups/statistics/config/{configId}
     */
    @GetMapping("/config/{configId}")
    public ResponseEntity<Map<String, Object>> getConfigStatistics(@PathVariable String configId) {
        log.debug("🌐 API: GET /api/backups/statistics/config/{}", configId);
        return ResponseEntity.ok(statisticsService.getConfigStatistics(configId));
    }

    /**
     * Get detailed statistics for specific job
     * GET /api/backups/statistics/job/{jobId}
     */
    @GetMapping("/job/{jobId}")
    public ResponseEntity<Map<String, Object>> getJobStatistics(@PathVariable String jobId) {
        log.debug("🌐 API: GET /api/backups/statistics/job/{}", jobId);
        return ResponseEntity.ok(statisticsService.getJobStatistics(jobId));
    }
}
