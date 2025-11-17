package com.kcn.hikvisionmanager.service.ffmpeg;

import com.kcn.hikvisionmanager.domain.RunningFfmpegProcess;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Future;

@Slf4j
@Component
public class FFmpegProcessHandler {

    private final ThreadPoolTaskExecutor streamTaskExecutor;

    public FFmpegProcessHandler(@Qualifier("streamTaskExecutor") ThreadPoolTaskExecutor  streamTaskExecutor) {
        this.streamTaskExecutor = streamTaskExecutor;
    }

    /**
     * Startuje proces FFmpeg i zwraca uchwyt (Future + Process)
     */
    public RunningFfmpegProcess startStreamingProcess(List<String> command,
                                                      Path outputDir) throws IOException {

        // Utwórz katalog docelowy jeśli nie istnieje
        Files.createDirectories(outputDir);
        log.debug("✅ Directory created/exists: {}", Files.exists(outputDir));

        // Wyczyść poprzednie HLS jeśli istnieje
        if (Files.exists(outputDir)) {
            log.debug("🧹 Cleaning previous stream files...");
            FileSystemUtils.deleteRecursively(outputDir.toFile());
        }
        Files.createDirectories(outputDir);
        log.debug("✅ Clean directory ready: {}", Files.exists(outputDir));

        log.info("▶️ Starting FFmpeg stream process in: {}", outputDir);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(false); // stderr musi być czytane, inaczej FFmpeg może zawisnąć

        Process process = pb.start();

        // Asynchroniczne czytanie STDERR aby uniknąć deadlocka
        Future<?> stderrReader = streamTaskExecutor.submit(
                () -> consumeStderr(process, outputDir)
        );

        //checkFilesCreated(outputDir);

        return new RunningFfmpegProcess(process, stderrReader);
    }

    /**
     * Czytanie stderr FFmpeg – nie parsujemy, tylko odciążamy bufor.
     */
    private void consumeStderr(Process process, Path outputDir) {
        log.info("📊 Starting to read FFmpeg stderr...");

        try (BufferedReader reader =
                     new BufferedReader(new InputStreamReader(process.getErrorStream()))) {

            String line;
            int lineCount = 0;
            while ((line = reader.readLine()) != null) {
                lineCount++;

                // Loguj ważne informacje
                if (line.contains("[hls") || line.contains("segment") || line.contains(".ts")) {
                    log.trace("🎬 FFmpeg HLS: {}", line);
                }
                else if (line.contains("Error") || line.contains("Failed")) {
                    // Ignoruj błędy związane z "No such file or directory" - to normalne przy stop
                    if (line.contains("No such file or directory")) {
                        log.trace("⚠️ FFmpeg expected error during shutdown: {}", line);
                    } else {
                        log.error("❌ FFmpeg ERROR: {}", line);
                    }
                }
                else if (line.contains("frame=")) {
                    log.trace("📈 FFmpeg stats: {}", line); // TRACE - bardzo verbose
                }

                if (lineCount % 200 == 0) {
                    log.debug("📊 FFmpeg stderr lines processed: {}", lineCount);
                }
            }

            log.debug("✅ FFmpeg stderr reading finished. Total lines: {}", lineCount);

        } catch (IOException e) {
            // Nie rzucaj exception - to może być normalne przy shutdown
            log.debug("⚠️ FFmpeg stderr stream closed: {}", e.getMessage());
        }
    }

    /**
     * Zatrzymuje proces FFmpeg i doprowadza do pełnego cleanupu
     */
    public void stopStreaming(RunningFfmpegProcess running) {

        if (running == null || running.process() == null)
            return;

        Process process = running.process();

        try {
            log.info("⛔ Stopping FFmpeg streaming process…");

            process.destroy();

            boolean exited = process.waitFor(1500, java.util.concurrent.TimeUnit.MILLISECONDS);

            if (!exited) {
                log.warn("FFmpeg did not exit gracefully, forcing kill");
                process.destroyForcibly();
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }

        // przerwij wątek czytający stderr
        if (running.stderrReader() != null)
            running.stderrReader().cancel(true);

        log.info("✔️ FFmpeg streaming stopped");
    }

    private void checkFilesCreated(Path outputDir) {
        try {
            if (!Files.exists(outputDir)) {
                log.error("❌ Output directory does not exist: {}", outputDir);
                return;
            }

            List<Path> files = Files.list(outputDir)
                    .filter(Files::isRegularFile)
                    .toList();

            log.debug("📁 Files in {}: {}", outputDir, files.size());

            // Detale plików - tylko TRACE (wyłączone w produkcji)
            for (Path file : files) {
                try {
                    long size = Files.size(file);
                    log.info("   📄 {} ({} bytes)", file.getFileName(), size);

                    // Jeśli to M3U8, pokaż zawartość
                    if (file.toString().endsWith(".m3u8")) {
                        List<String> content = Files.readAllLines(file);
                        log.trace("   📋 M3U8 content ({} lines):", content.size());
                        for (String line : content) {
                            if (!line.trim().isEmpty() && !line.startsWith("#")) {
                                log.info("      → {}", line);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.debug("   ⚠️ Could not read file info: {}", file.getFileName());
                }
            }

        } catch (IOException e) {
            log.error("❌ Error checking output directory: {}", e.getMessage());
        }
    }
}
