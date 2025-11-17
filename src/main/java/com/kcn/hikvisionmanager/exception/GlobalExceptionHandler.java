package com.kcn.hikvisionmanager.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        String detailMessage = "Invalid request format";

        if (ex.getMessage() != null && ex.getMessage().contains("LocalDateTime")) {
            detailMessage = "Invalid date format. Use format: yyyy-MM-ddTHH:mm";
        }

        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST,
                detailMessage,
                request.getRequestURI()
        );

        log.warn("❌ JSON parse error on {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField,  FieldError::getDefaultMessage, (a, b) -> a));

        ApiErrorResponse response = ApiErrorResponse.withValidation(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                request.getRequestURI(),
                errors
        );

        log.warn("❌ Validation error on {}: {}", request.getRequestURI(), errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(CameraValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleCameraValidationException(
            CameraValidationException ex, HttpServletRequest request) {

        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request.getRequestURI()
        );

        log.warn("❌ Camera validation error on {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(CameraUnauthorizedException.class)
    public ResponseEntity<ApiErrorResponse> handleUnauthorized(CameraUnauthorizedException ex, HttpServletRequest request) {
        log.warn("⛔ Camera access denied from {}: {}", request.getRemoteAddr(), ex.getMessage());

        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.UNAUTHORIZED,
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(CameraOfflineException.class)
    public ResponseEntity<ApiErrorResponse> handleCameraOfflineException(
            CameraOfflineException ex, HttpServletRequest request) {

        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.SERVICE_UNAVAILABLE,
                ex.getMessage(),
                request.getRequestURI()
        );

        log.warn("❌ Camera offline: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @ExceptionHandler(CameraRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleRequest(CameraRequestException ex, HttpServletRequest request) {
        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.BAD_GATEWAY,
                ex.getMessage(),
                request.getRequestURI()
        );
        log.error("❌ Camera request failed at {}", request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(response);
    }

    @ExceptionHandler(StreamAlreadyActiveException.class)
    public ResponseEntity<ApiErrorResponse> handleStreamAlreadyActive(
            StreamAlreadyActiveException ex, HttpServletRequest request) {

        log.warn("⚠️ Stream already active: {}", ex.getMessage());

        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(StreamStartException.class)
    public ResponseEntity<ApiErrorResponse> handleStreamStart(
            StreamStartException ex, HttpServletRequest request) {

        log.error("❌ Failed to start stream: {}", ex.getMessage(), ex);

        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to start stream: " + ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(FFmpegProcessException.class)
    public ResponseEntity<ApiErrorResponse> handleFFmpegProcess(
            FFmpegProcessException ex, HttpServletRequest request) {

        log.error("❌ FFmpeg process error: {}", ex.getMessage(), ex);

        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Media processing error",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(CameraParsingException.class)
    public ResponseEntity<ApiErrorResponse> handleParsing(CameraParsingException ex, HttpServletRequest request) {
        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.BAD_GATEWAY,
                "Failed to parse response from camera: " + ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(response);
    }

    @ExceptionHandler(JobNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleJobNotFound(
            JobNotFoundException ex, HttpServletRequest request) {

        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                request.getRequestURI()
        );

        log.warn("❌ Download job not found on {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(BackupNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleBackupNotFound(
            BackupNotFoundException ex, HttpServletRequest request) {

        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                request.getRequestURI()
        );

        log.warn("❌ Backup not found on {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(BackupExecutionException.class)
    public ResponseEntity<ApiErrorResponse> handleBackupExecution(
            BackupExecutionException ex, HttpServletRequest request) {

        log.error("❌ Backup execution failed [Job: {}, Status: {}]: {}",
                ex.getBackupJobId(), ex.getStatus(), ex.getMessage(), ex);

        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Backup execution failed: " + ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    // ✨ NEW: Handler for storage/filesystem errors
    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ApiErrorResponse> handleStorage(
            StorageException ex, HttpServletRequest request) {

        log.error("❌ Storage error on {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Storage error: " + ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiErrorResponse> handleRuntimeException(
            RuntimeException ex, HttpServletRequest request) {

        log.error("❗ Unhandled exception at {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error",
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
