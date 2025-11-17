/**
 * Date formatting utilities
 */

/**
 * Format ISO datetime for datetime-local input
 * @param {string} isoString - ISO 8601 datetime string
 * @returns {string} - Formatted string for datetime-local input
 */
export function formatForDatetimeLocal(isoString) {
  if (!isoString) return '';

  const date = new Date(isoString);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');

  return `${year}-${month}-${day}T${hours}:${minutes}`;
}

/**
 * Parse datetime-local value to ISO string
 * @param {string} localValue - Value from datetime-local input
 * @returns {string} - ISO 8601 string
 */
export function parseFromDatetimeLocal(localValue) {
  if (!localValue) return '';
  return new Date(localValue).toISOString();
}

/**
 * Format duration in seconds to human-readable string
 * @param {number} seconds - Duration in seconds
 * @returns {string} - Formatted duration (e.g., "1h 23m 45s")
 */
export function formatDuration(seconds) {
  if (!seconds || seconds < 0) return '0s';

  const hours = Math.floor(seconds / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  const secs = seconds % 60;

  const parts = [];
  if (hours > 0) parts.push(`${hours}h`);
  if (minutes > 0) parts.push(`${minutes}m`);
  if (secs > 0 || parts.length === 0) parts.push(`${secs}s`);

  return parts.join(' ');
}

/**
 * Format timestamp to readable date
 * @param {string|Date} timestamp - Date or ISO string
 * @param {boolean} includeTime - Include time in output
 * @returns {string} - Formatted date
 */
export function formatDate(timestamp, includeTime = true) {
  if (!timestamp) return '—';

  const date = new Date(timestamp);

  if (isNaN(date.getTime())) return '—';

  const options = {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  };

  if (includeTime) {
    options.hour = '2-digit';
    options.minute = '2-digit';
    options.second = '2-digit';
  }

  return date.toLocaleString('en-GB', options);
}

/**
 * Get relative time string (e.g., "2 hours ago")
 * @param {string|Date} timestamp - Date or ISO string
 * @returns {string} - Relative time string
 */
export function getRelativeTime(timestamp) {
  if (!timestamp) return '—';

  const date = new Date(timestamp);
  const now = new Date();
  const diffMs = now - date;
  const diffSec = Math.floor(diffMs / 1000);

  if (diffSec < 60) return 'just now';
  if (diffSec < 3600) return `${Math.floor(diffSec / 60)} minutes ago`;
  if (diffSec < 86400) return `${Math.floor(diffSec / 3600)} hours ago`;
  return `${Math.floor(diffSec / 86400)} days ago`;
}

/**
 * Set default datetime-local values (last 24 hours)
 * @param {HTMLInputElement} startInput - Start datetime input
 * @param {HTMLInputElement} endInput - End datetime input
 */
export function setDefaultDateRange(startInput, endInput) {
  if (!startInput || !endInput) return;

  const now = new Date();
  const yesterday = new Date(now.getTime() - 24 * 60 * 60 * 1000);

  startInput.value = formatForDatetimeLocal(yesterday.toISOString());
  endInput.value = formatForDatetimeLocal(now.toISOString());
}