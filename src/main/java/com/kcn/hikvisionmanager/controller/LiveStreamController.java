package com.kcn.hikvisionmanager.controller;

import com.kcn.hikvisionmanager.dto.stream.StreamApiResponse;
import com.kcn.hikvisionmanager.dto.stream.StreamStatusResponse;
import com.kcn.hikvisionmanager.exception.SessionNotFoundException;
import com.kcn.hikvisionmanager.exception.StreamAlreadyActiveException;
import com.kcn.hikvisionmanager.exception.StreamStartException;
import com.kcn.hikvisionmanager.service.CameraStreamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/live")
@Slf4j
@RequiredArgsConstructor
public class LiveStreamController {

    private final CameraStreamService streamService;

    /**
     * Start live stream
     * POST /api/live/start?channel={channelId}
     */
    @PostMapping("/start")
    public ResponseEntity<StreamApiResponse> startStream(
            @RequestParam String channel,
            @CookieValue(value = "sessionId", required = true) String sessionId) {

        log.info("🌐 API: POST /api/live/start - channel: {}, session: {}", channel, sessionId);

        // Validation sessionId
        if (sessionId == null || sessionId.isBlank()) {
            log.warn("⚠️ Missing sessionId cookie");
            throw new SessionNotFoundException("Session ID not found. Please refresh the page.");
        }

        try {
            String playlistUrl = streamService.startHlsStream(channel, sessionId);
            log.info("▶️ Stream started successfully. Playlist: {}", playlistUrl);

            return ResponseEntity.ok(StreamApiResponse.builder()
                    .status("success")
                    .playlistUrl(playlistUrl)
                    .message("Stream started successfully")
                    .build());

        } catch (StreamAlreadyActiveException ex) {
            // GlobalExceptionHandler -> 409 Conflict
            throw ex;

        } catch (StreamStartException ex) {
            // GlobalExceptionHandler -> 500
            throw ex;
        }
    }

    @PostMapping("/stop")
    public ResponseEntity<StreamApiResponse> stopStream(
            @CookieValue(value = "sessionId", required = true) String sessionId) {

        log.info("🌐 API: POST /api/live/stop - session: {}", sessionId);

        if (sessionId == null || sessionId.isBlank()) {
            return ResponseEntity.ok(
                    StreamApiResponse.builder()
                            .status("success")
                            .message("No active stream to stop.")
                            .build()
            );
        }

        boolean stopped = streamService.stopHlsStream(sessionId);
        log.info("✅ Stream stop result: {}", stopped);

        return ResponseEntity.ok(
                StreamApiResponse.builder()
                        .status("success")
                        .message(stopped ? "Stream stopped" : "No active stream found")
                        .build()
        );
    }

    @GetMapping("/status")
    public ResponseEntity<StreamStatusResponse> getStatus(
            @CookieValue(value = "sessionId", required = true) String sessionId) {

        log.debug("🌐 API: GET /api/live/status - session: {}", sessionId);

        if (sessionId == null || sessionId.isBlank()) {
            log.debug("No sessionId provided, returning inactive status");
            return ResponseEntity.ok(StreamStatusResponse.empty());
        }

        return streamService.getStreamStatus(sessionId)
                .map(stream -> ResponseEntity.ok(
                        StreamStatusResponse.builder()
                                .active(true)
                                .channel(stream.channelId())
                                .startTime(stream.startTime().toString())
                                .viewers(streamService.getViewerCount(sessionId))
                                .build()
                ))
                .orElseGet(() -> ResponseEntity.ok(StreamStatusResponse.empty()));
    }
}