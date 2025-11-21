package com.kcn.hikvisionmanager.service;

import com.kcn.hikvisionmanager.client.HikvisionIsapiClient;
import com.kcn.hikvisionmanager.dto.*;
import com.kcn.hikvisionmanager.dto.xml.response.*;
import com.kcn.hikvisionmanager.events.model.CameraRestartInitiatedEvent;
import com.kcn.hikvisionmanager.exception.CameraOfflineException;
import com.kcn.hikvisionmanager.exception.CameraParsingException;
import com.kcn.hikvisionmanager.exception.CameraRequestException;
import com.kcn.hikvisionmanager.exception.CameraUnauthorizedException;
import com.kcn.hikvisionmanager.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CameraService {

    private final CameraInfoMapper cameraInfoMapper;
    private final CameraHealthMapper cameraHealthMapper;
    private final CameraStorageMapper cameraStorageMapper;
    private final CameraNetworkMapper cameraNetworkMapper;
    private final CameraTimeMapper cameraTimeMapper;
    private final CameraChannelMapper cameraChannelMapper;
    private final HikvisionIsapiClient hikvisionIsapiClient;
    private final CameraUrlBuilder urlBuilder;

    private volatile LocalDateTime restartGraceUntil = null;

    @Cacheable("cameraInfo")
    public CameraInfoDTO getDeviceInfo() {
        DeviceInfoXml xml = fetchData(urlBuilder.buildDeviceInfoUrl(), DeviceInfoXml.class);
        return cameraInfoMapper.toCameraInfoDTO(xml);
    }

    @Cacheable("cameraStatus")
    public CameraHealthDTO getSystemStatus() {
        DeviceStatusXml xml = fetchData(urlBuilder.buildSystemStatusUrl(), DeviceStatusXml.class);
        return cameraHealthMapper.toCameraHealthDTO(xml);
    }

    public CameraStorageDTO getStorageInfo() {
        DeviceStorageInfoXml xml = fetchData(urlBuilder.buildStorageInfosUrl(), DeviceStorageInfoXml.class);
        return cameraStorageMapper.toCameraStorageDTO(xml);
    }

    public CameraNetworkDTO getNetworkInfo() {
        DeviceNetworkInfoXml xml = fetchData(urlBuilder.buildNetworkInfoUrl(), DeviceNetworkInfoXml.class);
        return cameraNetworkMapper.toCameraNetworkDTO(xml);
    }

    public CameraTimeDTO getTimeInfo() {
        DeviceTimeXml xml = fetchData(urlBuilder.buildTimeInfoUrl(), DeviceTimeXml.class);
        return cameraTimeMapper.toCameraTimeDTO(xml);
    }

    public List<CameraChannelInfoDTO> getTrackList() {
        TrackListXml xml = fetchData(urlBuilder.buildTrackListUrl(), TrackListXml.class);
        return cameraChannelMapper.toCameraChannelDTOs(xml);
    }


    /**
     * Event listener that handles camera restart initialization.
     * Sets grace period during which cache refresh attempts will be skipped.
     *
     * @param event Camera restart event containing grace period duration
     */
    @EventListener
    public void onCameraRestart(CameraRestartInitiatedEvent event) {
        restartGraceUntil = event.getOccurredAt().plusSeconds(event.getGracePeriodSeconds());
        log.debug("⏸️ Cache refresh paused for {} seconds due to camera restart (until: {})",
                event.getGracePeriodSeconds(), restartGraceUntil);
    }

    /**
     * Executes camera GET request and handles known exceptions consistently.
     * Skips HTTP calls during camera restart grace period to prevent connection errors.
     */
    private <T> T fetchData(String url, Class<T> responseType) {
        // Check if we're in grace period after restart
        if (isInRestartGracePeriod()) {
            log.debug("⏳ Skipping fetch from {} - camera restart grace period active until {}",
                    url, restartGraceUntil);
            throw new CameraOfflineException("Camera is restarting, please wait");
        }

        try {
            return hikvisionIsapiClient.executeGet(url, responseType);
        } catch (CameraOfflineException e) {
            log.warn("❌ Camera is offline while calling {}: {}", url, e.getMessage());
            throw e;
        } catch (CameraParsingException e) {
            log.error("❌ Failed to parse camera response from {}: {}", url, e.getMessage());
            throw e;
        } catch (CameraRequestException e) {
            log.error("❌ Camera request failed for {}: {}", url, e.getMessage());
            throw e;
        } catch (CameraUnauthorizedException e) {
            log.warn("❌ Unauthorized access to camera while calling {}: {}", url, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("❌ Unexpected error while fetching data from {}: {}", url, e.getMessage());
            throw new CameraRequestException("Unexpected error while communicating with camera", e);
        }
    }

    /**
     * Checks if current time is within restart grace period.
     * Automatically resets grace period after it expires.
     *
     * @return true if grace period is active, false otherwise
     */
    private boolean isInRestartGracePeriod() {
        if (restartGraceUntil == null) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();

        // Grace period still active
        if (now.isBefore(restartGraceUntil)) {
            return true;
        }

        // Grace period expired - reset and resume normal operations
        restartGraceUntil = null;
        log.info("✅ Cache refresh resumed - restart grace period ended");
        return false;
    }
}