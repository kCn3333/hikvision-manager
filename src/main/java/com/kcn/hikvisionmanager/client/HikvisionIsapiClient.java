package com.kcn.hikvisionmanager.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.kcn.hikvisionmanager.config.CameraConfig;
import com.kcn.hikvisionmanager.exception.CameraOfflineException;
import com.kcn.hikvisionmanager.exception.CameraParsingException;
import com.kcn.hikvisionmanager.exception.CameraRequestException;
import com.kcn.hikvisionmanager.exception.CameraUnauthorizedException;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.HttpRequestRetryStrategy;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.apache.hc.client5.http.config.RequestConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/**
 * Hikvision ISAPI HTTP client that provides GET/POST/PUT operations with XML parsing.
 * Handles digest authentication, automatic retries, connection pooling, and error mapping.
 * Uses shared HttpClient instance for efficient resource management.
 */
@Slf4j
@Component
public class HikvisionIsapiClient {

    private final CameraConfig cameraConfig;
    private final CloseableHttpClient httpClient;
    @Qualifier("xmlMapper")
    private final XmlMapper xmlMapper;

    /**
     * Constructor creates single HttpClient instance that will be reused for all requests.
     * Configures digest authentication, connection pooling, and retry strategy.
     */
    public HikvisionIsapiClient(CameraConfig cameraConfig, XmlMapper xmlMapper) {
        this.cameraConfig = cameraConfig;
        this.xmlMapper = xmlMapper;
        this.httpClient = createHttpClient();
        log.info("✅ HikvisionIsapiClient initialized for {}:{}", cameraConfig.getIp(), cameraConfig.getPort());
    }

    /**
     * Creates configured HttpClient with digest authentication and connection pooling.
     * This method is called once during initialization.
     *
     * @return Configured CloseableHttpClient instance
     */
    private CloseableHttpClient createHttpClient() {
        // Setup digest authentication credentials
        BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(cameraConfig.getIp(), cameraConfig.getPort()),
                new UsernamePasswordCredentials(cameraConfig.getUsername(), cameraConfig.getPassword().toCharArray())
        );

