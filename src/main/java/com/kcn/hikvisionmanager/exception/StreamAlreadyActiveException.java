package com.kcn.hikvisionmanager.exception;

public class StreamAlreadyActiveException extends RuntimeException {
    public StreamAlreadyActiveException(String sessionId, String channelId) {
        super(String.format("Stream already active for session %s on channel %s",
                sessionId, channelId));
    }
}