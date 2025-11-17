/**
 * Camera Restart Management
 */

import { API } from '../core/api.js';
import { http, apiCall } from '../core/http.js';
import { showNotification, confirm } from '../core/notifications.js';
import { cameraStatus } from './camera-status.js';

class CameraRestartManager {
  constructor() {
    this.modal = null;
    this.restartInProgress = false;
    this.checkInterval = null;
  }

  /**
   * Initialize restart manager
   */
  init() {
    const restartBtn = document.getElementById('restart-btn');
    const modalEl = document.getElementById('restartModal');

    if (!restartBtn || !modalEl) {
      console.warn('CameraRestartManager: elements not found');
      return;
    }

    // Initialize Bootstrap modal
    this.modal = new bootstrap.Modal(modalEl);

    // Setup event listener
    restartBtn.addEventListener('click', () => this.initiateRestart());
  }

  /**
   * Initiate camera restart
   */
  async initiateRestart() {
    if (this.restartInProgress) {
      return;
    }

    if (!confirm('Are you sure you want to restart the camera?', 'Restart Camera')) {
      return;
    }

    this.restartInProgress = true;
    const restartBtn = document.getElementById('restart-btn');
    restartBtn.disabled = true;

    // Show modal and blur background
    document.body.classList.add('restart-blur');
    this.modal.show();

    try {
      await apiCall(
        http.action(API.camera.restart),
        'Failed to initiate restart'
      );

      console.log('Restart initiated.');

      // Wait 40 seconds before checking status
      await this.delay(40000);

      // Start checking for camera to come back online
      await this.waitForCameraOnline();
    } catch (error) {
      this.restartFailed();
    }
  }

  /**
   * Wait for camera to come back online
   */
  async waitForCameraOnline() {
    const maxWaitTime = 120; // seconds
    const checkInterval = 5; // seconds
    let elapsed = 0;

    return new Promise((resolve) => {
      this.checkInterval = setInterval(async () => {
        elapsed += checkInterval;

        try {
          // Force a status check
          await cameraStatus.updateStatus();

          // Check if camera is online
          const dot = document.getElementById('connection-dot');
          if (dot && dot.classList.contains('connected')) {
            clearInterval(this.checkInterval);
            this.checkInterval = null;
            this.restartComplete();
            resolve();
          }
        } catch (error) {
          console.log('Camera still offline...');
        }

        // Timeout after max wait time
        if (elapsed >= maxWaitTime) {
          clearInterval(this.checkInterval);
          this.checkInterval = null;
          this.restartFailed();
          resolve();
        }
      }, checkInterval * 1000);
    });
  }

  /**
   * Handle successful restart
   */
  restartComplete() {
    // Hide modal and remove blur
    this.modal.hide();
    document.body.classList.remove('restart-blur');

    // Re-enable restart button
    const restartBtn = document.getElementById('restart-btn');
    restartBtn.disabled = false;
    this.restartInProgress = false;

    // Show success notification
    showNotification('Camera restarted successfully', 'success', 4000);
  }

  /**
   * Handle failed restart
   */
  restartFailed() {
    // Hide modal and remove blur
    this.modal.hide();
    document.body.classList.remove('restart-blur');

    // Re-enable restart button
    const restartBtn = document.getElementById('restart-btn');
    restartBtn.disabled = false;
    this.restartInProgress = false;

    // Show error notification
    showNotification(
      'Camera did not respond within 120 seconds',
      'error',
      5000
    );
  }

  /**
   * Helper to delay execution
   */
  delay(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
  }

  /**
   * Cleanup resources
   */
  destroy() {
    if (this.checkInterval) {
      clearInterval(this.checkInterval);
      this.checkInterval = null;
    }
  }
}

// Export singleton instance
export const cameraRestartManager = new CameraRestartManager();

// Auto-initialize on DOM ready
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', () => cameraRestartManager.init());
} else {
  cameraRestartManager.init();
}

// Cleanup on page unload
window.addEventListener('beforeunload', () => cameraRestartManager.destroy());