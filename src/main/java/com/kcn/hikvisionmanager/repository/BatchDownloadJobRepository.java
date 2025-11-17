package com.kcn.hikvisionmanager.repository;

import com.kcn.hikvisionmanager.domain.BatchDownloadJob;
import com.kcn.hikvisionmanager.domain.BatchDownloadStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for batch download jobs using Caffeine cache
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class BatchDownloadJobRepository {

    private final CacheManager cacheManager;
    private static final String CACHE_NAME = "batchDownloadJobs";

    /**
     * Get cache instance
     */
    private Cache getCache() {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache == null) {
            throw new IllegalStateException("Batch download jobs cache not initialized");
        }
        return cache;
    }

    /**
     * Save or update batch job
     */
    public BatchDownloadJob save(BatchDownloadJob batch) {
        getCache().put(batch.getBatchId(), batch);
        log.trace ("Saving batch {} to cache: {}", batch.getBatchId(), getCache());
        if (!exists(batch.getBatchId())) {
            log.warn("⚠️ Overwriting non-existent batch {}", batch.getBatchId());
        }
        return batch;
    }

    /**
     * Find batch by ID
     */
    public Optional<BatchDownloadJob> findById(String batchId) {
        Cache.ValueWrapper wrapper = getCache().get(batchId);
        if (wrapper != null) {
            return Optional.ofNullable((BatchDownloadJob) wrapper.get());
        }
        return Optional.empty();
    }

    /**
     * Find all batches
     */
    public List<BatchDownloadJob> findAll() {
        com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache =
                (com.github.benmanes.caffeine.cache.Cache<Object, Object>) getCache().getNativeCache();

        return nativeCache.asMap().values().stream()
                .map(obj -> (BatchDownloadJob) obj)
                .toList();
    }

    /**
     * Find batches by status
     */
    public List<BatchDownloadJob> findByStatus(BatchDownloadStatus status) {
        return findAll().stream()
                .filter(batch -> batch.getStatus() == status)
                .toList();
    }

    /**
     * Find batches older than specified date
     */
    public List<BatchDownloadJob> findOlderThan(LocalDateTime dateTime) {
        return findAll().stream()
                .filter(batch -> batch.getCreatedAt().isBefore(dateTime))
                .toList();
    }

    /**
     * Delete batch by ID
     */
    public void delete(String batchId) {
        getCache().evict(batchId);
        log.info("Evicted batch {} download job from cache: {}", batchId, getCache());
    }

    /**
     * Delete multiple batches
     */
    public void deleteAll(List<BatchDownloadJob> batchesToDelete) {
        batchesToDelete.forEach(batch -> delete(batch.getBatchId()));
    }

    /**
     * Check if batch exists
     */
    public boolean exists(String batchId) {
        return findById(batchId).isPresent();
    }

    public void saveAndFlush(BatchDownloadJob batch) {
    }
}
