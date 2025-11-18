package com.kcn.hikvisionmanager.events.publishers;

import com.kcn.hikvisionmanager.events.model.CameraRestartInitiatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Publisher for camera restart lifecycle events.
 * Used to notify other components when camera restart is initiated.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CameraRestartPublisher {

    private final EventPublisherHelper eventPublisherHelper;

    /**
     * Publishes event when camera restart is initiated.
     * This signals to cache refresh mechanisms to pause operations during grace period.
     *
     * @param gracePeriodSeconds Duration in seconds for which camera will be offline
     */
    public void publishRestartInitiated(int gracePeriodSeconds) {
        try {
            eventPublisherHelper.publish(new CameraRestartInitiatedEvent(
                    gracePeriodSeconds,
                    LocalDateTime.now()
            ));
            log.debug("📣 Published CameraRestartInitiatedEvent with {}s grace period", gracePeriodSeconds);
        } catch (Exception e) {
            log.warn("⛔ Failed to publish CameraRestartInitiatedEvent: {}", e.getMessage());
        }
    }
}