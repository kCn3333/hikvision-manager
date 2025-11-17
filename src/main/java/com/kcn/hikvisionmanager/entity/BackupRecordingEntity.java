package com.kcn.hikvisionmanager.entity;

import com.kcn.hikvisionmanager.domain.BackupRecordingStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "backup_recordings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupRecordingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "backup_job_id", nullable = false)
    private String backupJobId;

    @Column(name = "recording_id", nullable = false, length = 100)
    private String recordingId;

    @Column(name = "track_id", nullable = false, length = 10)
    private String trackId;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(length = 20)
    private String duration;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private BackupRecordingStatus status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "downloaded_at")
    private LocalDateTime downloadedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "backup_job_id", insertable = false, updatable = false)
    private BackupJobEntity backupJob;
}
