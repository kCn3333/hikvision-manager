/**
 * Backup History and Statistics
 */

import { API } from '../core/api.js';
import { http, apiCall } from '../core/http.js';
import { formatFileSize } from '../utils/file-size.js';
import { formatDate } from '../utils/date-formatter.js';

class BackupHistoryManager {
  constructor() {
    this.jobs = [];
    this.statistics = null;
    this.refreshInterval = null;
    this.currentPage = 0;
    this.pageSize = 10;
    this.totalPages = 0;
    this.totalElements = 0;
  }

  /**
   * Initialize backup history manager
   */
  init() {
    this.loadStatistics();
    this.loadJobs();

    // Auto-refresh every 30 seconds
    // this.startAutoRefresh(30000);

    // Setup pagination listeners
    this.setupPaginationListeners();
  }

  /**
   * Setup pagination event listeners
   */
  setupPaginationListeners() {
    // Page size selector
    const pageSizeSelect = document.getElementById('jobs-page-size');
    if (pageSizeSelect) {
      pageSizeSelect.addEventListener('change', (e) => {
        this.pageSize = parseInt(e.target.value);
        this.currentPage = 0;
        this.loadJobs();
      });
    }
  }

  /**
   * Load backup statistics
   */
  async loadStatistics() {
    try {
      this.statistics = await apiCall(
        http.get(API.backups.statistics),
        'Failed to load backup statistics'
      );
      this.renderStatistics();
    } catch (error) {
      // Error already handled by apiCall
    }
  }

  /**
   * Load backup jobs history
   */
  async loadJobs(page = this.currentPage) {
    try {
      const url = `${API.backups.jobs}?page=${page}&size=${this.pageSize}&sort=startedAt,desc`;
      const data = await apiCall(
        http.get(url),
        'Failed to load backup history'
      );

      // Spring Page response
      this.jobs = data.content || [];
      this.currentPage = data.number || 0;
      this.totalPages = data.totalPages || 0;
      this.totalElements = data.totalElements || 0;

      this.renderJobsTable();
      this.renderPagination();
    } catch (error) {
      // Error already handled by apiCall
    }
  }

  /**
   * Render statistics cards
   */
  renderStatistics() {
    if (!this.statistics) return;

    const stats = this.statistics;

    // Total Backups
    this.setElementText('stat-total-backups', stats.totalBackups);

    // Completed Backups
    this.setElementText('stat-completed-backups', stats.completedBackups);

    // Failed Backups
    this.setElementText('stat-failed-backups', stats.failedBackups);

    // Success Rate
    const successRate = stats.successRate.toFixed(1);
    this.setElementText('stat-success-rate', `${successRate}%`);

    // Color code success rate
    const rateElement = document.getElementById('stat-success-rate');
    if (rateElement) {
      rateElement.classList.remove('text-success', 'text-warning', 'text-danger');
      if (stats.successRate >= 90) {
        rateElement.classList.add('text-success');
      } else if (stats.successRate >= 70) {
        rateElement.classList.add('text-warning');
      } else {
        rateElement.classList.add('text-danger');
      }
    }

    // Total Size
    this.setElementText('stat-total-size', stats.totalSizeFormatted);

    // Average Backup Size
    this.setElementText('stat-average-size', stats.averageBackupSizeFormatted);

    // Total Recordings
    this.setElementText('stat-total-recordings', stats.totalRecordings);

    // Recent Backups (last 30 days)
    this.setElementText('stat-recent-backups', stats.recentBackupsCount);

    // Update progress bars if available
    this.updateStatisticsCharts();
  }

