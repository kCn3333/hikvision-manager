package com.kcn.hikvisionmanager.repository;

import com.kcn.hikvisionmanager.entity.BackupJobEntity;
import com.kcn.hikvisionmanager.domain.BackupJobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for BackupJobEntity.
 */
@Repository
public interface BackupJobRepository extends JpaRepository<BackupJobEntity, String> {

    List<BackupJobEntity> findByConfigId(String configId);

    List<BackupJobEntity> findByStatus(BackupJobStatus status);

    List<BackupJobEntity> findByConfigIdOrderByStartedAtDesc(String configId);

    @Query("SELECT b FROM BackupJobEntity b WHERE b.startedAt BETWEEN :startDate AND :endDate")
    List<BackupJobEntity> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    @Query("SELECT SUM(b.totalSizeBytes) FROM BackupJobEntity b WHERE b.status = 'COMPLETED'")
    Long getTotalBackupSize();

    @Query("SELECT COUNT(b) FROM BackupJobEntity b WHERE b.status = 'COMPLETED'")
    long countCompletedBackups();

    List<BackupJobEntity> findAllByOrderByStartedAtDesc();
    Page<BackupJobEntity> findAllByOrderByStartedAtDesc(Pageable pageable);

    @Modifying
    @Query("UPDATE BackupJobEntity b SET " +
            "b.completedRecordings = b.completedRecordings + 1, " +
            "b.totalSizeBytes = b.totalSizeBytes + :sizeBytes " +
            "WHERE b.id = :backupJobId")
    void incrementCompleted(@Param("backupJobId") String backupJobId,
                            @Param("sizeBytes") long sizeBytes);

    @Modifying
    @Query("UPDATE BackupJobEntity b SET " +
            "b.failedRecordings = b.failedRecordings + 1 " +
            "WHERE b.id = :backupJobId")
    void incrementFailed(@Param("backupJobId") String backupJobId);
}
