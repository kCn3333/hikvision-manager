package com.kcn.hikvisionmanager.dto;

import com.kcn.hikvisionmanager.dto.xml.response.RecordingType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CameraChannelInfoDTO {
    private String channelId;
    private RecordingType recordingType;
    private String codec;
    private String bitrate;
    private String resolution;
    private String framerate;
    private boolean enabled;
    private boolean hasSchedule;
    private String scheduleDescription;
}
