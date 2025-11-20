# Hikvision Manager API Documentation

Complete documentation of all REST API endpoints in the Hikvision Manager application.

## Base URL

All API endpoints are prefixed with `/api` unless otherwise specified.

---

## Table of Contents

1. [Camera Information](#camera-information)
2. [Camera Management](#camera-management)
3. [Live Streaming](#live-streaming)
4. [Recordings](#recordings)
5. [Recording Downloads](#recording-downloads)
6. [Backups](#backups)
7. [Backup Statistics](#backup-statistics)
8. [Stream Files](#stream-files)

---

## Camera Information

### Get Device Information

Retrieves detailed information about the camera device.

**Endpoint:** `GET /api/camera/info`

**Response:** `200 OK`

```json
{
  "version": "string",
  "deviceName": "string",
  "deviceDescription": "string",
  "manufacturer": "string",
  "deviceLocation": "string",
  "model": "string",
  "serialNumber": "string",
  "firmwareVersion": "string",
  "firmwareReleasedDate": "string"
}
```

---

### Get Camera Status

Retrieves the health and system status of the camera.

**Endpoint:** `GET /api/camera/status`

**Response:** `200 OK`

```json
{
  "online": true,
  "cpuUsage": 45,
  "memoryUsage": 62,
  "uptimeMinutes": 12345,
  "status": "string",
  "currentDeviceTime": "string"
}
```

---

### Get Network Information

Retrieves network configuration and status of the camera.

**Endpoint:** `GET /api/camera/network`

**Response:** `200 OK`

```json
{
  "ipAddress": "192.168.1.100",
  "macAddress": "00:11:22:33:44:55",
  "subnetMask": "255.255.255.0",
  "defaultGateway": "192.168.1.1",
  "dnsServer": "8.8.8.8",
  "speed": "1000",
  "mtu": "1500",
  "duplex": "full"
}
```

---

### Get Storage Information

Retrieves storage device information and usage statistics.

**Endpoint:** `GET /api/camera/storage`

**Response:** `200 OK`

```json
{
  "id": 1,
  "name": "string",
  "path": "string",
  "type": "string",
  "status": "string",
  "capacity": "string",
  "usage": "string",
  "property": "string",
  "formatType": "string",
  "mountTypes": ["string"],
  "authentications": ["string"]
}
```

---

### Get Time Information

Retrieves time configuration and current time settings.

**Endpoint:** `GET /api/camera/time`

**Response:** `200 OK`

```json
{
  "timeMode": "string",
  "localTime": "string",
  "formattedLocalTime": "string",
  "timeZone": "string",
  "formattedTimeZone": "string",
  "ntpEnabled": true,
  "statusMessage": "string"
}
```

---

### Get All Channels

Retrieves information about all camera channels/tracks.

**Endpoint:** `GET /api/camera/channels`

**Response:** `200 OK`

```json
[
  {
    "channelId": "string",
    "recordingType": "TIMED | MANUAL | ALARM",
    "codec": "string",
    "bitrate": "string",
    "resolution": "string",
    "framerate": "string",
    "enabled": true,
    "hasSchedule": true,
    "scheduleDescription": "string"
  }
]
```

---

## Camera Management

### Restart Camera

Restarts the camera device.

**Endpoint:** `POST /api/camera/management/restart`

**Response:** `200 OK`

```json
"Camera restarted successfully"
```

**Error Response:** `200 OK` (with failure message)

```json
"Failed to restart camera"
```

---

## Live Streaming

### Start Live Stream

Starts an HLS live stream for the specified channel.

**Endpoint:** `POST /api/live/start`

**Query Parameters:**
- `channel` (required): Channel ID to stream

**Response:** `200 OK`

```json
{
  "status": "success",
  "playlistUrl": "/streams/{sessionId}/playlist.m3u8",
  "message": "Stream started successfully",
  "details": null
}
```

---

### Stop Live Stream

Stops the active HLS stream for the current session.

**Endpoint:** `POST /api/live/stop`

**Response:** `200 OK`

```json
{
  "status": "success",
  "playlistUrl": null,
  "message": "Stream stopped",
  "details": null
}
```

---

### Get Stream Status

Retrieves the current status of the live stream for the session.

**Endpoint:** `GET /api/live/status`

**Response:** `200 OK`

**Active Stream:**
```json
{
  "active": true,
  "channel": "101",
  "startTime": "2024-01-15T10:30:00",
  "viewers": 2,
  "error": null
}
```

**No Active Stream:**
```json
{
  "active": false,
  "channel": null,
  "startTime": null,
  "viewers": 0,
  "error": null
}
```

---

## Recordings

### Search Recordings

Searches for recordings within a specified time range.

**Endpoint:** `POST /api/recordings/search`

**Request Body:**
```json
{
  "startTime": "2024-01-15T00:00:00",
  "endTime": "2024-01-15T23:59:59",
  "page": 0,
  "pageSize": 10
}
```

**Validation Rules:**
- `startTime`: Required, must be in the past or present
- `endTime`: Required, must be in the past or present, must be after `startTime`
- `page`: Minimum 0 (default: 0)
- `pageSize`: Minimum 1 (default: 10)

**Response:** `200 OK`

```json
{
  "recordings": [
    {
      "recordingId": "string",
      "trackId": "string",
      "startTime": "2024-01-15T10:30:00",
      "endTime": "2024-01-15T10:45:00",
      "duration": "15:00",
      "codec": "H.264",
      "playbackUrl": "string",
      "fileSize": "250 MB",
      "hasMoreResults": false
    }
  ],
  "currentPage": 0,
  "pageSize": 10,
  "totalMatches": 25,
  "hasMore": true,
  "searchId": "search-uuid"
}
```

---

### Get Recent Recordings

Retrieves recent recordings from the last N hours.

**Endpoint:** `GET /api/recordings/recent`

**Query Parameters:**
- `hours` (optional): Number of hours to look back (default: 24)
- `pageSize` (optional): Number of results per page (default: 10)

**Response:** `200 OK`

Same structure as `POST /api/recordings/search` response.

---

### Get Recordings by Date

Retrieves all recordings for a specific date.

**Endpoint:** `GET /api/recordings/date/{date}`

**Path Parameters:**
- `date`: Date and time in ISO format (e.g., `2024-01-15T00:00:00`)

**Query Parameters:**
- `pageSize` (optional): Number of results per page (default: 10)

**Response:** `200 OK`

Same structure as `POST /api/recordings/search` response.

---

### Search Next Page

Fetches the next page of recordings based on the previous search result.

**Endpoint:** `POST /api/recordings/search/next`

**Request Body:**
```json
{
  "recordings": [...],
  "currentPage": 0,
  "pageSize": 10,
  "totalMatches": 25,
  "hasMore": true,
  "searchId": "search-uuid"
}
```

**Query Parameters:**
- `pageSize` (optional): Number of results per page (default: 10)

**Response:** `200 OK`

Same structure as `POST /api/recordings/search` response.

---

## Recording Downloads

### Start Single Download

Starts downloading a single recording.

**Endpoint:** `POST /api/recordings/download/start`

**Request Body:**
```json
{
  "recordingId": "string",
  "trackId": "string",
  "startTime": "2024-01-15T10:30:00",
  "endTime": "2024-01-15T10:45:00",
  "duration": "15:00",
  "codec": "H.264",
  "playbackUrl": "string",
  "fileSize": "250 MB",
  "hasMoreResults": false
}
```

**Response:** `201 Created`

```json
{
  "batchId": "batch-uuid",
  "statusUrl": "/api/recordings/download/batch/{batchId}/status"
}
```

**Error Response:** `500 Internal Server Error`

---

### Start Batch Download

Starts downloading multiple recordings as a batch.

**Endpoint:** `POST /api/recordings/download/start/batch`

**Request Body:**
```json
[
  {
    "recordingId": "string",
    "trackId": "string",
    "startTime": "2024-01-15T10:30:00",
    "endTime": "2024-01-15T10:45:00",
    "duration": "15:00",
    "codec": "H.264",
    "playbackUrl": "string",
    "fileSize": "250 MB",
    "hasMoreResults": false
  }
]
```

**Response:** `201 Created`

```json
{
  "batchId": "batch-uuid",
  "statusUrl": "/api/recordings/download/batch/{batchId}/status"
}
```

**Error Response:** `500 Internal Server Error`

---

### Start Direct Download

Searches for recordings and downloads all matches as a batch (convenience endpoint).

**Endpoint:** `POST /api/recordings/download/start-direct`

**Request Body:**
```json
{
  "startTime": "2024-01-15T00:00:00",
  "endTime": "2024-01-15T23:59:59",
  "page": 0,
  "pageSize": 10
}
```

**Response:** `201 Created`

```json
{
  "batchId": "batch-uuid",
  "statusUrl": "/api/recordings/download/batch/{batchId}/status",
  "total": 5,
  "message": "Started batch download of 5 recordings"
}
```

**Error Responses:**
- `400 Bad Request`: No recordings found in specified time range
- `500 Internal Server Error`

---

### Get Download Status

Retrieves the status of a single download job.

**Endpoint:** `GET /api/recordings/download/{jobId}/status`

**Path Parameters:**
- `jobId`: Download job identifier

**Response:** `200 OK`

```json
{
  "jobId": "job-uuid",
  "status": "QUEUED | DOWNLOADING | COMPLETED | FAILED | CANCELLED",
  "message": "string",
  "filePath": "string",
  "progressPercent": 45,
  "downloadSpeed": 2.5,
  "downloadedSize": "112 MB",
  "totalSize": "249 MB",
  "eta": "2m 30s",
  "downloadUrl": "/api/recordings/download/{jobId}/file",
  "fileName": "recording_2024-01-15_10-30-00.mp4",
  "actualFileSize": "249 MB",
  "errorMessage": null
}
```

**Error Responses:**
- `404 Not Found`: Download job not found
- `500 Internal Server Error`

---

### Download File

Downloads the completed recording file.

**Endpoint:** `GET /api/recordings/download/{jobId}/file`

**Path Parameters:**
- `jobId`: Download job identifier

**Response:** `200 OK`

- **Content-Type:** `application/octet-stream`
- **Content-Disposition:** `attachment; filename="recording.mp4"`
- **Body:** Binary file stream

**Error Responses:**
- `404 Not Found`: Download job not found
- `409 Conflict`: Download not completed yet
- `410 Gone`: File no longer available
- `500 Internal Server Error`

---

### Cancel Download

Cancels a single download job.

**Endpoint:** `DELETE /api/recordings/download/{jobId}/cancel`

**Path Parameters:**
- `jobId`: Download job identifier

**Response:** `200 OK`

```json
{
  "jobId": "job-uuid",
  "message": "Download cancelled successfully"
}
```

**Error Responses:**
- `400 Bad Request`: Cannot cancel download (invalid state)
- `404 Not Found`: Download job not found
- `500 Internal Server Error`

---

### Get Batch Download Status

Retrieves the status of a batch download operation.

**Endpoint:** `GET /api/recordings/download/batch/{batchId}/status`

**Path Parameters:**
- `batchId`: Batch download identifier

**Response:** `200 OK`

```json
{
  "batchId": "batch-uuid",
  "status": "QUEUED | IN_PROGRESS | COMPLETED | FAILED | CANCELLED",
  "total": 5,
  "completed": 3,
  "inProgress": 1,
  "failed": 0,
  "queued": 1,
  "message": "string",
  "path": "string",
  "createdAt": "2024-01-15T10:30:00",
  "completedAt": null,
  "jobs": [
    {
      "jobId": "job-uuid",
      "status": "COMPLETED",
      "message": "string",
      "filePath": "string",
      "progressPercent": 100,
      "downloadSpeed": 0.0,
      "downloadedSize": "249 MB",
      "totalSize": "249 MB",
      "eta": null,
      "downloadUrl": "/api/recordings/download/{jobId}/file",
      "fileName": "recording.mp4",
      "actualFileSize": "249 MB",
      "errorMessage": null
    }
  ]
}
```

**Error Responses:**
- `404 Not Found`: Batch not found
- `500 Internal Server Error`

---

### Cancel Batch Download

Cancels an entire batch download operation.

**Endpoint:** `DELETE /api/recordings/download/batch/{batchId}/cancel`

**Path Parameters:**
- `batchId`: Batch download identifier

**Response:** `200 OK`

```json
{
  "batchId": "batch-uuid",
  "message": "Batch download cancelled successfully"
}
```

**Error Responses:**
- `404 Not Found`: Batch not found
- `500 Internal Server Error`

---

## Backups

### Create Backup Configuration

Creates a new backup configuration.

**Endpoint:** `POST /api/backups/config`

**Request Body:**
```json
{
  "id": null,
  "name": "Daily Backup",
  "cameraId": "cam01",
  "enabled": true,
  "scheduleType": "DAILY",
  "time": "02:30",
  "retentionDays": 7,
  "notifyOnComplete": true,
  "dayOfWeek": null,
  "timeRangeStrategy": null
}
```

**Validation Rules:**
- `name`: Required, 3-100 characters
- `cameraId`: Required
- `scheduleType`: Required (DAILY, WEEKLY, CUSTOM)
- `time`: Required, format HH:mm (e.g., "02:30")
- `retentionDays`: Required, 1-30
- `dayOfWeek`: Used only for WEEKLY schedule

**Response:** `201 Created`

Returns the created `BackupConfigDTO` with generated `id`.

---

### Get All Backup Configurations

Retrieves all backup configurations.

**Endpoint:** `GET /api/backups/config`

**Response:** `200 OK`

```json
[
  {
    "id": "config-uuid",
    "name": "Daily Backup",
    "cameraId": "cam01",
    "enabled": true,
    "scheduleType": "DAILY",
    "time": "02:30",
    "retentionDays": 7,
    "notifyOnComplete": true,
    "dayOfWeek": null,
    "timeRangeStrategy": null
  }
]
```

---

### Get Backup Configuration

Retrieves a specific backup configuration by ID.

**Endpoint:** `GET /api/backups/config/{id}`

**Path Parameters:**
- `id`: Configuration identifier

**Response:** `200 OK`

Returns a single `BackupConfigDTO` object.

---

### Update Backup Configuration

Updates an existing backup configuration.

**Endpoint:** `PUT /api/backups/config/{id}`

**Path Parameters:**
- `id`: Configuration identifier

**Request Body:**
Same structure as `POST /api/backups/config`

**Response:** `200 OK`

Returns the updated `BackupConfigDTO`.

---

### Delete Backup Configuration

Deletes a backup configuration.

**Endpoint:** `DELETE /api/backups/config/{id}`

**Path Parameters:**
- `id`: Configuration identifier

**Response:** `200 OK`

```json
"Backup configuration deleted successfully"
```

---

### Execute Backup

Manually triggers a backup for a specific configuration.

**Endpoint:** `POST /api/backups/execute/{configId}`

**Path Parameters:**
- `configId`: Configuration identifier

**Response:** `200 OK`

```json
{
  "batchId": "batch-uuid"
}
```

---

### Get All Backup Jobs

Retrieves all backup jobs with pagination.

**Endpoint:** `GET /api/backups/jobs`

**Query Parameters:**
- `page` (optional): Page number (default: 0)
- `size` (optional): Page size (default: 10)
- `sort` (optional): Sort criteria (default: `startedAt,desc`)

**Response:** `200 OK`

```json
{
  "content": [
    {
      "jobId": "job-uuid",
      "cameraId": "cam01",
      "startedAt": "2024-01-15T02:30:00",
      "endTime": "2024-01-15T03:15:00",
      "totalFiles": 25,
      "completedFiles": 25,
      "totalBytes": 1073741824,
      "status": "SUCCESS",
      "logPath": "/path/to/log.txt"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "sort": {
      "sorted": true,
      "unsorted": false
    }
  },
  "totalElements": 50,
  "totalPages": 5,
  "last": false,
  "first": true,
  "numberOfElements": 10,
  "size": 10,
  "number": 0,
  "empty": false
}
```

---

### Get Backup Jobs for Configuration

Retrieves all backup jobs for a specific configuration.

**Endpoint:** `GET /api/backups/{configId}/jobs`

**Path Parameters:**
- `configId`: Configuration identifier

**Response:** `200 OK`

```json
[
  {
    "jobId": "job-uuid",
    "cameraId": "cam01",
    "startedAt": "2024-01-15T02:30:00",
    "endTime": "2024-01-15T03:15:00",
    "totalFiles": 25,
    "completedFiles": 25,
    "totalBytes": 1073741824,
    "status": "SUCCESS",
    "logPath": "/path/to/log.txt"
  }
]
```

---

## Backup Statistics

### Get Overall Statistics

Retrieves overall backup statistics across all configurations.

**Endpoint:** `GET /api/backups/statistics`

**Response:** `200 OK`

```json
{
  "totalConfigurations": 5,
  "activeConfigurations": 3,
  "totalJobs": 150,
  "successfulJobs": 145,
  "failedJobs": 5,
  "totalFilesBackedUp": 3750,
  "totalBytesBackedUp": 16106127360,
  "averageJobDuration": "45 minutes"
}
```

---

### Get Configuration Statistics

Retrieves statistics for a specific backup configuration.

**Endpoint:** `GET /api/backups/statistics/config/{configId}`

**Path Parameters:**
- `configId`: Configuration identifier

**Response:** `200 OK`

```json
{
  "configId": "config-uuid",
  "configName": "Daily Backup",
  "totalJobs": 30,
  "successfulJobs": 28,
  "failedJobs": 2,
  "totalFilesBackedUp": 750,
  "totalBytesBackedUp": 3221225472,
  "lastJobTime": "2024-01-15T02:30:00",
  "lastJobStatus": "SUCCESS"
}
```

---

### Get Job Statistics

Retrieves detailed statistics for a specific backup job.

**Endpoint:** `GET /api/backups/statistics/job/{jobId}`

**Path Parameters:**
- `jobId`: Job identifier

**Response:** `200 OK`

```json
{
  "jobId": "job-uuid",
  "configId": "config-uuid",
  "configName": "Daily Backup",
  "cameraId": "cam01",
  "startedAt": "2024-01-15T02:30:00",
  "endTime": "2024-01-15T03:15:00",
  "duration": "45 minutes",
  "totalFiles": 25,
  "completedFiles": 25,
  "failedFiles": 0,
  "totalBytes": 1073741824,
  "averageFileSize": 42949672,
  "status": "SUCCESS",
  "logPath": "/path/to/log.txt"
}
```

---

## Stream Files

### Serve Stream File

Serves HLS stream files (playlist and segments) for live streaming.

**Endpoint:** `GET /streams/{sessionId}/{filename}`

**Path Parameters:**
- `sessionId`: Session identifier
- `filename`: File name (e.g., `playlist.m3u8` or `segment001.ts`)

**Response:** `200 OK`

- **Content-Type:** 
  - `application/vnd.apple.mpegurl` for `.m3u8` files
  - `video/mp2t` for `.ts` files
- **Headers:**
  - `Cache-Control: no-cache, no-store, must-revalidate`
  - `Pragma: no-cache`
  - `Expires: 0`
  - `Access-Control-Allow-Origin: *`
- **Body:** File content (binary for `.ts`, text for `.m3u8`)

**Error Response:** `404 Not Found` - File not found

---

## Error Responses

All endpoints may return the following error responses:

### 400 Bad Request
Invalid request parameters or validation errors.

```json
{
  "error": "Validation failed",
  "message": "End time must be after start time"
}
```

### 404 Not Found
Resource not found.

### 409 Conflict
Resource conflict (e.g., download not completed).

### 410 Gone
Resource no longer available.

### 500 Internal Server Error
Server error.

```json
{
  "error": "Internal server error",
  "message": "An unexpected error occurred"
}
```

---

## Authentication

All API endpoints require authentication. The application uses Spring Security for authentication. Ensure you include valid session cookies or authentication tokens in your requests.

---

## Notes

1. **Time Formats:** All datetime fields use ISO 8601 format (e.g., `2024-01-15T10:30:00`).

2. **Pagination:** Pagination parameters follow Spring Data conventions:
   - `page`: Zero-based page index
   - `size`: Number of elements per page
   - `sort`: Sort criteria (e.g., `startedAt,desc`)

3. **Session Management:** Live streaming endpoints use HTTP session IDs to track individual user streams.

4. **Download Status:** Download status can be polled using the status endpoints. The status will transition through: `QUEUED` → `DOWNLOADING` → `COMPLETED` or `FAILED`.

5. **Backup Status:** Backup job status values: `RUNNING`, `SUCCESS`, `FAILED`.

6. **Batch Downloads:** When starting a batch download, all recordings are queued and processed sequentially. Use the batch status endpoint to monitor progress.

---

## Version

This documentation corresponds to the current version of the Hikvision Manager application.

