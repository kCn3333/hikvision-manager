/**
 * Backup Configurations Management
 */

import { API } from '../core/api.js';
import { http, apiCall } from '../core/http.js';
import { showNotification, confirm } from '../core/notifications.js';
import { progressPanel } from '../components/progress-panel.js';

class BackupManager {
  constructor() {
    this.modal = null;
    this.form = {};
    this.tableBody = null;
  }

  /**
   * Initialize backup manager
   */
  init() {
    // Get DOM elements
    this.tableBody = document.getElementById('backup-table-body');
    const modalEl = document.getElementById('backupModal');

    if (!modalEl) {
      console.warn('BackupManager: modal not found');
      return;
    }

    // Initialize Bootstrap modal
    this.modal = new bootstrap.Modal(modalEl);

    // Cache form elements
    this.form = {
      id: document.getElementById('backup-id'),
      name: document.getElementById('backup-name'),
      camera: document.getElementById('backup-camera'),
      scheduleType: document.getElementById('backup-scheduleType'),
      time: document.getElementById('backup-time'),
      dayOfWeek: document.getElementById('backup-dayOfWeek'),
      retention: document.getElementById('backup-retention'),
      enabled: document.getElementById('backup-enabled'),
      notify: document.getElementById('backup-notify'),
      dayContainer: document.getElementById('dayOfWeek-container'),
    };

    // Setup event listeners
    this.setupEventListeners();

    // Load configurations
    this.loadConfigs();
  }

  /**
   * Setup event listeners
   */
  setupEventListeners() {
    // Schedule type change - show/hide day of week
    this.form.scheduleType?.addEventListener('change', () => {
      const isWeekly = this.form.scheduleType.value === 'WEEKLY';
      if (this.form.dayContainer) {
        this.form.dayContainer.style.display = isWeekly ? 'block' : 'none';
      }
    });

    // Add button
    document.getElementById('add-backup-btn')?.addEventListener('click', () => {
      this.openModal();
    });

    // Save button
    document.getElementById('save-backup-btn')?.addEventListener('click', () => {
      this.saveConfig();
    });
  }

  /**
   * Load backup configurations
   */
  async loadConfigs() {
    try {
      const configs = await apiCall(
        http.get(API.backups.list),
        'Failed to load backup configurations'
      );
      this.renderTable(configs);
    } catch (error) {
      // Error already handled by apiCall
    }
  }

