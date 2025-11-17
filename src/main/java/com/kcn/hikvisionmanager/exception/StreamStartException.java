package com.kcn.hikvisionmanager.exception;

public class StreamStartException extends RuntimeException {
    public StreamStartException(String message, Throwable cause) {
        super(message, cause);
    }
    public StreamStartException(String message) {
        super(message);
    }
}