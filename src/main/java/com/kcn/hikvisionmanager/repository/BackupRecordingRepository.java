package com.kcn.hikvisionmanager.repository;

import com.kcn.hikvisionmanager.entity.BackupRecordingEntity;
import com.kcn.hikvisionmanager.domain.BackupRecordingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BackupRecordingRepository extends JpaRepository<BackupRecordingEntity, Long> {

    List<BackupRecordingEntity> findByBackupJobId(String backupJobId);

    List<BackupRecordingEntity> findByBackupJobIdAndStatus(String backupJobId, BackupRecordingStatus status);

    long countByBackupJobIdAndStatus(String backupJobId, BackupRecordingStatus status);

    @Query("SELECT SUM(r.fileSizeBytes) FROM BackupRecordingEntity r WHERE r.backupJobId = :jobId AND r.status = 'COMPLETED'")
    Long getTotalSizeByJobId(String jobId);

    Optional<BackupRecordingEntity> findByBackupJobIdAndRecordingId(String s, String recordingId);
}
