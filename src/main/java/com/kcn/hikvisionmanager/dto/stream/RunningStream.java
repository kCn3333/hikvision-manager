package com.kcn.hikvisionmanager.dto.stream;

import com.kcn.hikvisionmanager.domain.RunningFfmpegProcess;

import java.nio.file.Path;
import java.time.Instant;

public record RunningStream(
        String channelId,
        String sessionId,
        Path outputDir,
        RunningFfmpegProcess runningProcess,
        Instant startTime
) {
}