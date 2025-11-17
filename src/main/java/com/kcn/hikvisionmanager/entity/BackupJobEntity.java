package com.kcn.hikvisionmanager.entity;

import com.kcn.hikvisionmanager.domain.BackupJobStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "backup_jobs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupJobEntity {

    @Id
    private String id;

    @Column(name = "config_id", nullable = false)
    private String configId;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private BackupJobStatus status;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "search_start_time", nullable = false)
    private LocalDateTime searchStartTime;

    @Column(name = "search_end_time", nullable = false)
    private LocalDateTime searchEndTime;

    @Column(name = "total_recordings", nullable = false)
    private int totalRecordings;

    @Column(name = "completed_recordings")
    private int completedRecordings;

    @Column(name = "failed_recordings")
    private int failedRecordings;

    @Column(name = "total_size_bytes")
    private Long totalSizeBytes;

    @Column(name = "backup_directory", nullable = false, length = 500)
    private String backupDirectory;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "config_id", insertable = false, updatable = false)
    private BackupConfigurationEntity configuration;

    @OneToMany(mappedBy = "backupJob", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BackupRecordingEntity> recordings = new ArrayList<>();
}