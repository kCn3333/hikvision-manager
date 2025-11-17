package com.kcn.hikvisionmanager.service;

import com.kcn.hikvisionmanager.client.HikvisionIsapiClient;
import com.kcn.hikvisionmanager.dto.xml.request.RebootRequestXml;
import com.kcn.hikvisionmanager.dto.xml.response.RebootResponseXml;
import com.kcn.hikvisionmanager.exception.CameraOfflineException;
import com.kcn.hikvisionmanager.exception.CameraParsingException;
import com.kcn.hikvisionmanager.exception.CameraRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CameraManagementService {

    private final HikvisionIsapiClient hikvisionIsapiClient;
    private final CameraUrlBuilder urlBuilder;

    /**
     * Sends a restart command to the camera via ISAPI.
     * Returns true if restart was successfully initiated.
     */
    public boolean restartCamera() {
        log.info("Initiating camera restart...");

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
