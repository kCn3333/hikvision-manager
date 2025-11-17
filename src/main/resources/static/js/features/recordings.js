/**
 * Recordings Search and Download Management
 */

import { API } from '../core/api.js';
import { http, apiCall } from '../core/http.js';
import { showNotification } from '../core/notifications.js';
import { progressPanel } from '../components/progress-panel.js';

class RecordingsManager {
  constructor() {
    this.searchResults = [];
    this.elements = {};
    this.currentPage = 0;
    this.pageSize = 50;
    this.hasMore = false;
    this.currentSearch = null;
  }

  /**
   * Initialize recordings manager
   */
  init() {
    // Cache DOM elements
    this.elements = {
      searchBtn: document.getElementById('rec-search-btn'),
      startInput: document.getElementById('rec-start'),
      endInput: document.getElementById('rec-end'),
      tableBody: document.getElementById('recordings-table-body'),
      bulkDownloadBtn: document.getElementById('bulk-download'),
      selectAllCheckbox: document.getElementById('select-all'),
      feedback: document.getElementById('rec-search-feedback'),
      paginationInfo: document.getElementById('pagination-info'),
      paginationControls: document.getElementById('pagination-controls'),
    };

    // Setup event listeners
    this.setupEventListeners();

    // Load storage info
    this.loadStorageInfo();
  }

