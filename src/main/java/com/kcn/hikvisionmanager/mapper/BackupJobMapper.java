package com.kcn.hikvisionmanager.mapper;

import com.kcn.hikvisionmanager.entity.BackupJobEntity;
import com.kcn.hikvisionmanager.dto.BackupJobDTO;
import com.kcn.hikvisionmanager.util.TimeUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper to convert BackupJobEntity → BackupJobDTO.
 * Converts UTC times to local timezone for frontend display.
 */
@Component
public class BackupJobMapper {

    public BackupJobDTO toDTO(BackupJobEntity entity) {
        if (entity == null) {
            return null;
        }

        return BackupJobDTO.builder()
                .jobId(entity.getId())
                .cameraId(entity.getConfiguration() != null
                        ? entity.getConfiguration().getCameraId()
                        : null)
                .startedAt(entity.getStartedAt() != null
                        ? TimeUtils.cameraUtcToLocal(entity.getStartedAt())
                        : null)
                .endTime(entity.getCompletedAt() != null
                        ? TimeUtils.cameraUtcToLocal(entity.getCompletedAt())
                        : null)
                .totalFiles(entity.getTotalRecordings())
                .completedFiles(entity.getCompletedRecordings())
                .totalBytes(entity.getTotalSizeBytes() != null ? entity.getTotalSizeBytes() : 0)
                .status(entity.getStatus() != null ? entity.getStatus().name() : "UNKNOWN")
                .logPath(entity.getBackupDirectory())
                .build();
    }
}