package com.kcn.hikvisionmanager.domain;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class StreamSession {
    private final String sessionId;
    private final String channel;
    private final Instant startTime;
    private final String playlistUrl;
    private final RunningFfmpegProcess ffmpegProcess;

    public StreamSession(String sessionId, String channel, Instant startTime,
                         String playlistUrl, RunningFfmpegProcess ffmpegProcess) {
        this.sessionId = sessionId;
        this.channel = channel;
        this.startTime = startTime;
        this.playlistUrl = playlistUrl;
        this.ffmpegProcess = ffmpegProcess;
    }

}