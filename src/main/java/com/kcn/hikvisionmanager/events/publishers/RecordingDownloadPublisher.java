package com.kcn.hikvisionmanager.events.publishers;

import com.kcn.hikvisionmanager.events.model.RecordingDownloadCompletedEvent;
import com.kcn.hikvisionmanager.events.model.RecordingDownloadFailedEvent;
import com.kcn.hikvisionmanager.events.model.RecordingDownloadStartedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
@RequiredArgsConstructor
public class RecordingDownloadPublisher {

    private final EventPublisherHelper eventPublisherHelper;

    public void publishDownloadStarted(String recordingId, String batchId) {
        try {
            eventPublisherHelper.publish(new RecordingDownloadStartedEvent(
                    recordingId,
                    batchId,
                    LocalDateTime.now()
            ));
            log.debug("\uD83D\uDCE3 Published DownloadStartedEvent: {}", recordingId);
        } catch (Exception e) {
            log.warn("⛔ Failed to publish DownloadStartedEvent: {}", e.getMessage());
        }

    }

    public void publishDownloadCompleted(String recordingId, String batchId, Long actualFileSizeBytes) {
        try {
            eventPublisherHelper.publish(new RecordingDownloadCompletedEvent(
                    recordingId,
                    batchId,
                    actualFileSizeBytes,
                    LocalDateTime.now()
            ));
            log.debug("\uD83D\uDCE3 Published DownloadCompletedEvent: {}", recordingId);
        } catch (Exception e) {
            log.warn("⛔ Failed to publish DownloadCompletedEvent: {}", e.getMessage());
        }
    }

    public void publishDownloadFailed(String recordingId, String batchId, Long actualFileSizeBytes, String errorMessage) {
        try {
            eventPublisherHelper.publish(new RecordingDownloadFailedEvent(
                    recordingId,
                    batchId,
                    errorMessage,
                    actualFileSizeBytes,
                    LocalDateTime.now()
            ));
            log.info("\uD83D\uDCE3 Published DownloadFailedEvent: {}", recordingId);
        } catch (Exception e) {
            log.warn("⛔ Failed to publish DownloadFailedEvent: {}", e.getMessage());
        }
    }
}