  /**
   * Update statistics charts/progress bars
   */
  updateStatisticsCharts() {
    if (!this.statistics) return;

    // Success rate progress bar
    const successBar = document.getElementById('success-rate-bar');
    if (successBar) {
      const rate = this.statistics.successRate;
      successBar.style.width = `${rate}%`;
      successBar.setAttribute('aria-valuenow', rate);

      // Color based on rate
      successBar.classList.remove('bg-success', 'bg-warning', 'bg-danger');
      if (rate >= 90) {
        successBar.classList.add('bg-success');
      } else if (rate >= 70) {
        successBar.classList.add('bg-warning');
      } else {
        successBar.classList.add('bg-danger');
      }
    }

    // Storage usage (if needed)
    const storageBar = document.getElementById('storage-usage-bar');
    if (storageBar && this.statistics.totalSizeBytes) {
      // Calculate percentage of some limit (e.g., 100GB)
      const limitBytes = 100 * 1024 * 1024 * 1024; // 100GB
      const percent = Math.min(100, (this.statistics.totalSizeBytes / limitBytes) * 100);
      storageBar.style.width = `${percent}%`;
      storageBar.setAttribute('aria-valuenow', percent);
    }
  }

  /**
   * Render jobs table
   */
  renderJobsTable() {
    const tbody = document.getElementById('backup-jobs-table-body');
    if (!tbody) return;

    tbody.innerHTML = '';

    if (!this.jobs || this.jobs.length === 0) {
      tbody.innerHTML = `
        <tr>
          <td colspan="7" class="text-center text-secondary py-3">
            No backup history found.
          </td>
        </tr>
      `;
      return;
    }

    this.jobs.forEach(job => {
      const tr = document.createElement('tr');

      // Status badge
      const statusBadge = this.getStatusBadge(job.status);

      // Duration
      const duration = this.calculateDuration(job.startedAt, job.endTime);

      // Size
      const size = job.totalBytes > 0
        ? formatFileSize(job.totalBytes)
        : '—';

      // Files progress
      const filesProgress = job.completedFiles !== undefined && job.totalFiles > 0
        ? `${job.completedFiles}/${job.totalFiles}`
        : job.totalFiles || '—';

      tr.innerHTML = `
        <td>${this.escapeHtml(job.cameraId)}</td>
        <td>${formatDate(job.startedAt)}</td>
        <td>${duration}</td>
        <td>${filesProgress}</td>
        <td>${size}</td>
        <td>${statusBadge}</td>
        <td>
          ${job.logPath ? `
            <button class="btn btn-sm btn-outline-info view-log-btn"
                    data-path="${this.escapeHtml(job.logPath)}"
                    title="View log">
              <i class="bi bi-file-text"></i>
            </button>
          ` : '—'}
        </td>
      `;

      tbody.appendChild(tr);
    });

    // Attach log button listeners
    tbody.querySelectorAll('.view-log-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        const logPath = btn.dataset.path;
        this.viewLog(logPath);
      });
    });

    // Update job count
    const countText = this.totalElements > 0
      ? `${this.totalElements} total`
      : this.jobs.length;
    this.setElementText('jobs-count', countText);
  }

  /**
   * Render pagination controls
   */
  renderPagination() {
    const paginationInfo = document.getElementById('jobs-pagination-info');
    const paginationControls = document.getElementById('jobs-pagination-controls');

    if (!paginationInfo || !paginationControls) return;

    // Update info text
    const pageNum = this.currentPage + 1;
    const startItem = this.currentPage * this.pageSize + 1;
    const endItem = Math.min(startItem + this.jobs.length - 1, this.totalElements);

    paginationInfo.textContent = this.totalElements > 0
      ? `Showing ${startItem}-${endItem} of ${this.totalElements}`
      : 'No jobs found';

    // Build pagination controls
    const controls = [];

    // First button
    if (this.currentPage > 0) {
      controls.push(`
        <li class="page-item">
          <button class="page-link" data-page="0" title="First page">
            <i class="bi bi-chevron-double-left"></i>
          </button>
        </li>
      `);
    }

    // Previous button
    if (this.currentPage > 0) {
      controls.push(`
        <li class="page-item">
          <button class="page-link" data-page="${this.currentPage - 1}">
            <i class="bi bi-chevron-left"></i>
          </button>
        </li>
      `);
    } else {
      controls.push(`
        <li class="page-item disabled">
          <span class="page-link"><i class="bi bi-chevron-left"></i></span>
        </li>
      `);
    }

    // Page numbers (show current and 2 pages around)
    const startPage = Math.max(0, this.currentPage - 2);
    const endPage = Math.min(this.totalPages - 1, this.currentPage + 2);

    for (let i = startPage; i <= endPage; i++) {
      if (i === this.currentPage) {
        controls.push(`
          <li class="page-item active">
            <span class="page-link">${i + 1}</span>
          </li>
        `);
      } else {
        controls.push(`
          <li class="page-item">
            <button class="page-link" data-page="${i}">${i + 1}</button>
          </li>
        `);
      }
    }

    // Next button
    if (this.currentPage < this.totalPages - 1) {
      controls.push(`
        <li class="page-item">
          <button class="page-link" data-page="${this.currentPage + 1}">
            <i class="bi bi-chevron-right"></i>
          </button>
        </li>
      `);
    } else {
      controls.push(`
        <li class="page-item disabled">
          <span class="page-link"><i class="bi bi-chevron-right"></i></span>
        </li>
      `);
    }

    // Last button
    if (this.currentPage < this.totalPages - 1) {
      controls.push(`
        <li class="page-item">
          <button class="page-link" data-page="${this.totalPages - 1}" title="Last page">
            <i class="bi bi-chevron-double-right"></i>
          </button>
        </li>
      `);
    }

    paginationControls.innerHTML = controls.join('');

    // Attach event listeners
    paginationControls.querySelectorAll('button[data-page]').forEach(btn => {
      btn.addEventListener('click', () => {
        const page = parseInt(btn.dataset.page);
        this.loadJobs(page);
      });
    });
  }

  /**
   * Get status badge HTML
   */
  getStatusBadge(status) {
    const badges = {
      'COMPLETED': '<span class="badge bg-success">Completed</span>',
      'DOWNLOADING': '<span class="badge bg-primary">Running</span>',
      'QUEUED': '<span class="badge bg-secondary">Queued</span>',
      'FAILED': '<span class="badge bg-danger">Failed</span>',
      'CANCELED': '<span class="badge bg-danger">Canceled</span>',
    };
    return badges[status] || `<span class="badge bg-secondary">${status}</span>`;
  }

  /**
   * Calculate duration between start and end time
   */
  calculateDuration(startedAt, endTime) {
    if (!endTime || !startedAt) return '—';

    const start = new Date(startedAt);
    const end = new Date(endTime);
    const diffMs = end - start;

    if (diffMs < 0) return '—';

    const diffSec = Math.floor(diffMs / 1000);
    const hours = Math.floor(diffSec / 3600);
    const minutes = Math.floor((diffSec % 3600) / 60);
    const seconds = diffSec % 60;

    if (hours > 0) {
      return `${hours}h ${minutes}m ${seconds}s`;
    } else if (minutes > 0) {
      return `${minutes}m ${seconds}s`;
    } else {
      return `${seconds}s`;
    }
  }

  /**
   * View log file (opens in modal or new tab)
   */
  viewLog(logPath) {
    // You can implement this to fetch and display log content
    // For now, just show the path
    alert(`Log file location:\n${logPath}`);

    // TODO: Implement actual log viewing
    // Could be:
    // - Fetch log content via API and show in modal
    // - Open log file in new tab
    // - Download log file
  }

  /**
   * Start auto-refresh
   */
  startAutoRefresh(intervalMs = 30000) {
    this.refreshInterval = setInterval(() => {
      this.loadStatistics();
      this.loadJobs();
    }, intervalMs);
  }

  /**
   * Stop auto-refresh
   */
  stopAutoRefresh() {
    if (this.refreshInterval) {
      clearInterval(this.refreshInterval);
      this.refreshInterval = null;
    }
  }

  /**
   * Manual refresh
   */
  async refresh() {
    await Promise.all([
      this.loadStatistics(),
      this.loadJobs()
    ]);
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

  /**
   * Escape HTML to prevent XSS
   */
  escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
  }

  /**
   * Cleanup resources
   */
  destroy() {
    this.stopAutoRefresh();
  }
}

// Export singleton instance
export const backupHistoryManager = new BackupHistoryManager();

// Auto-initialize on DOM ready
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', () => backupHistoryManager.init());
} else {
  backupHistoryManager.init();
}

// Cleanup on page unload
window.addEventListener('beforeunload', () => backupHistoryManager.destroy());