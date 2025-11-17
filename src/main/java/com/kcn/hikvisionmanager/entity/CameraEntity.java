package com.kcn.hikvisionmanager.entity;

import jakarta.persistence.*;
import lombok.Data;


/**
 * Entity representing a single camera.
 * Stores the essential information required to connect
 * and manage the camera via ISAPI and streaming.
 */
@Entity
@Table(name = "cameras")
@Data
public class CameraEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private String id; // Unique camera identifier, e.g., "cam01"

    @Column(nullable = false)
    private String name; // Human-readable camera name, e.g., "Front Gate"

    @Column(nullable = false)
    private String ip; // Camera IP address

    @Column(nullable = false)
    private int port; // HTTP port (80/443)

    @Column(nullable = false)
    private String username; // Login username for ISAPI

    @Column(nullable = false)
    private String password; // Login password for ISAPI (stored encrypted in DB)

    @Column(name = "stream_main_url")
    private String streamMainUrl; // Primary stream RTSP URL (101)

    @Column(name = "stream_sub_url")
    private String streamSubUrl; // Secondary stream RTSP URL (102)

    @Column(name = "firmware_version")
    private String firmwareVersion;

    @Column(name = "model")
    private String model;

    @Column(name = "connected")
    private boolean connected; // Camera reachability status
}
