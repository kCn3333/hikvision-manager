package com.kcn.hikvisionmanager.controller;

import com.kcn.hikvisionmanager.domain.DownloadJob;
import com.kcn.hikvisionmanager.dto.*;
import com.kcn.hikvisionmanager.mapper.BatchDownloadJobMapper;
import com.kcn.hikvisionmanager.mapper.DownloadJobMapper;
import com.kcn.hikvisionmanager.repository.BatchDownloadJobRepository;
import com.kcn.hikvisionmanager.service.download.BatchDownloadService;
import com.kcn.hikvisionmanager.service.download.RecordingDownloadService;
import com.kcn.hikvisionmanager.service.RecordingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.nio.file.Path;
import java.util.*;

import static org.springframework.http.ResponseEntity.badRequest;

/**
 * REST Controller for recording downloads
 */
@RestController
@RequestMapping("/api/recordings/download")
@RequiredArgsConstructor
@Slf4j
public class RecordingDownloadController {

    private final RecordingDownloadService downloadService;
    private final RecordingService searchService;
    private final DownloadJobMapper downloadJobMapper;
    private final BatchDownloadService batchService;

    /**
     * Start download for a specific recording (after search)
     *
     * POST /api/recordings/download/start
     * Body: RecordingItemDTO (from search results)
     *
     * @return Batch ID for tracking
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, String>> startDownload(
            @Valid @RequestBody RecordingItemDTO recording) {

        log.info("🌐 API: POST /api/recordings/download/start - recordingId: {}",
                recording.getRecordingId());

        try {
            String batchId = batchService.startBatchDownload(List.of(recording));

            Map<String, String> response = new HashMap<>();
            response.put("batchId", batchId);
            response.put("statusUrl", "/api/recordings/download/batch/" + batchId + "/status");

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            log.error("❌ Failed to start download", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Start download for a multiple recordings
     *
     * POST /api/recordings/download/start/batch
     * Body: List<RecordingItemDTO> (from search results)
     *
     * @return Batch ID for tracking
     */
    @PostMapping("/start/batch")
    public ResponseEntity<Map<String, String>> startDownload(
            @Valid @RequestBody List<RecordingItemDTO> recordings) {

        log.debug("🌐 API: POST /api/recordings/download/start/batch - {} recordings",
                recordings.size());

        try {
            String batchId = batchService.startBatchDownload(recordings);

            Map<String, String> response = new HashMap<>();
            response.put("batchId", batchId);
            response.put("statusUrl", "/api/recordings/download/batch/" + batchId + "/status");

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            log.error("❌ Failed to start download", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Start download directly without search first (convenience endpoint)
     * Backend will search for recordings and download ALL matches as a batch
     *
     * POST /api/recordings/download/start-direct
     * Body: RecordingSearchRequestDTO (same as search endpoint)
     *
     * @return Batch ID for tracking all downloads
     */
    @PostMapping("/start-direct")
    public ResponseEntity<?> startDirectDownload(
            @Valid @RequestBody RecordingSearchRequestDTO request) {

        log.debug("🌐 API: POST /api/recordings/download/start-direct - range: {} to {}, page: {}",
                request.getStartTime(), request.getEndTime(), request.getPage());

        try {
            // Search for recordings
            RecordingSearchResultDTO searchResult = searchService.searchRecordings(request);

            if (searchResult.getRecordings().isEmpty()) {
                log.warn("❌ No recordings found for direct download request");
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "No recordings found in specified time range"));
            }

            // Start batch download for ALL recordings
            String batchId = batchService.startBatchDownload(searchResult.getRecordings());

            Map<String, Object> response = new HashMap<>();
            response.put("batchId", batchId);
            response.put("statusUrl", "/api/recordings/download/batch/" + batchId + "/status");
            response.put("total", searchResult.getRecordings().size());
            response.put("message", String.format("Started batch download of %d recordings",
                    searchResult.getRecordings().size()));

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            log.error("❌ Failed to start direct download", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    /**
     * Get download status (for polling)
     *
     * GET /api/recordings/download/{jobId}/status
     *
     * @return Current download status with progress
     */
    @GetMapping("/{jobId}/status")
    public ResponseEntity<DownloadStatusDTO> getDownloadStatus(@PathVariable String jobId) {

        log.debug("🌐 API: GET /api/recordings/download/{}/status", jobId);

        try {
            DownloadJob job = downloadService.getDownloadStatus(jobId);
            DownloadStatusDTO statusDTO = downloadJobMapper.toStatusDTO(job);

            return ResponseEntity.ok(statusDTO);

        } catch (IllegalArgumentException e) {
            log.error("❌ Download job not found: {}", jobId);
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            log.error("❌ Failed to get download status", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Download the completed file
     *
     * GET /api/recordings/download/{jobId}/file
     *
     * @return Video file as stream
     */
    @GetMapping("/{jobId}/file")
    public ResponseEntity<Resource> downloadFile(@PathVariable String jobId) {

        log.debug("🌐 API: GET /api/recordings/download/{}/file", jobId);

        try {
            Path filePath = downloadService.getDownloadFile(jobId);
            Resource resource = new FileSystemResource(filePath);

            String fileName = filePath.getFileName().toString();

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + fileName + "\"")
                    .body(resource);

        } catch (IllegalArgumentException e) {
            log.error("❌ Download job not found: {}", jobId);
            return ResponseEntity.notFound().build();

        } catch (IllegalStateException e) {
            log.error("❌ Download not ready: {}", e.getMessage());

            if (e.getMessage().contains("not completed")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            } else {
                return ResponseEntity.status(HttpStatus.GONE).build();
            }

        } catch (Exception e) {
            log.error("❌ Failed to download file", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Cancel a download
     *
     * DELETE /api/recordings/download/{jobId}/cancel
     */
    @DeleteMapping("/{jobId}/cancel")
    public ResponseEntity<Map<String, String>> cancelDownload(@PathVariable String jobId) {

        log.info("🌐 API: DELETE /api/recordings/download/{}/cancel", jobId);

        try {
            downloadService.cancelDownload(jobId);

            Map<String, String> response = new HashMap<>();
            response.put("jobId", jobId);
            response.put("message", "Download cancelled successfully");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("❌ Download job not found: {}", jobId);
            return ResponseEntity.notFound().build();

        } catch (IllegalStateException e) {
            log.error("❌ Cannot cancel download: {}", e.getMessage());
            return badRequest().build();

        } catch (Exception e) {
            log.error("❌ Failed to cancel download", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get batch download status
     *
     * GET /api/recordings/download/batch/{batchId}/status
     *
     * @return Batch status with all individual job statuses
     */
    @GetMapping("/batch/{batchId}/status")
    public ResponseEntity<BatchStatusDTO> getBatchStatus(@PathVariable String batchId) {

        log.debug("🌐 API: GET /api/recordings/download/batch/{}/status", batchId);

        try {
            BatchStatusDTO status = batchService.getBatchStatus(batchId);
            return ResponseEntity.ok(status);

        } catch (IllegalArgumentException e) {
            log.error("❌ Batch not found: {}", batchId);
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            log.error("❌ Failed to get batch status", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Cancel the entire batch download
     *
     * DELETE /api/recordings/download/batch/{batchId}/cancel
     */
    @DeleteMapping("/batch/{batchId}/cancel")
    public ResponseEntity<Map<String, String>> cancelBatch(@PathVariable String batchId) {

        log.info("🌐 API: DELETE /api/recordings/download/batch/{}/cancel", batchId);

        try {
            batchService.cancelBatch(batchId);

            Map<String, String> response = new HashMap<>();
            response.put("batchId", batchId);
            response.put("message", "Batch download cancelled successfully");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("❌ Batch not found: {}", batchId);
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            log.error("❌ Failed to cancel batch", e);
            return ResponseEntity.internalServerError().build();
        }
    }

}