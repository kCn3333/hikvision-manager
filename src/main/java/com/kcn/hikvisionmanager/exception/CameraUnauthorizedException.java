package com.kcn.hikvisionmanager.exception;

public class CameraUnauthorizedException extends RuntimeException {
    public CameraUnauthorizedException(String message) {
        super(message);
    }
}