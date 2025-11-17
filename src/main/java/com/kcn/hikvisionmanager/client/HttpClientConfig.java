package com.kcn.hikvisionmanager.client;

/**
 * Configuration constants for HTTP client used in Hikvision ISAPI communication.
 * Contains timeout settings, connection pool limits, retry strategy, and buffer sizes.
 */
public final class HttpClientConfig {

    // Connection and response timeouts
    public static final int CONNECT_TIMEOUT_SECONDS = 5;
    public static final int RESPONSE_TIMEOUT_SECONDS = 10;
    public static final int DOWNLOAD_TIMEOUT_MINUTES = 30;

    // Connection pool configuration
    public static final int MAX_TOTAL_CONNECTIONS = 20;
    public static final int MAX_CONNECTIONS_PER_ROUTE = 5;
    public static final int IDLE_CONNECTION_EVICT_SECONDS = 30;

    // Retry strategy
    public static final int RETRY_ATTEMPTS = 3;
    public static final int RETRY_INTERVAL_SECONDS = 1;

    // Download streaming buffer sizes
    public static final int STREAM_BUFFER_SIZE = 64 * 1024;      // 64KB for buffered streams
    public static final int CHUNK_SIZE = 8192;                   // 8KB for read/write chunks
    public static final long PROGRESS_REPORT_INTERVAL = 102400;  // Report progress every ~100KB

    // Private constructor to prevent instantiation
    private HttpClientConfig() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}