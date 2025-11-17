package com.kcn.hikvisionmanager.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.kcn.hikvisionmanager.dto.stream.RunningStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.transform.TransformerFactory;
import java.time.Clock;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Main application configuration for Hikvision Manager.
 * Configures thread pools, caching, object mappers, and security settings.
 */
@Slf4j
@Configuration
@EnableScheduling
@EnableCaching
public class AppConfig {

    // ========================================
    // Cache TTL Configuration (hardcoded by design)
    // ========================================
    // These values are technical parameters that don't require external configuration.
    // They are optimized for Hikvision camera API response patterns.

    private static final int CAMERA_STATUS_TTL = 2;           // 2 seconds - rapid status updates
    private static final int CAMERA_INFO_TTL = 5;             // 5 minutes - static camera properties
    private static final int DOWNLOAD_JOBS_CACHE_TTL = 3;     // 3 hours - active download sessions
    private static final int BATCH_DOWNLOAD_JOBS_CACHE_TTL = 24;  // 24 hours - batch operations
    private static final int HLS_MANIFEST_TTL = 2;            // 2 seconds - live streaming manifest
    private static final int STREAM_SESSION_TTL = 2;          // 2 hours - active stream sessions

    // ========================================
    // Thread Pool Configuration (from properties)
    // ========================================

    @Value("${thread-pool.camera.core-size:2}")
    private int cameraPoolCoreSize;

    @Value("${thread-pool.camera.max-size:4}")
    private int cameraPoolMaxSize;

    @Value("${thread-pool.camera.queue-capacity:100}")
    private int cameraPoolQueueCapacity;

    @Value("${thread-pool.stream.core-size:1}")
    private int streamPoolCoreSize;

    @Value("${thread-pool.stream.max-size:2}")
    private int streamPoolMaxSize;

    @Value("${thread-pool.stream.queue-capacity:0}")
    private int streamPoolQueueCapacity;

    /**
     * Provides system default timezone clock for timestamp operations.
     */
    @Bean
    public Clock systemClock() {
        return Clock.systemDefaultZone();
    }

    /**
     * Thread pool executor for camera API operations.
     * Handles concurrent requests to Hikvision camera API endpoints.
     * Uses CallerRunsPolicy to prevent task rejection under high load.
     */
    @Bean(name = "cameraTaskExecutor", destroyMethod = "shutdown")
    public ThreadPoolTaskExecutor cameraTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(cameraPoolCoreSize);
        executor.setMaxPoolSize(cameraPoolMaxSize);
        executor.setQueueCapacity(cameraPoolQueueCapacity);
        executor.setThreadNamePrefix("camera-task-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        log.info("✅ Camera TaskExecutor initialized (core={}, max={}, queue={})",
                cameraPoolCoreSize, cameraPoolMaxSize, cameraPoolQueueCapacity);
        return executor;
    }

    /**
     * Thread pool executor for video streaming operations.
     * Constrained to camera hardware limitations (single concurrent stream).
     * Uses DiscardPolicy as streams cannot be queued.
     */
    @Bean(name = "streamTaskExecutor", destroyMethod = "shutdown")
    public ThreadPoolTaskExecutor streamTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(streamPoolCoreSize);
        executor.setMaxPoolSize(streamPoolMaxSize);
        executor.setQueueCapacity(streamPoolQueueCapacity);
        executor.setThreadNamePrefix("stream-task-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        executor.initialize();
        log.info("✅ StreamTaskExecutor initialized (core={}, max={}, queue={})",
                streamPoolCoreSize, streamPoolMaxSize, streamPoolQueueCapacity);
        return executor;
    }

    /**
     * Configures Caffeine cache manager with multiple specialized caches.
     * Each cache is optimized for specific data access patterns.
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        // Camera status cache - frequent updates, short TTL
        cacheManager.registerCustomCache("cameraStatus",
                Caffeine.newBuilder()
                        .expireAfterWrite(CAMERA_STATUS_TTL, TimeUnit.SECONDS)
                        .maximumSize(10)
                        .build());

        // Camera info cache - static data, longer TTL
        cacheManager.registerCustomCache("cameraInfo",
                Caffeine.newBuilder()
                        .expireAfterWrite(CAMERA_INFO_TTL, TimeUnit.MINUTES)
                        .maximumSize(2)
                        .build());

        // Download jobs cache - active downloads tracking
        cacheManager.registerCustomCache("downloadJobs",
                Caffeine.newBuilder()
                        .expireAfterWrite(DOWNLOAD_JOBS_CACHE_TTL, TimeUnit.HOURS)
                        .maximumSize(300)
                        .recordStats()
                        .build());

        // Batch download jobs cache - long-running batch operations
        cacheManager.registerCustomCache("batchDownloadJobs",
                Caffeine.newBuilder()
                        .expireAfterWrite(BATCH_DOWNLOAD_JOBS_CACHE_TTL, TimeUnit.HOURS)
                        .maximumSize(50)
                        .recordStats()
                        .build());

        // HLS manifest cache - live streaming playlist
        cacheManager.registerCustomCache("hlsManifest",
                Caffeine.newBuilder()
                        .expireAfterWrite(HLS_MANIFEST_TTL, TimeUnit.SECONDS)
                        .maximumSize(5)
                        .recordStats()
                        .build());

        // Live stream cache - active streaming sessions
        cacheManager.registerCustomCache("liveStreamCache",
                Caffeine.newBuilder()
                        .expireAfterWrite(STREAM_SESSION_TTL, TimeUnit.HOURS)
                        .maximumSize(5)
                        .recordStats()
                        .build());

        log.info("✅ CaffeineCacheManager initialized with {} caches",
                cacheManager.getCacheNames().size());

        return cacheManager;
    }

    /**
     * Direct cache instance for live streaming operations.
     * Provides non-Spring managed access to stream cache.
     */
    @Bean
    @Qualifier("liveStreamCache")
    public Cache<String, RunningStream> directLiveStreamCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(STREAM_SESSION_TTL, TimeUnit.HOURS)
                .maximumSize(5)
                .recordStats()
                .build();
    }

    /**
     * Primary JSON ObjectMapper with Java 8 time support.
     * Configured for lenient deserialization and ISO-8601 date formatting.
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        log.info("✅ ObjectMapper (JSON) initialized as PRIMARY");
        return mapper;
    }

    /**
     * XML mapper for Hikvision camera XML API responses.
     * Configured with XXE attack prevention.
     */
    @Bean("xmlMapper")
    public XmlMapper xmlMapper() {
        XmlMapper mapper = new XmlMapper();

        // XXE Security: Disable external entity processing
        mapper.getFactory()
                .getXMLInputFactory()
                .setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        mapper.getFactory()
                .getXMLInputFactory()
                .setProperty(XMLInputFactory.SUPPORT_DTD, false);

        // Functional configuration
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true);

        log.info("✅ XmlMapper initialized (XXE protection enabled)");

        return mapper;
    }

    /**
     * Transformer factory for XML operations with security hardening.
     * Prevents XXE (XML External Entity) attacks.
     */
    @Bean
    public TransformerFactory transformerFactory() {
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            // Security: Prevent XXE attacks
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            log.info("✅ TransformerFactory initialized (secure mode)");
            return factory;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to configure TransformerFactory securely", e);
        }
    }
}