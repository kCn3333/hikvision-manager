package com.kcn.hikvisionmanager.mapper;

import com.kcn.hikvisionmanager.dto.CameraChannelInfoDTO;
import com.kcn.hikvisionmanager.dto.xml.response.RecordingType;
import com.kcn.hikvisionmanager.dto.xml.response.TrackListXml;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CameraChannelMapper {

    public List<CameraChannelInfoDTO> toCameraChannelDTOs(TrackListXml trackList) {
        if (trackList == null || trackList.getTracks() == null) {
            return List.of();
        }

        return trackList.getTracks().stream()
                .map(this::toCameraChannelDTO)
                .collect(Collectors.toList());
    }

    private CameraChannelInfoDTO toCameraChannelDTO(TrackListXml.Track track) {
        return CameraChannelInfoDTO.builder()
                .channelId(track.getChannel())
                .recordingType(track.getRecordingType())
                .codec(track.getCodec())
                .bitrate(track.getBitrate())
                .resolution(track.getResolution())
                .framerate(track.getFramerate())
                .enabled(track.isEnable())
                .hasSchedule(track.hasActiveSchedule())
                .scheduleDescription(track.getScheduleDescription())
                .build();
    }


    public List<CameraChannelInfoDTO> toCameraChannelDTOs(TrackListXml trackList, boolean cameraOnline) {
        if (!cameraOnline) {
            return createOfflineChannels();
        }
        return toCameraChannelDTOs(trackList);
    }

    private List<CameraChannelInfoDTO> createOfflineChannels() {
        return List.of(
                CameraChannelInfoDTO.builder()
                        .channelId("101")
                        .recordingType(RecordingType.UNKNOWN)
                        .codec("Unknown")
                        .bitrate("Unknown")
                        .resolution("Unknown")
                        .framerate("Unknown")
                        .enabled(false)
                        .hasSchedule(false)
                        .scheduleDescription("Offline")
                        .build()
        );
    }
}