package com.kcn.hikvisionmanager.controller;

import com.kcn.hikvisionmanager.dto.RecordingSearchRequestDTO;
import com.kcn.hikvisionmanager.dto.RecordingSearchResultDTO;
import com.kcn.hikvisionmanager.service.RecordingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/recordings")
@RequiredArgsConstructor
public class RecordingController {

    private final RecordingService recordingService;
    private static final String DEFAULT_PAGE_SIZE = "10";
    private static final String DEFAULT_HOURS = "24";

    /**
     * POST /api/recordings/search
     * Searches for recordings in a given time range.
     */
    @PostMapping("/search")
    public ResponseEntity<RecordingSearchResultDTO> searchRecordings(
            @Valid @RequestBody RecordingSearchRequestDTO request) {

        log.debug("🌐 API: POST /api/recordings/search | startTime={} | endTime={} | page={} | pageSize={}",
                request.getStartTime(), request.getEndTime(), request.getPage(), request.getPageSize());

        return ResponseEntity.ok(recordingService.searchRecordings(request));
    }

    /**
     * GET /api/recordings/recent
     * Returns recent recordings from the last N hours.
     */
    @GetMapping("/recent")
    public ResponseEntity<RecordingSearchResultDTO> getRecentRecordings(
            @RequestParam(defaultValue = DEFAULT_HOURS) int hours,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) int pageSize) {

        log.debug("🌐 API: GET /api/recordings/recent | hours={} | pageSize={}", hours, pageSize);
        return ResponseEntity.ok(recordingService.searchRecentRecordings(hours, pageSize));
    }

    /**
     * GET /api/recordings/date/{date}
     * Returns all recordings for a specific date.
     */
    @GetMapping("/date/{date}")
    public ResponseEntity<RecordingSearchResultDTO> getRecordingsByDate(
            @PathVariable
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) int pageSize) {

        log.debug("🌐 API: GET /api/recordings/date/{} | pageSize={}", date, pageSize);
        return ResponseEntity.ok(recordingService.searchRecordingsByDate(date, pageSize));
    }

    /**
     * POST /api/recordings/search/next
     * Fetches the next page of recordings based on the previous search result.
     */
    @PostMapping("/search/next")
    public ResponseEntity<RecordingSearchResultDTO> searchNextPage(
            @Valid @RequestBody RecordingSearchResultDTO currentResult,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) int pageSize) {

        log.debug("🌐 API: POST /api/recordings/search/next | currentSearchId={} | pageSize={}",
                currentResult.getSearchId(), pageSize);

        return ResponseEntity.ok(recordingService.searchNextPage(currentResult, pageSize));
    }
}