  /**
   * Setup event listeners
   */
  setupEventListeners() {
    // Search button
    this.elements.searchBtn?.addEventListener('click', () => {
      this.searchRecordings();
    });

    // Bulk download button
    this.elements.bulkDownloadBtn?.addEventListener('click', () => {
      this.bulkDownload();
    });

    // Select all checkbox
    this.elements.selectAllCheckbox?.addEventListener('change', (e) => {
      const checkboxes = document.querySelectorAll('.rec-select');
      checkboxes.forEach(cb => cb.checked = e.target.checked);
    });

    // Enter key in date inputs
    [this.elements.startInput, this.elements.endInput].forEach(input => {
      input?.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') {
          this.searchRecordings();
        }
      });
    });
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

    // Update elements
    const elements = {
      format: document.getElementById('storage-format'),
      type: document.getElementById('storage-type'),
      mounts: document.getElementById('storage-mounts'),
      capacity: document.getElementById('storage-capacity'),
      bar: document.getElementById('storage-progress-bar'),
      usage: document.getElementById('storage-usage'),
      dot: document.getElementById('storage-status-indicator'),
    };

    if (elements.format) elements.format.textContent = data.formatType || '—';
    if (elements.type) elements.type.textContent = data.type || '—';
    if (elements.mounts) elements.mounts.textContent = data.mountTypes?.join(', ') || '—';
    if (elements.capacity) elements.capacity.textContent = capacityClean;

    // Parse usage percentage
    const percent = parseFloat(data.usage?.replace(',', '.')) || 0;

    if (elements.bar) {
      elements.bar.style.width = `${percent}%`;

      // Color code based on usage
      elements.bar.classList.remove('bg-success', 'bg-warning', 'bg-danger');
      if (percent < 70) {
        elements.bar.classList.add('bg-success');
      } else if (percent < 90) {
        elements.bar.classList.add('bg-warning');
      } else {
        elements.bar.classList.add('bg-danger');
      }
    }

    if (elements.usage) {
      elements.usage.textContent = percent.toFixed(1) + '%';
    }

    if (elements.dot) {
      elements.dot.classList.toggle('connected', data.status === 'ok');
    }
  }

  /**
   * Search for recordings
   */
  async searchRecordings(page = 0) {
    const startTime = this.elements.startInput?.value;
    const endTime = this.elements.endInput?.value;

    // Clear previous feedback
    if (this.elements.feedback) {
      this.elements.feedback.textContent = '';
    }

    // Validate inputs
    if (!startTime || !endTime) {
      if (this.elements.feedback) {
        this.elements.feedback.textContent = 'Please select both start and end time';
      }
      showNotification('Please select both start and end time', 'warning');
      return;
    }

    if (new Date(startTime) >= new Date(endTime)) {
      if (this.elements.feedback) {
        this.elements.feedback.textContent = 'End time must be after start time';
      }
      showNotification('End time must be after start time', 'warning');
      return;
    }

    // Store current search params
    this.currentSearch = { startTime, endTime };
    this.currentPage = page;

    try {
      // Disable search button
      this.elements.searchBtn.disabled = true;
      this.elements.searchBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>Searching...';

      const data = await apiCall(
        http.post(API.recordings.search, {
          startTime,
          endTime,
          page,
          size: this.pageSize
        }),
        'Failed to search recordings'
      );

      this.searchResults = data.recordings || [];
      this.hasMore = data.hasMore || false;

      this.renderRecordingsTable();
      this.renderPagination();
    } catch (error) {
      // Error already handled by apiCall
    } finally {
      // Re-enable search button
      this.elements.searchBtn.disabled = false;
      this.elements.searchBtn.innerHTML = '<i class="bi bi-search"></i> Search';
    }
  }

  /**
   * Render recordings table
   */
  renderRecordingsTable() {
    if (!this.elements.tableBody) return;

    this.elements.tableBody.innerHTML = '';

    if (this.searchResults.length === 0) {
      this.elements.tableBody.innerHTML = `
        <tr>
          <td colspan="7" class="text-center text-secondary py-3">
            No recordings found. Choose date range and click "Search".
          </td>
        </tr>
      `;
      return;
    }

    this.searchResults.forEach(rec => {
      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td class="text-center">
          <input type="checkbox"
                 class="rec-select form-check-input"
                 data-id="${rec.recordingId}">
        </td>
        <td>${this.escapeHtml(rec.startTime)}</td>
        <td>${this.escapeHtml(rec.endTime)}</td>
        <td>${this.escapeHtml(rec.duration)}</td>
        <td><span class="badge bg-secondary">${this.escapeHtml(rec.codec)}</span></td>
        <td>${this.escapeHtml(rec.fileSize)}</td>
        <td class="text-center">
          <button class="btn btn-outline-success btn-sm download-btn"
                  data-id="${rec.recordingId}"
                  title="Download this recording">
            <i class="bi bi-download"></i>
          </button>
        </td>
      `;
      this.elements.tableBody.appendChild(tr);
    });

    // Attach download button listeners
    document.querySelectorAll('.download-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        const id = btn.dataset.id;
        this.downloadSingle(id);
      });
    });

    // Reset select all checkbox
    if (this.elements.selectAllCheckbox) {
      this.elements.selectAllCheckbox.checked = false;
    }
  }

  /**
   * Download single recording
   */
  async downloadSingle(recordingId) {
    const recording = this.searchResults.find(r => r.recordingId === recordingId);
    if (!recording) {
      showNotification('Recording not found', 'error');
      return;
    }

    try {
      // Call single download endpoint
      const response = await apiCall(
        http.post(API.recordings.downloadStart, recording),
        'Failed to start download'
      );

      const batchId = response.batchId;

      // Start progress monitoring
      progressPanel.start(batchId);
    } catch (error) {
      // Error already handled by apiCall
    }
  }

  /**
   * Bulk download selected recordings
   */
  async bulkDownload() {
    // Get selected checkbox IDs
    const selected = Array.from(document.querySelectorAll('.rec-select:checked'))
      .map(cb => cb.dataset.id);

    if (selected.length === 0) {
      showNotification('Please select at least one recording', 'warning');
      return;
    }

    // Filter recordings
    const recordings = this.searchResults.filter(r =>
      selected.includes(r.recordingId)
    );

    await this.startBatchDownload(recordings);
  }

  /**
   * Start batch download
   */
  async startBatchDownload(recordings) {
    try {
      // Call batch download endpoint
      const response = await apiCall(
        http.post(API.recordings.downloadStartBatch, recordings),
        'Failed to start download'
      );

      const batchId = response.batchId;

      // Start progress monitoring
      progressPanel.start(batchId);
    } catch (error) {
      // Error already handled by apiCall
    }
  }

  /**
   * Render pagination controls
   */
  renderPagination() {
    if (!this.elements.paginationInfo || !this.elements.paginationControls) return;

    // Update info text
    const pageNum = this.currentPage + 1;
    const countInfo = this.hasMore ? `${this.searchResults.length}+` : this.searchResults.length;
    this.elements.paginationInfo.textContent =
      `Page ${pageNum} | Showing ${countInfo} recordings`;

    // Build pagination controls
    const controls = [];

    // Previous button
    if (this.currentPage > 0) {
      controls.push(`
        <li class="page-item">
          <button class="page-link" data-page="${this.currentPage - 1}">
            <i class="bi bi-chevron-left"></i> Previous
          </button>
        </li>
      `);
    } else {
      controls.push(`
        <li class="page-item disabled">
          <span class="page-link"><i class="bi bi-chevron-left"></i> Previous</span>
        </li>
      `);
    }

    // Current page indicator
    controls.push(`
      <li class="page-item active">
        <span class="page-link">${pageNum}</span>
      </li>
    `);

    // Next button
    if (this.hasMore) {
      controls.push(`
        <li class="page-item">
          <button class="page-link" data-page="${this.currentPage + 1}">
            Next <i class="bi bi-chevron-right"></i>
          </button>
        </li>
      `);
    } else {
      controls.push(`
        <li class="page-item disabled">
          <span class="page-link">Next <i class="bi bi-chevron-right"></i></span>
        </li>
      `);
    }

    this.elements.paginationControls.innerHTML = controls.join('');

    // Attach event listeners
    this.elements.paginationControls.querySelectorAll('button[data-page]').forEach(btn => {
      btn.addEventListener('click', () => {
        const page = parseInt(btn.dataset.page);
        this.searchRecordings(page);
      });
    });
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
export const recordingsManager = new RecordingsManager();

// Auto-initialize on DOM ready
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', () => recordingsManager.init());
} else {
  recordingsManager.init();
}