package com.kcn.hikvisionmanager.dto;

import lombok.Data;

/**
 * Data Transfer Object for Camera.
 * Used to send camera information to the frontend.
 * Password is intentionally excluded from DTO for security reasons.
 */
@Data
public class CameraDTO {

    private String id;                // Unique camera identifier, e.g., "cam01"
    private String name;              // Human-readable camera name
    private String ip;                // Camera IP address
    private String operatorName;      // Camera operator name
    private boolean connected;        // Camera connection status
    private String firmwareVersion;   // Camera firmware version
    private String model;             // Camera model

    private CameraHealthDTO cameraHealth;
}
