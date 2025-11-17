package com.kcn.hikvisionmanager.repository;

import com.kcn.hikvisionmanager.domain.DownloadJob;
import com.kcn.hikvisionmanager.domain.DownloadStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for download jobs using Caffeine cache
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class DownloadJobRepository {

    private final CacheManager cacheManager;
    private static final String CACHE_NAME = "downloadJobs";

    /**
     * Get cache instance
     */
    private Cache getCache() {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache == null) {
            throw new IllegalStateException("Download jobs cache not initialized");
        }
        return cache;
    }

    /**
     * Save or update download job
     */
    public DownloadJob save(DownloadJob job) {
        getCache().put(job.getJobId(), job);
        log.trace ("Saved download job {} to cache: {}", job.getJobId(), getCache());
        return job;
    }

    /**
     * Find job by ID
     */
    public Optional<DownloadJob> findById(String jobId) {
        Cache.ValueWrapper wrapper = getCache().get(jobId);
        if (wrapper != null) {
            return Optional.ofNullable((DownloadJob) wrapper.get());
        }
        return Optional.empty();
    }

    /**
     * Find all jobs
     */
    public List<DownloadJob> findAll() {
        com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache =
                (com.github.benmanes.caffeine.cache.Cache<Object, Object>) getCache().getNativeCache();

        return nativeCache.asMap().values().stream()
                .map(obj -> (DownloadJob) obj)
                .toList();
    }

    /**
     * Find jobs by status
     */
    public List<DownloadJob> findByStatus(DownloadStatus status) {
        return findAll().stream()
                .filter(job -> job.getStatus() == status)
                .toList();
    }

    /**
     * Find jobs older than specified date
     */
    public List<DownloadJob> findOlderThan(LocalDateTime dateTime) {
        return findAll().stream()
                .filter(job -> job.getCreatedAt().isBefore(dateTime))
                .toList();
    }

    /**
     * Delete job by ID
     */
    public void delete(String jobId) {
        getCache().evict(jobId);
        log.info("Evicted download job from cache: {}", jobId);
    }

    /**
     * Delete multiple jobs
     */
    public void deleteAll(List<DownloadJob> jobsToDelete) {
        jobsToDelete.forEach(job -> delete(job.getJobId()));
    }

    /**
     * Count jobs by status
     */
    public long countByStatus(DownloadStatus status) {
        return findAll().stream()
                .filter(job -> job.getStatus() == status)
                .count();
    }

    /**
     * Check if job exists
     */
    public boolean exists(String jobId) {
        return findById(jobId).isPresent();
    }
}
