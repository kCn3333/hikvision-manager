package com.kcn.hikvisionmanager.exception;

public class SessionNotFoundException extends RuntimeException {
    public SessionNotFoundException(String sessionId) {
        super(String.format("Session not found: %s", sessionId));
    }
}
