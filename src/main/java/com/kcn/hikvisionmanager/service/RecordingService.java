package com.kcn.hikvisionmanager.service;

import com.kcn.hikvisionmanager.client.HikvisionIsapiClient;
import com.kcn.hikvisionmanager.dto.RecordingSearchRequestDTO;
import com.kcn.hikvisionmanager.dto.RecordingSearchResultDTO;
import com.kcn.hikvisionmanager.dto.xml.request.RecordingSearchRequestXml;
import com.kcn.hikvisionmanager.dto.xml.response.RecordingSearchResponseXml;
import com.kcn.hikvisionmanager.exception.CameraOfflineException;
import com.kcn.hikvisionmanager.exception.CameraParsingException;
import com.kcn.hikvisionmanager.exception.CameraRequestException;
import com.kcn.hikvisionmanager.mapper.RecordingSearchMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecordingService {

    private final HikvisionIsapiClient hikvisionIsapiClient;
    private final CameraUrlBuilder urlBuilder;
    private final RecordingSearchMapper recordingSearchMapper;

    /**
     * Searches for recordings within the given time range and pagination settings.
     */
    public RecordingSearchResultDTO searchRecordings(RecordingSearchRequestDTO request) {
        log.debug("🔍 Searching recordings from {} to {}, page {}",
                request.getStartTime(), request.getEndTime(), request.getPage());

        try {
            // 1️⃣ Map DTO → XML request
            RecordingSearchRequestXml xmlRequest = recordingSearchMapper.toXmlRequest(request);
            log.debug("Mapped XML request: trackId={}, maxResults={}, position={}",
                    xmlRequest.getTrackIdList().getTrackId(),
                    xmlRequest.getMaxResults(),
                    xmlRequest.getSearchResultPosition());

            // 2️⃣ Send request to camera (XML → camera)
            RecordingSearchResponseXml xmlResponse = hikvisionIsapiClient.executePost(
                    urlBuilder.buildRecordingSearchUrl(),
                    xmlRequest,
                    RecordingSearchResponseXml.class
            );

            // 3️⃣ Map XML → DTO
            RecordingSearchResultDTO result = recordingSearchMapper.toSearchResult(xmlResponse, request);

            log.debug("✅ Found {} recordings (hasMore={})",
                    result.getTotalMatches(),
                    result.isHasMore());
            return result;

        } catch (CameraParsingException e) {
            log.error("❌ Failed to parse XML response: {}", e.getMessage(), e);
            throw e;

        } catch (CameraOfflineException e) {
            log.warn("❌ Camera is offline: {}", e.getMessage());
            throw e;

        } catch (CameraRequestException e) {
            log.error("❌ Camera request failed: {}", e.getMessage(), e);
            throw e;

        } catch (Exception e) {
            // Defensive fallback for any unexpected exception
            log.error("❌ Unexpected error during recording search: {}", e.getMessage(), e);
            throw new CameraRequestException("Unexpected error during recording search", e);
        }
    }

    /**
     * Searches for recordings from the last N hours.
     */
    public RecordingSearchResultDTO searchRecentRecordings(int lastNHours, int pageSize) {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusHours(lastNHours);

        RecordingSearchRequestDTO request = RecordingSearchRequestDTO.builder()
                .startTime(startTime)
                .endTime(endTime)
                .page(1)
                .pageSize(pageSize)
                .build();

        return searchRecordings(request);
    }

    /**
     * Searches for recordings by a specific date (00:00-23:59).
     */
    public RecordingSearchResultDTO searchRecordingsByDate(LocalDateTime date, int pageSize) {
        LocalDateTime startTime = date.toLocalDate().atStartOfDay();
        LocalDateTime endTime = startTime.plusDays(1).minusSeconds(1);

        RecordingSearchRequestDTO request = RecordingSearchRequestDTO.builder()
                .startTime(startTime)
                .endTime(endTime)
                .page(1)
                .pageSize(pageSize)
                .build();

        return searchRecordings(request);
    }

    /**
     * Fetches the next page of results if available.
     */
    public RecordingSearchResultDTO searchNextPage(RecordingSearchResultDTO currentResult, int pageSize) {
        if (!currentResult.isHasMore()) {
            log.warn("⚠️ No more results available.");
            return currentResult;
        }

        RecordingSearchRequestDTO request = RecordingSearchRequestDTO.builder()
                .startTime(currentResult.getRecordings().getFirst().getStartTime())
                .endTime(currentResult.getRecordings().getLast().getEndTime())
                .page(currentResult.getCurrentPage() + 1)
                .pageSize(pageSize)
                .build();

        return searchRecordings(request);
    }

}