        // Configure connection and response timeouts
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(HttpClientConfig.CONNECT_TIMEOUT_SECONDS))
                .setResponseTimeout(Timeout.ofSeconds(HttpClientConfig.RESPONSE_TIMEOUT_SECONDS))
                .build();

        // Setup connection pool for concurrent requests
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(HttpClientConfig.MAX_TOTAL_CONNECTIONS);
        connectionManager.setDefaultMaxPerRoute(HttpClientConfig.MAX_CONNECTIONS_PER_ROUTE);

        return HttpClients.custom()
                .setDefaultCredentialsProvider(credsProvider)
                .setDefaultRequestConfig(requestConfig)
                .setConnectionManager(connectionManager)
                .setConnectionManagerShared(true)
                .setRetryStrategy(retryStrategy())
                .evictExpiredConnections()
                .evictIdleConnections(TimeValue.ofSeconds(HttpClientConfig.IDLE_CONNECTION_EVICT_SECONDS))
                .build();
    }

    /**
     * Configures automatic retry strategy for transient network failures.
     *
     * @return HttpRequestRetryStrategy with configured retry attempts and interval
     */
    private HttpRequestRetryStrategy retryStrategy() {
        return new DefaultHttpRequestRetryStrategy(
                HttpClientConfig.RETRY_ATTEMPTS,
                TimeValue.ofSeconds(HttpClientConfig.RETRY_INTERVAL_SECONDS)
        );
    }

    /**
     * Exposes HttpClient as Spring Bean for use by other components (e.g., HttpDownloadService).
     *
     * @return Shared CloseableHttpClient instance
     */
    @Bean
    public CloseableHttpClient httpClient() {
        return this.httpClient;
    }

    /**
     * Executes HTTP GET request and parses XML response to specified type.
     * Handles authentication errors, HTTP errors, and network failures.
     *
     * @param url ISAPI endpoint URL
     * @param responseType Class type to deserialize XML response into
     * @param <T> Type of expected response object
     * @return Parsed response object
     * @throws CameraUnauthorizedException If authentication fails (401/403)
     * @throws CameraRequestException If HTTP request fails with 4xx/5xx status
     * @throws CameraOfflineException If camera is unreachable or times out
     * @throws CameraParsingException If XML parsing fails
     */
    public <T> T executeGet(String url, Class<T> responseType) {
        log.debug("GET request to: {}", url);
        HttpGet httpGet = new HttpGet(url);
        try {
            return httpClient.execute(httpGet, response -> {
                int statusCode = response.getCode();
                log.debug("Response status: {}", statusCode);

                // Handle authentication errors
                if (statusCode == 401 || statusCode == 403) {
                    throw new CameraUnauthorizedException("Unauthorized access to camera " + cameraConfig.getIp());
                }

                // Handle other HTTP errors
                if (statusCode >= 400) {
                    String errorBody = EntityUtils.toString(response.getEntity());
                    throw new CameraRequestException("Camera request failed with status " + statusCode + ": " + errorBody);
                }

                // Parse successful response
                String responseBody = EntityUtils.toString(response.getEntity());
                log.debug("GET successful: {} bytes received", responseBody.length());

                try {
                    return xmlMapper.readValue(responseBody, responseType);
                } catch (JsonProcessingException e) {
                    throw new CameraParsingException("Failed to parse camera XML response", e);
                }
            });
        } catch (UnknownHostException | SocketTimeoutException e) {
            throw new CameraOfflineException("Camera at " + cameraConfig.getIp() + " is offline or unreachable", e);
        } catch (IOException e) {
            throw new CameraRequestException("I/O error during camera GET request: " + e.getMessage(), e);
        }
    }

    /**
     * Executes HTTP PUT request with XML body and parses XML response to specified type.
     * Serializes request object to XML before sending.
     *
     * @param url ISAPI endpoint URL
     * @param requestBody Object to serialize as XML request body
     * @param responseType Class type to deserialize XML response into
     * @param <T> Type of expected response object
     * @return Parsed response object
     * @throws CameraUnauthorizedException If authentication fails (401/403)
     * @throws CameraRequestException If HTTP request fails with 4xx/5xx status
     * @throws CameraOfflineException If camera is unreachable or times out
     * @throws CameraParsingException If XML parsing fails
     */
    public <T> T executePut(String url, Object requestBody, Class<T> responseType) {
        log.debug("PUT request to: {}", url);
        HttpPut httpPut = new HttpPut(url);

        try {
            String xmlBody = xmlMapper.writeValueAsString(requestBody);
            httpPut.setEntity(new StringEntity(xmlBody, ContentType.APPLICATION_XML));
            log.trace("Request body: {}", xmlBody);

            return httpClient.execute(httpPut, response -> {
                int statusCode = response.getCode();
                log.debug("Response status: {}", statusCode);

                if (statusCode == 401 || statusCode == 403) {
                    throw new CameraUnauthorizedException("Unauthorized access to camera " + cameraConfig.getIp());
                }

                if (statusCode >= 400) {
                    String errorBody = EntityUtils.toString(response.getEntity());
                    throw new CameraRequestException("PUT request failed with status " + statusCode + ": " + errorBody);
                }

                String responseBody = EntityUtils.toString(response.getEntity());
                log.debug("PUT successful: {} bytes received", responseBody.length());

                try {
                    return xmlMapper.readValue(responseBody, responseType);
                } catch (JsonProcessingException e) {
                    throw new CameraParsingException("Failed to parse camera XML response", e);
                }
            });
        } catch (UnknownHostException | SocketTimeoutException e) {
            throw new CameraOfflineException("Camera at " + cameraConfig.getIp() + " is offline or unreachable", e);
        } catch (IOException e) {
            throw new CameraRequestException("I/O error during camera PUT request: " + e.getMessage(), e);
        }
    }

    /**
     * Executes HTTP POST request with object serialized as XML body.
     * Parses XML response to specified type.
     *
     * @param url ISAPI endpoint URL
     * @param requestBody Object to serialize as XML request body
     * @param responseType Class type to deserialize XML response into
     * @param <T> Type of expected response object
     * @return Parsed response object
     * @throws CameraUnauthorizedException If authentication fails (401/403)
     * @throws CameraRequestException If HTTP request fails with 4xx/5xx status
     * @throws CameraOfflineException If camera is unreachable or times out
     * @throws CameraParsingException If XML parsing fails
     */
    public <T> T executePost(String url, Object requestBody, Class<T> responseType) {
        log.debug("POST request to: {}", url);
        HttpPost httpPost = new HttpPost(url);

        try {
            String xmlBody = xmlMapper.writeValueAsString(requestBody);
            httpPost.setEntity(new StringEntity(xmlBody, ContentType.APPLICATION_XML));
            log.trace("Request body: {}", xmlBody);

            return httpClient.execute(httpPost, response -> {
                int statusCode = response.getCode();
                log.debug("Response status: {}", statusCode);

                if (statusCode == 401 || statusCode == 403) {
                    throw new CameraUnauthorizedException("Unauthorized access to camera " + cameraConfig.getIp());
                }

                if (statusCode >= 400) {
                    String errorBody = EntityUtils.toString(response.getEntity());
                    throw new CameraRequestException("POST request failed with status " + statusCode + ": " + errorBody);
                }

                String responseBody = EntityUtils.toString(response.getEntity());
                log.debug("POST successful: {} bytes received", responseBody.length());

                try {
                    return xmlMapper.readValue(responseBody, responseType);
                } catch (JsonProcessingException e) {
                    throw new CameraParsingException("Failed to parse camera XML response", e);
                }
            });
        } catch (UnknownHostException | SocketTimeoutException e) {
            throw new CameraOfflineException("Camera at " + cameraConfig.getIp() + " is offline or unreachable", e);
        } catch (IOException e) {
            throw new CameraRequestException("I/O error during camera POST request: " + e.getMessage(), e);
        }
    }

    /**
     * Executes HTTP POST request with raw XML string as body.
     * Useful when XML is pre-formatted or dynamically generated.
     *
     * @param url ISAPI endpoint URL
     * @param xmlBody Pre-formatted XML string to send as request body
     * @param responseType Class type to deserialize XML response into
     * @param <T> Type of expected response object
     * @return Parsed response object
     * @throws CameraUnauthorizedException If authentication fails (401/403)
     * @throws CameraRequestException If HTTP request fails with 4xx/5xx status
     * @throws CameraOfflineException If camera is unreachable or times out
     * @throws CameraParsingException If XML parsing fails
     */
    public <T> T executePost(String url, String xmlBody, Class<T> responseType) {
        log.debug("POST request to: {}", url);
        HttpPost httpPost = new HttpPost(url);

        try {
            httpPost.setEntity(new StringEntity(xmlBody, ContentType.APPLICATION_XML));
            log.trace("Request body: {}", xmlBody);

            return httpClient.execute(httpPost, response -> {
                int statusCode = response.getCode();
                log.debug("Response status: {}", statusCode);

                if (statusCode == 401 || statusCode == 403) {
                    throw new CameraUnauthorizedException("Unauthorized access to camera " + cameraConfig.getIp());
                }

                if (statusCode >= 400) {
                    String errorBody = EntityUtils.toString(response.getEntity());
                    throw new CameraRequestException("POST request failed with status " + statusCode + ": " + errorBody);
                }

                String responseBody = EntityUtils.toString(response.getEntity());
                log.debug("POST successful: {} bytes received", responseBody.length());

                try {
                    return xmlMapper.readValue(responseBody, responseType);
                } catch (JsonProcessingException e) {
                    throw new CameraParsingException("Failed to parse camera XML response", e);
                }
            });
        } catch (UnknownHostException | SocketTimeoutException e) {
            throw new CameraOfflineException("Camera at " + cameraConfig.getIp() + " is offline or unreachable", e);
        } catch (IOException e) {
            throw new CameraRequestException("I/O error during camera POST request: " + e.getMessage(), e);
        }
    }

    /**
     * Cleanup method called when Spring context is destroyed.
     * Closes HttpClient and releases all connection pool resources.
     */
    @PreDestroy
    public void destroy() {
        try {
            httpClient.close();
            log.info("🔴 HikvisionIsapiClient closed");
        } catch (IOException e) {
            log.error("Error closing HttpClient", e);
        }
    }
}