  /**
   * Render configurations table
   */
  renderTable(configs) {
    if (!this.tableBody) return;

    this.tableBody.innerHTML = '';

    if (!configs || configs.length === 0) {
      this.tableBody.innerHTML = `
        <tr>
          <td colspan="8" class="text-secondary py-3">
            No backup configurations found.
          </td>
        </tr>
      `;
      return;
    }

    configs.forEach(cfg => {
      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td>${this.escapeHtml(cfg.name)}</td>
        <td>${this.escapeHtml(cfg.cameraId)}</td>
        <td><span class="badge bg-secondary">${cfg.scheduleType}</span></td>
        <td>${cfg.time || '—'}</td>
        <td>${cfg.retentionDays} days</td>
        <td>${cfg.notifyOnComplete ? '✅' : '❌'}</td>
        <td>${cfg.enabled ? '🟢' : '🔴'}</td>
        <td>
          <div class="btn-group btn-group-sm" role="group">
            <button class="btn btn-outline-success"
                    data-action="run"
                    data-id="${cfg.id}"
                    title="Run backup now">
              <i class="bi bi-play-fill"></i>
            </button>
            <button class="btn btn-outline-warning"
                    data-action="edit"
                    data-id="${cfg.id}"
                    title="Edit configuration">
              <i class="bi bi-pencil"></i>
            </button>
            <button class="btn btn-outline-danger"
                    data-action="delete"
                    data-id="${cfg.id}"
                    title="Delete configuration">
              <i class="bi bi-trash"></i>
            </button>
          </div>
        </td>
      `;
      this.tableBody.appendChild(tr);
    });

    // Attach event listeners to action buttons
    this.tableBody.querySelectorAll('button[data-action]').forEach(btn => {
      btn.addEventListener('click', () => {
        const action = btn.dataset.action;
        const id = btn.dataset.id;
        this.handleAction(action, id);
      });
    });
  }

  /**
   * Handle table actions (run, edit, delete)
   */
  async handleAction(action, id) {
    switch (action) {
      case 'run':
        await this.runBackup(id);
        break;
      case 'edit':
        await this.editConfig(id);
        break;
      case 'delete':
        await this.deleteConfig(id);
        break;
    }
  }

  /**
   * Run backup manually
   */
  async runBackup(id) {
    try {
      const response = await apiCall(
        http.action(API.backups.execute(id)),
        'Failed to start backup'
      );

      const batchId = response.batchId;
      showNotification('Backup started successfully', 'success');

      // Start progress monitoring
      progressPanel.start(batchId);
    } catch (error) {
      // Error already handled by apiCall
    }
  }

  /**
   * Edit configuration
   */
  async editConfig(id) {
    try {
      const config = await apiCall(
        http.get(API.backups.get(id)),
        'Failed to load configuration'
      );
      this.openModal(config);
    } catch (error) {
      // Error already handled by apiCall
    }
  }

  /**
   * Delete configuration
   */
  async deleteConfig(id) {
    if (!confirm('Are you sure you want to delete this backup configuration?')) {
      return;
    }

    try {
      await apiCall(
        http.delete(API.backups.delete(id)),
        'Failed to delete configuration'
      );
      showNotification('Configuration deleted successfully', 'success');
      this.loadConfigs();
    } catch (error) {
      // Error already handled by apiCall
    }
  }

  /**
   * Open modal for add/edit
   */
  openModal(config = null) {
    const modalTitle = document.getElementById('backupModalLabel');

    if (config) {
      // Edit mode
      this.form.id.value = config.id;
      this.form.name.value = config.name;
      this.form.camera.value = config.cameraId;
      this.form.scheduleType.value = config.scheduleType;
      this.form.time.value = config.time || '';
      this.form.dayOfWeek.value = config.dayOfWeek || 'MONDAY';
      this.form.retention.value = config.retentionDays;
      this.form.enabled.checked = config.enabled;
      this.form.notify.checked = config.notifyOnComplete;

      const isWeekly = config.scheduleType === 'WEEKLY';
      this.form.dayContainer.style.display = isWeekly ? 'block' : 'none';

      if (modalTitle) {
        modalTitle.textContent = 'Edit Backup Configuration';
      }
    } else {
      // Add mode - set defaults
      this.form.id.value = '';
      this.form.name.value = '';
      this.form.camera.value = 'hikvision-main';
      this.form.scheduleType.value = 'DAILY';
      this.form.time.value = '02:00';
      this.form.dayOfWeek.value = 'MONDAY';
      this.form.retention.value = 7;
      this.form.enabled.checked = true;
      this.form.notify.checked = false;
      this.form.dayContainer.style.display = 'none';

      if (modalTitle) {
        modalTitle.textContent = 'Add Backup Configuration';
      }
    }

    this.modal.show();
  }

  /**
   * Save configuration
   */
  async saveConfig() {
    // Build payload
    const payload = {
      name: this.form.name.value.trim(),
      cameraId: this.form.camera.value.trim(),
      enabled: this.form.enabled.checked,
      scheduleType: this.form.scheduleType.value,
      time: this.form.time.value,
      retentionDays: parseInt(this.form.retention.value) || 0,
      notifyOnComplete: this.form.notify.checked,
      dayOfWeek: this.form.scheduleType.value === 'WEEKLY'
        ? this.form.dayOfWeek.value
        : null,
    };

    // Validate
    if (!payload.name) {
      showNotification('Please enter a backup name', 'warning');
      return;
    }

    if (!payload.cameraId) {
      showNotification('Please enter a camera ID', 'warning');
      return;
    }

    try {
      const id = this.form.id.value;

      if (id) {
        // Update existing
        await apiCall(
          http.put(API.backups.update(id), payload),
          'Failed to update configuration'
        );
        showNotification('Configuration updated successfully', 'success');
      } else {
        // Create new
        await apiCall(
          http.post(API.backups.create, payload),
          'Failed to create configuration'
        );
        showNotification('Configuration created successfully', 'success');
      }

      this.modal.hide();
      this.loadConfigs();
    } catch (error) {
      // Error already handled by apiCall
    }
  }

  /**
   * Escape HTML to prevent XSS
   */
  escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
  }
}

// Export singleton instance
export const backupManager = new BackupManager();

// Auto-initialize on DOM ready
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', () => backupManager.init());
} else {
  backupManager.init();
}