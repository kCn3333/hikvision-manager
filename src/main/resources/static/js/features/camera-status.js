/**
 * Camera Status Monitor
 * Auto-refreshes camera health information
 */

import { API } from '../core/api.js';
import { http } from '../core/http.js';

class CameraStatusMonitor {
  constructor() {
    this.refreshInterval = null;
    this.elements = {};
  }

  /**
   * Initialize monitor
   */
  init(refreshRate = 5000) {
    // Cache DOM elements
    this.elements = {
      cpuUsage: document.getElementById('cpuUsage'),
      memoryUsage: document.getElementById('memoryUsage'),
      uptime: document.getElementById('uptime'),
      deviceTime: document.getElementById('deviceTime'),
      connectionDot: document.getElementById('connection-dot'),
      statusLabel: null, // Will be found dynamically
    };

    // Find status label
    if (this.elements.connectionDot) {
      const header = this.elements.connectionDot.closest('.card-header');
      if (header) {
        const activity = header.querySelector('span i.bi-activity');
        if (activity) {
          this.elements.statusLabel = activity.parentNode;
        }
      }
    }

    // Initial update
    this.updateStatus();

    // Start auto-refresh
    this.refreshInterval = setInterval(() => {
      this.updateStatus();
    }, refreshRate);
  }

  /**
   * Update camera status
   */
  async updateStatus() {
    try {
      const data = await http.get(API.camera.status);
      this.renderStatus(data);
    } catch (error) {
      console.error('Failed to update camera status:', error);
      this.renderOffline();
    }
  }

  /**
   * Render online status
   */
  renderStatus(data) {
    // Update metrics
    if (this.elements.cpuUsage) {
      this.elements.cpuUsage.textContent = data.cpuUsage || '—';
    }

    if (this.elements.memoryUsage) {
      this.elements.memoryUsage.textContent = data.memoryUsage || '—';
    }

    if (this.elements.uptime) {
      this.elements.uptime.textContent =
        data.formattedUptime || `${data.uptimeMinutes || 0} min`;
    }

    if (this.elements.deviceTime) {
      this.elements.deviceTime.textContent = data.currentDeviceTime || '—';
    }

    // Update connection indicator
    if (data.online) {
      this.setConnected();
    } else {
      this.setDisconnected();
    }
  }

  /**
   * Render offline status
   */
  renderOffline() {
    if (this.elements.cpuUsage) this.elements.cpuUsage.textContent = '—';
    if (this.elements.memoryUsage) this.elements.memoryUsage.textContent = '—';
    if (this.elements.uptime) this.elements.uptime.textContent = '—';
    if (this.elements.deviceTime) this.elements.deviceTime.textContent = '—';

    this.setDisconnected();
  }

  /**
   * Set connected state
   */
  setConnected() {
    if (this.elements.connectionDot) {
      this.elements.connectionDot.classList.add('connected');
      this.elements.connectionDot.classList.remove('disconnected');
    }

    if (this.elements.statusLabel) {
      this.elements.statusLabel.innerHTML =
        '<i class="bi bi-activity"></i> Connected';
    }
  }

  /**
   * Set disconnected state
   */
  setDisconnected() {
    if (this.elements.connectionDot) {
      this.elements.connectionDot.classList.remove('connected');
      this.elements.connectionDot.classList.add('disconnected');
    }

    if (this.elements.statusLabel) {
      this.elements.statusLabel.innerHTML =
        '<i class="bi bi-activity text-secondary"></i> Disconnected';
    }
  }

  /**
   * Stop monitoring
   */
  destroy() {
    if (this.refreshInterval) {
      clearInterval(this.refreshInterval);
      this.refreshInterval = null;
    }
  }
}

// Export singleton instance
export const cameraStatus = new CameraStatusMonitor();

// Auto-initialize on DOM ready
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', () => cameraStatus.init());
} else {
  cameraStatus.init();
}

// Cleanup on page unload
window.addEventListener('beforeunload', () => cameraStatus.destroy());