package com.kcn.hikvisionmanager.repository;

import com.kcn.hikvisionmanager.entity.BackupConfigurationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BackupConfigurationRepository extends JpaRepository<BackupConfigurationEntity, String> {

    List<BackupConfigurationEntity> findByEnabled(boolean enabled);

    List<BackupConfigurationEntity> findByCameraId(String cameraId);
}
