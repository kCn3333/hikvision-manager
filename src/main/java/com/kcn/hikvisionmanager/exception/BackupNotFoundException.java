package com.kcn.hikvisionmanager.exception;

public class BackupNotFoundException extends RuntimeException {
    public BackupNotFoundException(String message) {
        super(message);
    }
}