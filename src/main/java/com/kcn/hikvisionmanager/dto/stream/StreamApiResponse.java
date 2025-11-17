package com.kcn.hikvisionmanager.dto.stream;

import lombok.Builder;

// Response DTO
@Builder
public record StreamApiResponse(
    String status,        // "success" or "error"
    String playlistUrl,   // "/live/stream.m3u8" or null
    String message,
    String details
){}
