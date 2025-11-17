package com.kcn.hikvisionmanager.exception;

/**
 * Exception thrown when storage operations fail.
 * Used for file system errors, directory access issues, etc.
 */
public class StorageException extends RuntimeException {

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}