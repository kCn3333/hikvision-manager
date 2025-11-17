package com.kcn.hikvisionmanager.mapper;

import com.kcn.hikvisionmanager.dto.RecordingItemDTO;
import com.kcn.hikvisionmanager.dto.RecordingSearchRequestDTO;
import com.kcn.hikvisionmanager.dto.RecordingSearchResultDTO;
import com.kcn.hikvisionmanager.dto.xml.request.RecordingSearchRequestXml;
import com.kcn.hikvisionmanager.dto.xml.response.RecordingSearchResponseXml;
import com.kcn.hikvisionmanager.util.TimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
public class RecordingSearchMapper {

    private final int defaultTrackId;

    public RecordingSearchMapper(@Value("${camera.default.track-id}") int defaultTrackId) {
        this.defaultTrackId = defaultTrackId;
    }

    public RecordingSearchRequestXml toXmlRequest(RecordingSearchRequestDTO request) {
        RecordingSearchRequestXml xmlRequest = new RecordingSearchRequestXml();

        // Generate unique searchId for each request
        String searchId = UUID.randomUUID().toString();
        xmlRequest.setSearchId(searchId);
        log.debug("Creating RecordingSearchRequestXml with searchId={}", searchId);

        // Track ID
        RecordingSearchRequestXml.TrackIdList trackIdList = new RecordingSearchRequestXml.TrackIdList();
        trackIdList.setTrackId(String.valueOf(defaultTrackId));
        xmlRequest.setTrackIdList(trackIdList);

        // Time span (converted to camera UTC)
        RecordingSearchRequestXml.TimeSpan timeSpan = new RecordingSearchRequestXml.TimeSpan();
        timeSpan.setStartTime(TimeUtils.localToCameraUtc(request.getStartTime()));
        timeSpan.setEndTime(TimeUtils.localToCameraUtc(request.getEndTime()));

        RecordingSearchRequestXml.TimeSpanList timeSpanList = new RecordingSearchRequestXml.TimeSpanList();
        timeSpanList.setTimeSpan(timeSpan);
        xmlRequest.setTimeSpanList(timeSpanList);

        // Pagination (ensure page >= 1)
        int page = Math.max(request.getPage(), 1);
        xmlRequest.setMaxResults(request.getPageSize());
        xmlRequest.setSearchResultPosition((page - 1) * request.getPageSize());

        return xmlRequest;
    }

    public RecordingSearchResultDTO toSearchResult(RecordingSearchResponseXml xmlResponse,
                                                RecordingSearchRequestDTO request) {

        if (xmlResponse == null) {
            log.warn("Null RecordingSearchResponseXml received.");
            return emptyResult(request);
        }

        log.debug("Mapping RecordingSearchResponseXml → RecordingSearchResult, matches={}, status={}",
                xmlResponse.getNumOfMatches(), xmlResponse.getResponseStatusString());

        // Safely handle empty or missing match list
        List<RecordingItemDTO> recordings = Optional.ofNullable(xmlResponse.getMatchList())
                .map(RecordingSearchResponseXml.MatchList::getSearchMatchItems)
                .orElse(Collections.emptyList())
                .stream()
                .map(this::toRecordingItemDTO)
                .collect(Collectors.toList());

        return RecordingSearchResultDTO.builder()
                .recordings(recordings)
                .currentPage(request.getPage())
                .pageSize(request.getPageSize())
                .totalMatches(xmlResponse.getNumOfMatches())
                .hasMore("MORE".equalsIgnoreCase(xmlResponse.getResponseStatusString()))
                .searchId(xmlResponse.getSearchId())
                .build();
    }

    private RecordingSearchResultDTO emptyResult(RecordingSearchRequestDTO request) {
        return RecordingSearchResultDTO.builder()
                .recordings(List.of())
                .currentPage(request.getPage())
                .pageSize(request.getPageSize())
                .totalMatches(0)
                .hasMore(false)
                .build();
    }

    private RecordingItemDTO toRecordingItemDTO(RecordingSearchResponseXml.SearchMatchItem matchItem) {
        if (matchItem == null || matchItem.getTimeSpan() == null) {
            log.warn("Invalid SearchMatchItem: missing TimeSpan or null item.");
            return RecordingItemDTO.builder()
                    .recordingId("UNKNOWN")
                    .trackId("UNKNOWN")
                    .duration("00:00")
                    .fileSize("Unknown")
                    .build();
        }

        // Convert UTC timestamps to local time
        LocalDateTime startTime = TimeUtils.cameraUtcToLocal(matchItem.getTimeSpan().getStartTime());
        LocalDateTime endTime = TimeUtils.cameraUtcToLocal(matchItem.getTimeSpan().getEndTime());

        return RecordingItemDTO.builder()
                .recordingId(generateRecordingId(matchItem))
                .trackId(matchItem.getTrackId())
                .startTime(startTime)
                .endTime(endTime)
                .duration(calculateDuration(startTime, endTime))
                .codec(matchItem.getMediaSegmentDescriptor().getCodecType())
                .playbackUrl(matchItem.getMediaSegmentDescriptor().getPlaybackUri())
                .fileSize(extractFileSize(matchItem.getMediaSegmentDescriptor().getPlaybackUri()))
                .build();
    }

    private String generateRecordingId(RecordingSearchResponseXml.SearchMatchItem matchItem) {
        return String.format("%s_%s", matchItem.getTrackId(), matchItem.getTimeSpan().getStartTime());
    }

    private String calculateDuration(LocalDateTime start, LocalDateTime end) {
        Duration duration = Duration.between(start, end);
        long minutes = duration.toMinutes();
        long seconds = duration.minusMinutes(minutes).getSeconds();
        return String.format("%02d:%02d", minutes, seconds);
    }

    private String extractFileSize(String playbackUri) {
        if (playbackUri == null) return "Unknown";

        try {
            String[] params = playbackUri.split("&");
            for (String param : params) {
                if (param.trim().startsWith("size=")) {
                    String sizeStr = param.substring(5); // skip "size="
                    long sizeBytes = Long.parseLong(sizeStr);
                    return formatFileSize(sizeBytes);
                }
            }
        } catch (Exception e) {
            log.debug("Failed to extract file size from URL [{}]: {}", playbackUri, e.getMessage());
        }

        return "Unknown";
    }

    private String formatFileSize(long bytes) {
        double kb = bytes / 1024.0;
        double mb = kb / 1024.0;
        double gb = mb / 1024.0;

        if (gb >= 1) return String.format("%.2f GB", gb);
        if (mb >= 1) return String.format("%.1f MB", mb);
        if (kb >= 1) return String.format("%.1f KB", kb);
        return bytes + " B";
    }
}