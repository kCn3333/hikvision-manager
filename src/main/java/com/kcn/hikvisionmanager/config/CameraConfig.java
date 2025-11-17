package com.kcn.hikvisionmanager.config;


import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "camera.default")
@Getter
@Slf4j
public class CameraConfig {
    private final String ip;
    private final int port;
    private final String username;
    private final String password;
    private final int rtsp;
    private final String timezone;
    private final int trackId;

    public CameraConfig(String ip, int port, String username, String password, int rtsp, String timezone, int trackId) {
        this.ip = ip;
        this.port = port;
        this.username = username;
        this.password = password;
        this.rtsp = rtsp;
        this.timezone = timezone;
        this.trackId = trackId;
        log.info("✅ CameraConfig initialized");
    }
}
