/**
 * File size formatting utilities
 */

/**
 * Format bytes to human-readable size
 * @param {number} bytes - Size in bytes
 * @param {number} decimals - Number of decimal places
 * @returns {string} - Formatted size (e.g., "1.5 MB")
 */
export function formatFileSize(bytes, decimals = 2) {
  if (bytes === 0) return '0 Bytes';
  if (!bytes || bytes < 0) return '—';

  const k = 1024;
  const dm = decimals < 0 ? 0 : decimals;
  const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];

  const i = Math.floor(Math.log(bytes) / Math.log(k));

  return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i];
}

/**
 * Parse human-readable size to bytes
 * @param {string} sizeStr - Size string (e.g., "1.5 MB")
 * @returns {number} - Size in bytes
 */
export function parseFileSize(sizeStr) {
  if (!sizeStr) return 0;

  const units = {
    'B': 1,
    'BYTES': 1,
    'KB': 1024,
    'MB': 1024 * 1024,
    'GB': 1024 * 1024 * 1024,
    'TB': 1024 * 1024 * 1024 * 1024,
  };

  const match = sizeStr.trim().match(/^([\d.]+)\s*([A-Z]+)$/i);
  if (!match) return 0;

  const value = parseFloat(match[1]);
  const unit = match[2].toUpperCase();
  const multiplier = units[unit] || 1;

  return Math.round(value * multiplier);
}

/**
 * Calculate download speed in MB/s
 * @param {number} bytesDownloaded - Bytes downloaded
 * @param {number} timeElapsedMs - Time elapsed in milliseconds
 * @returns {number} - Speed in MB/s
 */
export function calculateDownloadSpeed(bytesDownloaded, timeElapsedMs) {
  if (!timeElapsedMs || timeElapsedMs <= 0) return 0;

  const timeElapsedSec = timeElapsedMs / 1000;
  const megabytesDownloaded = bytesDownloaded / (1024 * 1024);

  return megabytesDownloaded / timeElapsedSec;
}

/**
 * Calculate ETA (Estimated Time of Arrival)
 * @param {number} bytesRemaining - Bytes remaining
 * @param {number} speedBytesPerSec - Download speed in bytes/sec
 * @returns {string} - Formatted ETA (e.g., "2m 30s")
 */
export function calculateETA(bytesRemaining, speedBytesPerSec) {
  if (!speedBytesPerSec || speedBytesPerSec <= 0) return '—';
  if (bytesRemaining <= 0) return '0s';

  const secondsRemaining = Math.ceil(bytesRemaining / speedBytesPerSec);

  if (secondsRemaining < 60) {
    return `${secondsRemaining}s`;
  }

  const minutes = Math.floor(secondsRemaining / 60);
  const seconds = secondsRemaining % 60;

  if (minutes < 60) {
    return seconds > 0 ? `${minutes}m ${seconds}s` : `${minutes}m`;
  }

  const hours = Math.floor(minutes / 60);
  const mins = minutes % 60;

  return mins > 0 ? `${hours}h ${mins}m` : `${hours}h`;
}

/**
 * Format download speed with unit
 * @param {number} mbps - Speed in MB/s
 * @returns {string} - Formatted speed (e.g., "12.5 MB/s")
 */
export function formatDownloadSpeed(mbps) {
  if (!mbps || mbps <= 0) return '—';

  if (mbps < 1) {
    return `${(mbps * 1024).toFixed(1)} KB/s`;
  }

  return `${mbps.toFixed(2)} Mbps`;
}