package com.kcn.hikvisionmanager.sheduler;

import com.kcn.hikvisionmanager.entity.BackupConfigurationEntity;
import com.kcn.hikvisionmanager.events.model.BackupTriggerEvent;
import com.kcn.hikvisionmanager.events.publishers.EventPublisherHelper;
import com.kcn.hikvisionmanager.repository.BackupConfigurationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class BackupScheduler {

    private final BackupConfigurationRepository configRepository;
    private final EventPublisherHelper publisher;

    @Scheduled(fixedRate = 60000)
    public void checkAndExecuteBackups() {
        List<BackupConfigurationEntity> configs = configRepository.findByEnabled(true);
        LocalDateTime now = LocalDateTime.now();

        for (BackupConfigurationEntity config : configs) {
            try {
                if (shouldExecuteBackup(config, now)) {
                    publisher.publish(new BackupTriggerEvent(config));
                }
            } catch (Exception e) {
                log.error("❌ Error scheduling backup for {}: {}", config.getName(), e.getMessage(), e);
            }
        }
    }

    private boolean shouldExecuteBackup(BackupConfigurationEntity config, LocalDateTime now) {
        try {
            CronExpression cron = CronExpression.parse(config.getCronExpression());
            if (config.getNextRunAt() != null && config.getNextRunAt().isAfter(now)) {
                return false;
            }
            LocalDateTime nextRun = cron.next(now);
            LocalDateTime previousNext = config.getNextRunAt();

            boolean due =
                    previousNext == null ||
                            (now.isAfter(previousNext.minusMinutes(1)) && now.isBefore(previousNext.plusMinutes(1)));

            if (due) {
                config.setNextRunAt(nextRun);
                configRepository.save(config);
                return true;
            }
            return false;

        } catch (Exception e) {
            log.error("Invalid cron for {}: {}", config.getId(), e.getMessage());
            return false;
        }
    }
}
