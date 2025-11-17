package com.kcn.hikvisionmanager.exception;

import java.net.SocketTimeoutException;

public class CameraOfflineException extends RuntimeException {
    public CameraOfflineException(String message) {
        super(message);
    }

    public CameraOfflineException(String message, Throwable cause) {
        super(message, cause);
    }
}