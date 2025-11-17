package com.kcn.hikvisionmanager.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.kcn.hikvisionmanager.domain.RunningFfmpegProcess;
import com.kcn.hikvisionmanager.dto.stream.RunningStream;
import com.kcn.hikvisionmanager.exception.StreamAlreadyActiveException;
import com.kcn.hikvisionmanager.exception.StreamStartException;
import com.kcn.hikvisionmanager.service.ffmpeg.FFmpegCommandBuilder;
import com.kcn.hikvisionmanager.service.ffmpeg.FFmpegProcessHandler;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class CameraStreamService {

    private final CameraUrlBuilder urlBuilder;
    private final FFmpegCommandBuilder commandBuilder;
    private final FFmpegProcessHandler processHandler;
    @Qualifier("liveStreamCache")
    private final Cache<String, RunningStream> liveStreamCache; // with TTL 2h
    private final String baseStreamDir;


    public CameraStreamService(CameraUrlBuilder urlBuilder,
                               FFmpegCommandBuilder commandBuilder,
                               FFmpegProcessHandler processHandler,
                               @Qualifier("liveStreamCache") Cache<String,
                                       RunningStream> liveStreamCache,
                               @Value("${backup.temp-dir}") String basePath) {
        this.urlBuilder = urlBuilder;
        this.commandBuilder = commandBuilder;
        this.processHandler = processHandler;
        this.liveStreamCache = liveStreamCache;
        this.baseStreamDir = basePath;
    }

    /**
     * Startuje HLS stream dla podanego kanału i sesji.
     * Zwraca URL do playlisty (m3u8) który frontend używa w Hls.js.
     */
    public String startHlsStream(String channelId, String sessionId) {

        RunningStream existing = liveStreamCache.getIfPresent(sessionId);
        if (existing != null) {
            throw new StreamAlreadyActiveException(sessionId, channelId);
        }
        String rtspUrl = urlBuilder.buildStreamUrl(channelId);
        Path sessionDir = Path.of(baseStreamDir, sessionId);

        List<String> command = commandBuilder.buildHlsCommand(rtspUrl, sessionDir);

        try {
            RunningFfmpegProcess running =
                    processHandler.startStreamingProcess(command, sessionDir);

            RunningStream descriptor = new RunningStream(
                    channelId,
                    sessionId,
                    sessionDir,
                    running,
                    Instant.now()
            );

            liveStreamCache.put(sessionId, descriptor);

            String playlist = "/streams/" + sessionId + "/index.m3u8";

            log.info("📡 HLS stream started for session={} channel={} → {}", sessionId, channelId, playlist);

            return playlist;

        } catch (IOException e) {
            log.error("❌ Failed to start HLS stream for session {}", sessionId, e);
            throw new StreamStartException("Failed to start HLS stream: " + e.getMessage(), e);
        }
    }

    /**
     * Zatrzymuje stream dla danej sesji i usuwa pliki HLS.
     */
    public boolean stopHlsStream(String sessionId) {
        RunningStream stream = liveStreamCache.getIfPresent(sessionId);
        if (stream == null) {
            return false;
        }

        try {
            log.info("⛔ Stopping HLS stream for session {}", sessionId);

            processHandler.stopStreaming(stream.runningProcess());
            cleanupHlsFiles(stream.outputDir());
            liveStreamCache.invalidate(sessionId);

            log.info("✔️ HLS stream stopped for session {}", sessionId);

        } catch (Exception e) {
            log.warn("❌ Error stopping stream session={} – forcing cleanup", sessionId);
            cleanupHlsFiles(stream.outputDir());
            liveStreamCache.invalidate(sessionId);
        }
        return true;
    }

    /**
     * Status – dla endpointu GET /api/live/status
     */
    public Optional<RunningStream> getStreamStatus(String sessionId) {
        return Optional.ofNullable(liveStreamCache.getIfPresent(sessionId));
    }

    public int getViewerCount(String sessionId) {
        return liveStreamCache.getIfPresent(sessionId) != null ? 1 : 0;
    }

    @PreDestroy
    public void shutdownAllStreams() {
        log.info("🛑 Application shutdown - stopping all active streams");

        Map<String, RunningStream> activeStreams = liveStreamCache.asMap();

        activeStreams.forEach((sessionId, stream) -> {
            try {
                log.info("⛔ Shutting down stream for session: {}", sessionId);
                processHandler.stopStreaming(stream.runningProcess());
                cleanupHlsFiles(stream.outputDir());
            } catch (Exception e) {
                log.error("❌ Error stopping stream during shutdown: {}", sessionId, e);
            }
        });

        liveStreamCache.invalidateAll();
        log.info("✅ All streams stopped");
    }

    // -------------------------------
    //       INTERNAL HELPERS
    // -------------------------------

    private void stopIfExists(String sessionId) {
        RunningStream stream = liveStreamCache.getIfPresent(sessionId);
        if (stream != null) {
            stopHlsStream(sessionId);
        }
    }

    private void cleanupHlsFiles(Path sessionDir) {
        try {
            if (Files.exists(sessionDir)) {
                FileSystemUtils.deleteRecursively(sessionDir);
            }
        } catch (Exception e) {
            log.error("❌ Failed to cleanup session HLS directory {}", sessionDir);
        }
    }

}
