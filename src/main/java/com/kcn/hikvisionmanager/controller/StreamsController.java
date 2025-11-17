package com.kcn.hikvisionmanager.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/streams")
@Slf4j
public class StreamsController {

    private final Path baseStreamDir;

    public StreamsController(@Value("${backup.temp-dir}") String basePath) {
        this.baseStreamDir = Paths.get(basePath);
        log.info("🎬 HttpStreamsController initialized. Base dir: {}", this.baseStreamDir.toAbsolutePath());
    }

    @GetMapping("/{sessionId}/{filename:.+}")
    public ResponseEntity<Resource> serveFile(
            @PathVariable String sessionId,
            @PathVariable String filename) {

        log.debug("📥 Request: GET /streams/{}/{}", sessionId, filename);

        Path filePath = baseStreamDir.resolve(sessionId).resolve(filename);
        File file = filePath.toFile();

        // log.debug("🔍 Looking for file: {}", filePath.toAbsolutePath());
        // log.debug("🔍 File exists: {}", file.exists());

        if (!file.exists()) {
            // Debug for dev mode
            if (log.isDebugEnabled()) {
                log.warn("❌ File not found: {}", filePath.toAbsolutePath());
                Path sessionDir = baseStreamDir.resolve(sessionId);
                if (Files.exists(sessionDir)) {
                    try {
                        log.debug("📂 Session directory exists. Contents:");
                        Files.list(sessionDir).forEach(p ->
                                log.debug("   📄 {}", p.getFileName())
                        );
                    } catch (Exception e) {
                        log.error("Failed to list directory", e);
                    }
                } else {
                    log.debug("❌ Session directory does not exist: {}", sessionDir.toAbsolutePath());
                }
            }

            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        String contentType = filename.endsWith(".m3u8")
                ? "application/vnd.apple.mpegurl"
                : "video/mp2t";

        Resource resource = new FileSystemResource(file);

        log.debug("✅ Serving file: {} ({} bytes, type: {})",
                filename, file.length(), contentType);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.EXPIRES, "0")
                .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                .body(resource);
    }
}