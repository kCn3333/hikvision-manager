package com.kcn.hikvisionmanager.scheduler;

import com.kcn.hikvisionmanager.entity.BackupConfigurationEntity;
import com.kcn.hikvisionmanager.events.model.BackupTriggerEvent;
import com.kcn.hikvisionmanager.events.publishers.EventPublisherHelper;
import com.kcn.hikvisionmanager.repository.BackupConfigurationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class BackupScheduler {

    private final BackupConfigurationRepository configRepository;
    private final EventPublisherHelper publisher;

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void checkAndExecuteBackups() {
        List<BackupConfigurationEntity> configs = configRepository.findByEnabled(true);
        LocalDateTime now = LocalDateTime.now();

        log.debug("🔍 Checking schedule for {} enabled configurations", configs.size());

        for (BackupConfigurationEntity config : configs) {
            try {
                if (shouldExecuteBackup(config, now)) {
                    publisher.publish(new BackupTriggerEvent(config.getId()));
                }
            } catch (Exception e) {
                log.error("❌ Error scheduling backup for {}: {}", config.getName(), e.getMessage(), e);
            }
        }
    }

    private boolean shouldExecuteBackup(BackupConfigurationEntity config, LocalDateTime now) {
        try {
            CronExpression cron = CronExpression.parse(config.getCronExpression());

            // 1. Initialization: If nextRunAt is missing (new config), calculate it and wait
            if (config.getNextRunAt() == null) {
                LocalDateTime nextRun = cron.next(now);
                config.setNextRunAt(nextRun);

                configRepository.save(config);

                log.debug("🗓️ Schedule initialized for: {}, next run at: {}", config.getName(), nextRun);
                return false;
            }

            // 2. If the scheduled time is in the future -> do nothing
            if (config.getNextRunAt().isAfter(now)) {
                log.debug("⏳ schedule for {}, nothing to do yet. ", config.getName());
                return false;
            }

            // 3. Time has come (or passed) -> EXECUTE

            // Calculate the NEXT run time from NOW to prevent infinite loops or double execution
            LocalDateTime nextRun = cron.next(now);
            config.setNextRunAt(nextRun);

            configRepository.save(config);

            log.info("⏰ Time for backup: {} (next run scheduled for: {})", config.getName(), nextRun);
            return true;

        } catch (Exception e) {
            log.error("❌ Invalid cron for {}: {}", config.getId(), e.getMessage());
            return false;
        }
    }
}