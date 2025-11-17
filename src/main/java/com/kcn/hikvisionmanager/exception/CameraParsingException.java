package com.kcn.hikvisionmanager.exception;

public class CameraParsingException extends RuntimeException {

    public CameraParsingException(String message) {
        super(message);
    }

    public CameraParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}