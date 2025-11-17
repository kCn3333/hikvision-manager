package com.kcn.hikvisionmanager.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.kcn.hikvisionmanager.entity.CameraEntity;

/**
 * Repository for CameraEntity.
 * Provides CRUD operations for cameras.
 */
@Repository
public interface CameraRepository extends JpaRepository<CameraEntity, String> {
    // Additional queries can be added here in the future if needed.
}
