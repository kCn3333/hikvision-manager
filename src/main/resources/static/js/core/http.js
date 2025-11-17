/**
 * HTTP client with unified error handling
 */

import { showNotification } from './notifications.js';

/**
 * Custom error class for API errors
 */
export class APIError extends Error {
  constructor(message, status, data) {
    super(message);
    this.name = 'APIError';
    this.status = status;
    this.data = data;
  }
}

/**
 * Unified fetch wrapper with error handling
 */
async function request(url, options = {}) {
  const defaultOptions = {
    credentials: "include",
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
    },
  };

  try {
    const response = await fetch(url, { ...defaultOptions, ...options });

    // Handle non-OK responses
    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new APIError(
        errorData.message || `HTTP ${response.status}`,
        response.status,
        errorData
      );
    }

    // Handle empty responses (204 No Content)
    if (response.status === 204) {
      return null;
    }

    // Parse JSON response
    const contentType = response.headers.get('content-type');
    if (contentType?.includes('application/json')) {
      return await response.json();
    }

    // Return text for non-JSON responses
    return await response.text();
  } catch (error) {
    // Network errors or other fetch failures
    if (error instanceof APIError) {
      throw error;
    }

    throw new APIError(
      error.message || 'Network error',
      0,
      { originalError: error }
    );
  }
}

/**
 * HTTP methods
 */
export const http = {
  async get(url, options = {}) {
    return request(url, { ...options, method: 'GET' });
  },

  async post(url, data, options = {}) {
    return request(url, {
      ...options,
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  async put(url, data, options = {}) {
    return request(url, {
      ...options,
      method: 'PUT',
      body: JSON.stringify(data),
    });
  },

  async delete(url, options = {}) {
    return request(url, { ...options, method: 'DELETE' });
  },

  /**
   * POST without body (for actions like start/stop)
   */
  async action(url, options = {}) {
    return request(url, {
      ...options,
      method: 'POST',
      headers: {
        ...options.headers,
      },
    });
  },
};

/**
 * Helper to handle API calls with automatic error notification
 */
export async function apiCall(promise, errorMessage = 'Operation failed') {
  try {
    return await promise;
  } catch (error) {
    console.error(`API Error:`, error);

    // Show user-friendly notification
    const message = error instanceof APIError
      ? error.message
      : errorMessage;

    showNotification(message, 'error');

    throw error;
  }
}