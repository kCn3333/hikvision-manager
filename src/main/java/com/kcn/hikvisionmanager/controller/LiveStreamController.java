package com.kcn.hikvisionmanager.controller;

import com.kcn.hikvisionmanager.dto.stream.StreamApiResponse;
import com.kcn.hikvisionmanager.dto.stream.StreamStatusResponse;
import com.kcn.hikvisionmanager.service.CameraStreamService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/live")
@Slf4j
@RequiredArgsConstructor
public class LiveStreamController {

    private final CameraStreamService streamService;

    @PostMapping("/start")
    public ResponseEntity<StreamApiResponse> startStream(
            @RequestParam String channel,
            HttpSession httpSession) {

        String sessionId = httpSession.getId();

        log.debug("🌐 API: POST /api/live/start - channel: {}, session: {}", channel, sessionId);

        String playlistUrl = streamService.startHlsStream(channel, sessionId);
        log.debug("▶️ Stream started successfully. Playlist: {}", playlistUrl);

        return ResponseEntity.ok(StreamApiResponse.builder()
                .status("success")
                .playlistUrl(playlistUrl)
                .message("Stream started successfully")
                .build());
    }

    @PostMapping("/stop")
    public ResponseEntity<StreamApiResponse> stopStream(HttpSession httpSession) {

        String sessionId = httpSession.getId();

        log.debug("🌐 API: POST /api/live/stop - session: {}", sessionId);

        boolean stopped = streamService.stopHlsStream(sessionId);
        log.debug("✅ Stream stop result: {}", stopped);

        return ResponseEntity.ok(
                StreamApiResponse.builder()
                        .status("success")
                        .message(stopped ? "Stream stopped" : "No active stream found")
                        .build()
        );
    }

    @GetMapping("/status")
    public ResponseEntity<StreamStatusResponse> getStatus(HttpSession httpSession) {

        String sessionId = httpSession.getId();

        log.debug("🌐 API: GET /api/live/status - session: {}", sessionId);

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