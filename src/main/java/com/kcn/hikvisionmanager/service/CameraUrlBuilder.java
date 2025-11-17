package com.kcn.hikvisionmanager.service;

import com.kcn.hikvisionmanager.config.CameraConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CameraUrlBuilder {
    private final CameraConfig cameraConfig;

    public String buildDeviceInfoUrl() {
        String url=buildBaseUrl()+"ISAPI/System/deviceInfo";
        log.debug("Camera URL: {}", url);
        return url;
    }

    public String buildSystemStatusUrl() {
        String url=buildBaseUrl()+"ISAPI/System/status";
        log.debug("Camera URL: {}", url);
        return url;
    }

    public String buildStorageInfosUrl() {
        String url=buildBaseUrl()+"ISAPI/ContentMgmt/Storage";
        log.debug("Camera URL: {}", url);
        return url;
    }

    public String buildNetworkInfoUrl() {
        String url=buildBaseUrl()+"ISAPI/System/Network/interfaces/1";
        log.debug("Camera URL: {}", url);
        return url;
    }

    public String buildTimeInfoUrl() {
        String url=buildBaseUrl()+"ISAPI/System/time";
        log.debug("Camera URL: {}", url);
        return url;
    }

    public String buildRestartUrl() {
        String url=buildBaseUrl()+ "ISAPI/System/reboot";
        log.debug("Camera URL: {}", url);
        return url;
    }

    public String buildTrackListUrl() {
        String url=buildBaseUrl()+"ISAPI/ContentMgmt/record/tracks";
        log.debug("Camera URL: {}", url);
        return url;
    }

    public String buildStopRecordUrl(String channelId) {
        String url=buildBaseUrl()+"ISAPI/ContentMgmt/record/control/manual/stop/track/"+channelId;
        log.debug("Camera URL: {}", url);
        return url;
    }

    public String buildStartRecordUrl(String channelId) {
        String url=buildBaseUrl()+"ISAPI/ContentMgmt/record/control/manual/start/track/"+channelId;
        log.debug("Camera URL: {}", url);
        return url;
    }

    public String buildRecordingSearchUrl() {
        String url=buildBaseUrl()+"ISAPI/ContentMgmt/search";
        log.debug("Camera URL: {}", url);
        return url;
    }

    public String buildDownloadUrl() {
        String url = buildBaseUrl() + "ISAPI/ContentMgmt/download";
        log.debug("Camera download URL: {}", url);
        return url;
    }

    public String buildStreamUrl(String channel) {
        return String.format("rtsp://%s:%s@%s:%d/Streaming/Channels/%s",
                cameraConfig.getUsername(),
                cameraConfig.getPassword(),
                cameraConfig.getIp(),
                cameraConfig.getRtsp(),
                channel);
    }

    private String buildBaseUrl() {
        return String.format("http://%s:%d/",
                cameraConfig.getIp(), cameraConfig.getPort());
    }

    /**
     * Add credentials to RTSP playback URL for FFmpeg download
     *
     * Input:  rtsp://192.168.0.64/Streaming/tracks/101/?starttime=20251030T145528Z&endtime=20251030T150554Z
     * Output: rtsp://admin:password@192.168.0.64/Streaming/tracks/101/?starttime=20251030T145528Z&endtime=20251030T150554Z
     *
     * @param playbackUrl RTSP URL without credentials (from RecordingItemDTO)
     * @return RTSP URL with embedded credentials
     */
    public String addCredentialsToRtspUrl(String playbackUrl) {
        if (playbackUrl == null || playbackUrl.isEmpty()) {
            log.warn("Empty playback URL provided");
            return playbackUrl;
        }

        if (!playbackUrl.startsWith("rtsp://")) {
            log.warn("Invalid RTSP URL format (must start with rtsp://): {}", playbackUrl);
            return playbackUrl;
        }

        // Decode HTML entities (&amp; -> &, &quot; -> ", etc.)
        // This happens when URL is serialized to JSON and then deserialized
        String decodedUrl = playbackUrl
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&apos;", "'");

        log.debug("Decoded URL: {}", decodedUrl);

        // Remove name and size parameters (cleaner URL, optional)
        String cleanUrl = decodedUrl;
        if (decodedUrl.contains("&name=")) {
            int nameIndex = decodedUrl.indexOf("&name=");
            cleanUrl = decodedUrl.substring(0, nameIndex);
            log.debug("Cleaned URL (removed name/size): {}", cleanUrl);
        }

        // Check if credentials already present
        if (decodedUrl.contains("@")) {
            log.debug("Credentials already present in URL");
            return decodedUrl;
        }

        // Insert credentials after rtsp://
        String urlWithAuth = decodedUrl.replace("rtsp://",
                String.format("rtsp://%s:%s@",
                        cameraConfig.getUsername(),
                        cameraConfig.getPassword()));

        log.debug("🔍 Final RTSP URL: {}", urlWithAuth);
        return urlWithAuth;
    }
}