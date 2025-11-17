/**
 * Central API configuration
 * Single source of truth for all backend endpoints
 */

const API_BASE = '';

export const API = {
  // Camera endpoints
  camera: {
    status: `${API_BASE}/api/camera/status`,
    info: `${API_BASE}/api/camera/info`,
    network: `${API_BASE}/api/camera/network`,
    time: `${API_BASE}/api/camera/time`,
    storage: `${API_BASE}/api/camera/storage`,
    restart: `${API_BASE}/api/camera/management/restart`,
  },

  // Live stream endpoints
  live: {
    start: (channel) => `${API_BASE}/api/live/start?channel=${channel}`,
    stop: `${API_BASE}/api/live/stop`,
    status: `${API_BASE}/api/live/status`,
  },

  // Recordings endpoints
  recordings: {
    search: `${API_BASE}/api/recordings/search`,
    downloadStart: `${API_BASE}/api/recordings/download/start`,
    downloadStartBatch: `${API_BASE}/api/recordings/download/start/batch`,
    downloadStatus: (jobId) => `${API_BASE}/api/recordings/download/${jobId}/status`,
    downloadCancel: (jobId) => `${API_BASE}/api/recordings/download/${jobId}/cancel`,
    downloadFile: (jobId) => `${API_BASE}/api/recordings/download/${jobId}/file`,
  },

  // Batch download endpoints
  batch: {
    start: `${API_BASE}/api/recordings/download/batch`,
    status: (batchId) => `${API_BASE}/api/recordings/download/batch/${batchId}/status`,
    cancel: (batchId) => `${API_BASE}/api/recordings/download/batch/${batchId}/cancel`,
  },

  // Backup endpoints
  backups: {
    list: `${API_BASE}/api/backups/config`,
    get: (id) => `${API_BASE}/api/backups/config/${id}`,
    create: `${API_BASE}/api/backups/config`,
    update: (id) => `${API_BASE}/api/backups/config/${id}`,
    delete: (id) => `${API_BASE}/api/backups/config/${id}`,
    execute: (id) => `${API_BASE}/api/backups/execute/${id}`,
    statistics: `${API_BASE}/api/backups/statistics`,
    jobs: `${API_BASE}/api/backups/jobs`,
  },
};

/**
 * Batch status polling configuration
 */
export const POLLING_CONFIG = {
  interval: 1000, // 1 second
  timeout: 300000, // 5 minutes max
};