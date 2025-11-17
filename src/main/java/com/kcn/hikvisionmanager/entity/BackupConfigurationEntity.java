package com.kcn.hikvisionmanager.entity;

import com.kcn.hikvisionmanager.domain.BackupTimeRangeStrategy;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "backup_configurations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupConfigurationEntity {

    @Id
    private String id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "camera_id", nullable = false, length = 50)
    private String cameraId;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "cron_expression", nullable = false, length = 100)
    private String cronExpression;

    @Column(name = "retention_days", nullable = false)
    private int retentionDays;

    @Column(name = "time_range_strategy", length = 20)
    @Enumerated(EnumType.STRING)
    private BackupTimeRangeStrategy timeRangeStrategy;

    @Column(name = "backup_path", nullable = false, length = 500)
    private String backupPath;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "last_run_at")
    private LocalDateTime lastRunAt;

    @Column(name = "next_run_at")
    private LocalDateTime nextRunAt;

    @OneToMany(mappedBy = "configuration", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BackupJobEntity> jobs = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}