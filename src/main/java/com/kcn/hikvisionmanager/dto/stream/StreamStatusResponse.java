package com.kcn.hikvisionmanager.dto.stream;


import lombok.Builder;

// Response DTO
@Builder
public record StreamStatusResponse(
        boolean active,
        String channel,
        String startTime,
        int viewers,
        String error
) {
    public static StreamStatusResponse empty() {
        return new StreamStatusResponse(false, null, null, 0, null);
    }
}
