package com.kcn.hikvisionmanager.controller;

import com.kcn.hikvisionmanager.dto.*;
import com.kcn.hikvisionmanager.service.CameraService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/camera")
@RequiredArgsConstructor
public class CameraController {

    private final CameraService cameraService;

    @GetMapping("/info")
    public ResponseEntity<CameraInfoDTO> getDeviceInfo() {
        log.debug("🌐 API: GET /api/camera/info");
        return ResponseEntity.ok(cameraService.getDeviceInfo());
    }

    @GetMapping("/status")
    public ResponseEntity<CameraHealthDTO> checkHealth() {
        log.debug("🌐 API: GET /api/camera/status");
            return ResponseEntity.ok(cameraService.getSystemStatus());
    }

    @GetMapping("/network")
    public ResponseEntity<CameraNetworkDTO> networkInfo() {
        log.debug("🌐 API: GET /api/camera/network");
        return ResponseEntity.ok(cameraService.getNetworkInfo());
    }

    @GetMapping("/storage")
    public ResponseEntity<CameraStorageDTO> storageInfo() {
        log.debug("🌐 API: GET /api/camera/storage");
        return ResponseEntity.ok(cameraService.getStorageInfo());
    }

    @GetMapping("/time")
    public ResponseEntity<CameraTimeDTO> timeInfo() {
        log.debug("🌐 API: GET /api/camera/time");
        return ResponseEntity.ok(cameraService.getTimeInfo());
    }

    @GetMapping("/channels")
    public List<CameraChannelInfoDTO> getAllChannels() {
        log.debug("🌐 API: GET /api/camera/channels");
        return cameraService.getTrackList();
    }

}