package com.kcn.hikvisionmanager.exception;

import java.io.IOException;

public class CameraRequestException extends RuntimeException {
    public CameraRequestException(String message) {
        super(message);
    }

    public CameraRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}