package com.kcn.hikvisionmanager.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Generic API response wrapper for REST endpoints.
 * Provides a success flag, message, and optional data payload.
 */
@Data
@Builder
public class ApiResponseDTO<T> {

    private boolean success;           // True if the operation succeeded
    private String message;            // Message for user or logs
    private T data;                    // Generic data payload
}
