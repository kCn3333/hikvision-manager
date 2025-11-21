package com.kcn.hikvisionmanager.service;

import com.kcn.hikvisionmanager.client.HikvisionIsapiClient;
import com.kcn.hikvisionmanager.dto.xml.request.RebootRequestXml;
import com.kcn.hikvisionmanager.dto.xml.response.RebootResponseXml;
import com.kcn.hikvisionmanager.events.publishers.CameraRestartPublisher;
import com.kcn.hikvisionmanager.exception.CameraOfflineException;
import com.kcn.hikvisionmanager.exception.CameraParsingException;
import com.kcn.hikvisionmanager.exception.CameraRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.kcn.hikvisionmanager.client.HttpClientConfig.CAMERA_RESTART_GRACE_SECONDS;

@Slf4j
@Service
@RequiredArgsConstructor
public class CameraManagementService {


    private final HikvisionIsapiClient hikvisionIsapiClient;
    private final CameraUrlBuilder urlBuilder;
    private final CameraRestartPublisher cameraRestartPublisher;

    /**
     * Initiates camera restart and publishes event to pause cache refresh.
     * Cache refresh will be paused for 15 seconds to avoid connection errors
     * while camera is rebooting.
     */
    public boolean restartCamera() {
        // Publish event BEFORE sending restart command
        // This ensures cache refresh is paused immediately
        cameraRestartPublisher.publishRestartInitiated(CAMERA_RESTART_GRACE_SECONDS);

        log.info("🔄 Camera restart initiated - grace period: {} seconds", CAMERA_RESTART_GRACE_SECONDS);


        try {
            // 1. Prepare request body
            RebootRequestXml request = new RebootRequestXml();

            // 2. Send request to camera
            RebootResponseXml response = hikvisionIsapiClient.executePut(
                    urlBuilder.buildRestartUrl(),
                    request,
                    RebootResponseXml.class
            );

            // 3. Handle camera response
            if (response.isSuccess()) {
                log.info("✅ Camera restart initiated successfully");
                return true;
            } else {
                log.warn("❌ Camera restart failed: {}", response.getStatusString());
                return false;
            }

        } catch (CameraOfflineException e) {
            log.warn("⚠️ Camera is offline, cannot restart: {}", e.getMessage());
            throw e;

        } catch (CameraRequestException e) {
            log.error("❌ Camera request failed during restart: {}", e.getMessage());
            throw e;

        } catch (CameraParsingException e) {
            log.error("❌ Failed to parse restart response XML", e);
            throw e;
        }
    }
}
