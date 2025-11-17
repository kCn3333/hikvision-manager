package com.kcn.hikvisionmanager.controller;

import com.kcn.hikvisionmanager.service.CameraManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/camera/management")
@RequiredArgsConstructor
public class CameraManagementController {

    private final CameraManagementService cameraService;

    @PostMapping("/restart")
    public ResponseEntity<String> restartCamera() {
        log.info("🌐 API: POST /api/camera/management/restart");
        boolean success= cameraService.restartCamera();
        return ResponseEntity.ok(success?"Camera restarted successfully":"Failed to restart camera");
    }
}
