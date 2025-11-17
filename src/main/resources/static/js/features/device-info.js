/**
 * Device Information Display
 */

import { API } from '../core/api.js';
import { http } from '../core/http.js';

class DeviceInfoManager {
  constructor() {
    this.elements = {};
  }

  /**
   * Initialize device info manager
   */
  init() {
    // Load all info sections
    this.loadAllInfo();
  }

  /**
   * Load all device information
   */
  async loadAllInfo() {
    await Promise.all([
      this.loadDeviceInfo(),
      this.loadNetworkInfo(),
      this.loadTimeInfo(),
      this.loadStorageInfo(),
    ]);
  }

  /**
   * Load device information
   */
  async loadDeviceInfo() {
    try {
      const data = await http.get(API.camera.info);
      this.renderDeviceInfo(data);
    } catch (error) {
      console.error('Failed to load device info:', error);
    }
  }

  /**
   * Render device information
   */
  renderDeviceInfo(data) {
    this.setElementText('deviceName', data.deviceName);
    this.setElementText('deviceDescription', data.deviceDescription);
    this.setElementText('manufacturer', data.manufacturer);
    this.setElementText('model', data.model);
    this.setElementText('serialNumber', data.serialNumber);
    this.setElementText('firmwareVersion', data.firmwareVersion);
    this.setElementText('firmwareReleasedDate', data.firmwareReleasedDate);
    this.setElementText('deviceLocation', data.deviceLocation);
  }

  /**
   * Load network information
   */
  async loadNetworkInfo() {
    try {
      const data = await http.get(API.camera.network);
      this.renderNetworkInfo(data);
    } catch (error) {
      console.error('Failed to load network info:', error);
    }
  }

  /**
   * Render network information
   */
  renderNetworkInfo(data) {
    this.setElementText('ipAddress', data.ipAddress);
    this.setElementText('macAddress', data.macAddress);
    this.setElementText('subnetMask', data.subnetMask);
    this.setElementText('defaultGateway', data.defaultGateway);
    this.setElementText('dnsServer', data.dnsServer);
    this.setElementText('speed', data.speed);
    this.setElementText('mtu', data.mtu);
    this.setElementText('duplex', data.duplex);
  }

  /**
   * Load time information
   */
  async loadTimeInfo() {
    try {
      const data = await http.get(API.camera.time);
      this.renderTimeInfo(data);
    } catch (error) {
      console.error('Failed to load time info:', error);
    }
  }

  /**
   * Render time information
   */
  renderTimeInfo(data) {
    this.setElementText('timeMode', data.timeMode);
    this.setElementText('formattedLocalTime', data.formattedLocalTime);
    this.setElementText('formattedTimeZone', data.formattedTimeZone);
    this.setElementText('ntpEnabled', data.ntpEnabled ? 'Yes' : 'No');
    this.setElementText('statusMessage', data.statusMessage);
  }

  /**
   * Load storage information
   */
  async loadStorageInfo() {
    try {
      const data = await http.get(API.camera.storage);
      this.renderStorageInfo(data);
    } catch (error) {
      console.error('Failed to load storage info:', error);
    }
  }

  /**
   * Render storage information
   */
  renderStorageInfo(data) {
    // Clean capacity (remove parentheses content)
    const capacityRaw = data.capacity || '';
    const capacityClean = capacityRaw.replace(/\s*\([^)]*\)/, '').trim();

    this.setElementText('storage-format', data.formatType);
    this.setElementText('storage-type', data.type);
    this.setElementText('storage-mounts', data.mountTypes?.join(', '));
    this.setElementText('storage-capacity', capacityClean);

    // Parse usage percentage
    const percent = parseFloat(data.usage?.replace(',', '.')) || 0;

    // Update progress bar
    const bar = document.getElementById('storage-progress-bar');
    if (bar) {
      bar.style.width = `${percent}%`;
    }

    // Update usage text
    const usage = document.getElementById('storage-usage');
    if (usage) {
      usage.textContent = percent.toFixed(1) + '%';
    }

    // Update status indicator
    const dot = document.getElementById('storage-status-indicator');
    if (dot) {
      dot.classList.toggle('connected', data.status === 'ok');
    }
  }

  /**
   * Helper to set element text content
   */
  setElementText(id, value) {
    const element = document.getElementById(id);
    if (element) {
      element.textContent = value || '—';
    }
  }
}

// Export singleton instance
export const deviceInfoManager = new DeviceInfoManager();

// Auto-initialize on DOM ready
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', () => deviceInfoManager.init());
} else {
  deviceInfoManager.init();
}