/**
 * Universal Progress Panel
 * Handles both batch progress and individual file download progress
 * Persists state across page navigation using sessionStorage
 */

import { API, POLLING_CONFIG } from '../core/api.js';
import { http } from '../core/http.js';
import { formatDownloadSpeed } from '../utils/file-size.js';
import { formatDuration } from '../utils/date-formatter.js';

class ProgressPanel {
  constructor() {
    this.container = null;
    this.elements = {};
    this.pollInterval = null;
    this.currentBatchId = null;
    this.startTime = null;
  }

  /**
   * Initialize panel (call once on page load)
   */
  init() {
    this.container = document.getElementById('progress-container');
    if (!this.container) {
      console.warn('ProgressPanel: container not found');
      return;
    }

    this.elements = {
      // Batch progress elements
      batchText: document.getElementById('progress-batch-text'),
      batchBar: document.getElementById('progress-batch-bar'),
      batchPercent: document.getElementById('progress-batch-percent'),

      // File progress elements
      fileText: document.getElementById('progress-file-text'),
      fileBar: document.getElementById('progress-file-bar'),
      filePercent: document.getElementById('progress-file-percent'),
      fileSpeed: document.getElementById('progress-file-speed'),
      fileEta: document.getElementById('progress-file-eta'),

      // Details and controls
      details: document.getElementById('progress-details'),
      jobsList: document.getElementById('progress-jobs-list'),
      closeBtn: document.getElementById('progress-close'),
    };

    this.elements.closeBtn?.addEventListener('click', () => this.hide());

    // Resume active batch if exists
    this.resumeIfActive();
  }

  /**
   * Resume polling if there's an active batch in session storage
   */
  resumeIfActive() {
    const activeBatchId = sessionStorage.getItem('activeBatchId');
    const startTime = sessionStorage.getItem('batchStartTime');

    if (activeBatchId && startTime) {
      console.debug('Resuming progress panel for batch:', activeBatchId);
      this.currentBatchId = activeBatchId;
      this.startTime = parseInt(startTime);
      this.show();
      this.poll(); // First poll immediately
      this.pollInterval = setInterval(() => this.poll(), POLLING_CONFIG.interval);
    }
  }

  /**
   * Start monitoring batch progress
   */
  start(batchId) {
    if (!this.container) {
      console.error('ProgressPanel not initialized');
      return;
    }

    this.currentBatchId = batchId;
    this.startTime = Date.now();

    // Persist to sessionStorage for cross-page navigation
    sessionStorage.setItem('activeBatchId', batchId);
    sessionStorage.setItem('batchStartTime', this.startTime.toString());

    this.show();
    this.poll();

    // Poll every 1 second
    this.pollInterval = setInterval(() => this.poll(), POLLING_CONFIG.interval);
  }

  /**
   * Show panel
   */
  show() {
    this.container.classList.add('active');
    this.resetUI();
  }

  /**
   * Hide panel
   */
  hide() {
    this.container.classList.remove('active');
    this.cleanup();

    // Clear session storage
    sessionStorage.removeItem('activeBatchId');
    sessionStorage.removeItem('batchStartTime');
  }

  /**
   * Reset UI to initial state
   */
  resetUI() {
    // Reset batch progress
    if (this.elements.batchBar) {
      this.elements.batchBar.style.width = '0%';
      this.elements.batchBar.classList.remove('bg-success', 'bg-danger', 'bg-warning');
      this.elements.batchBar.classList.add('bg-primary');
    }

    if (this.elements.batchText) {
      this.elements.batchText.textContent = 'Initializing...';
    }

    if (this.elements.batchPercent) {
      this.elements.batchPercent.textContent = '0%';
    }

    // Reset file progress
    if (this.elements.fileBar) {
      this.elements.fileBar.style.width = '0%';
    }

    if (this.elements.fileText) {
      this.elements.fileText.textContent = 'Waiting...';
    }

    if (this.elements.filePercent) {
      this.elements.filePercent.textContent = '0%';
    }

    // Hide details initially
    if (this.elements.details) {
      this.elements.details.innerHTML = '';
    }

    if (this.elements.jobsList) {
      this.elements.jobsList.innerHTML = '';
    }

    // Hide close button
    if (this.elements.closeBtn) {
      this.elements.closeBtn.style.display = 'none';
    }
  }

  /**
   * Poll batch status
   */
  async poll() {
    if (!this.currentBatchId) return;

    try {
      const data = await http.get(API.batch.status(this.currentBatchId));
      this.updateUI(data);

      // Stop polling if completed, failed, or partially failed
      if (data.status === 'COMPLETED' ||
          data.status === 'FAILED' ||
          data.status === 'PARTIAL_FAILURE') {
        this.cleanup();
        this.showCompletion(data);
      }
    } catch (error) {
      console.error('Progress poll error:', error);

      // If batch not found, it might have been cleaned up - stop polling
      if (error.status === 404) {
        console.warn('Batch not found, stopping polling');
        this.cleanup();
        this.hide();
      }
      // Continue polling on other errors (might be temporary network issue)
    }
  }

  /**
   * Update UI with batch status
   */
  updateUI(data) {
    // Update batch progress
    this.updateBatchProgress(data);

    // Update file progress (if any file is downloading)
    this.updateFileProgress(data);

    // Update details
    this.updateDetails(data);
  }

  /**
   * Update batch progress bar
   */
  updateBatchProgress(data) {
    const percent = data.total > 0
      ? Math.round((data.completed / data.total) * 100)
      : 0;

    if (this.elements.batchBar) {
      this.elements.batchBar.style.width = `${percent}%`;
      this.elements.batchBar.setAttribute('aria-valuenow', percent);
    }

    if (this.elements.batchPercent) {
      this.elements.batchPercent.textContent = `${percent}%`;
    }

    if (this.elements.batchText) {
      this.elements.batchText.textContent = data.message ||
        `Processing ${data.completed}/${data.total} files`;
    }
  }

  /**
   * Update individual file progress
   */
  updateFileProgress(data) {
    // Find currently downloading job
    const downloadingJob = data.jobs?.find(j => j.status === 'DOWNLOADING');

    if (downloadingJob) {
      const percent = downloadingJob.progressPercent || 0;

      if (this.elements.fileBar) {
        this.elements.fileBar.style.width = `${percent}%`;
        this.elements.fileBar.setAttribute('aria-valuenow', percent);
      }

      if (this.elements.filePercent) {
        this.elements.filePercent.textContent = `${percent}%`;
      }

      if (this.elements.fileText) {
        const fileName = downloadingJob.fileName || 'Unknown file';
        this.elements.fileText.textContent = `Downloading: ${fileName}`;
      }

      if (this.elements.fileSpeed && downloadingJob.downloadSpeed) {
        this.elements.fileSpeed.textContent = formatDownloadSpeed(downloadingJob.downloadSpeed);
      }

      if (this.elements.fileEta && downloadingJob.eta) {
        this.elements.fileEta.textContent = downloadingJob.eta;
      }

      // Show size info
      if (this.elements.details && downloadingJob.downloadedSize && downloadingJob.totalSize) {
        const sizeInfo = `${downloadingJob.downloadedSize} / ${downloadingJob.totalSize}`;
        this.elements.details.innerHTML = `<small class="text-muted">${sizeInfo}</small>`;
      }
    } else {
      // No active download - reset file progress
      if (this.elements.fileBar) {
        this.elements.fileBar.style.width = '0%';
      }
      if (this.elements.fileText) {
        this.elements.fileText.textContent = data.inProgress > 0
          ? 'Preparing download...'
          : 'Waiting for next file...';
      }
      if (this.elements.fileSpeed) {
        this.elements.fileSpeed.textContent = '—';
      }
      if (this.elements.fileEta) {
        this.elements.fileEta.textContent = '—';
      }
    }
  }

  /**
   * Update summary details
   */
  updateDetails(data) {
    if (!this.elements.details) return;

    const elapsed = Math.round((Date.now() - this.startTime) / 1000);
    const minutes = Math.floor(elapsed / 60);
    const seconds = elapsed % 60;
    const timeStr = `${minutes}:${seconds.toString().padStart(2, '0')}`;

    const detailsHTML = `
      <div class="d-flex justify-content-between align-items-center text-sm">
        <div>
          <span class="text-success">✓ ${data.completed}</span>
          <span class="text-info ms-2">⟳ ${data.inProgress}</span>
          <span class="text-muted ms-2">⏳ ${data.queued}</span>
          ${data.failed > 0 ? `<span class="text-danger ms-2">✗ ${data.failed}</span>` : ''}
        </div>
        <div class="text-muted">
          <i class="bi bi-clock me-1"></i>${timeStr}
        </div>
      </div>
    `;

    this.elements.details.innerHTML = detailsHTML;
  }

  /**
   * Show completion state
   */
  showCompletion(data) {
    const isSuccess = data.status === 'COMPLETED' && data.failed === 0;
    const isPartial = data.status === 'PARTIAL_FAILURE';

    // Update batch progress bar color
    if (this.elements.batchBar) {
      this.elements.batchBar.classList.remove('bg-primary');
      this.elements.batchBar.classList.add(
        isSuccess ? 'bg-success' :
        isPartial ? 'bg-warning' :
        'bg-danger'
      );
      this.elements.batchBar.style.width = '100%';
    }

    // Update message
    if (this.elements.batchText) {
      this.elements.batchText.textContent =
        isSuccess ? '✅ All files downloaded successfully' :
        isPartial ? `⚠️ Completed with ${data.failed} failed (${data.completed}/${data.total} successful)` :
        `❌ Completed with ${data.failed} failed`;
    }

    // Hide file progress
    if (this.elements.fileBar) {
      this.elements.fileBar.style.width = '0%';
    }
    if (this.elements.fileText) {
      this.elements.fileText.textContent = '';
    }

    // Show completed jobs list
    this.showJobsList(data.jobs);

    // Show close button
    if (this.elements.closeBtn) {
      this.elements.closeBtn.style.display = 'block';
    }

    // Clear session storage as batch is complete
    sessionStorage.removeItem('activeBatchId');
    sessionStorage.removeItem('batchStartTime');
  }

  /**
   * Display list of completed jobs
   */
  showJobsList(jobs) {
    if (!this.elements.jobsList || !jobs) return;

    const jobsHTML = jobs
      .map(job => {
        const icon = job.status === 'COMPLETED'
          ? '<i class="bi bi-file-earmark-check text-success"></i>'
          : job.status === 'FAILED'
          ? '<i class="bi bi-file-earmark-x text-danger"></i>'
          : '<i class="bi bi-file-earmark text-muted"></i>';

        const link = job.downloadUrl
          ? `<a href="${job.downloadUrl}" class="text-light text-decoration-none ms-2" download>
               ${job.fileName}
             </a>
             <small class="text-muted ms-1">(${job.actualFileSize})</small>`
          : `<span class="text-muted ms-2">${job.message || 'No file available'}</span>`;

        return `<div class="py-1">${icon}${link}</div>`;
      })
      .join('');

    this.elements.jobsList.innerHTML = jobsHTML;
  }

  /**
   * Cleanup resources
   */
  cleanup() {
    if (this.pollInterval) {
      clearInterval(this.pollInterval);
      this.pollInterval = null;
    }
    this.currentBatchId = null;
    this.startTime = null;
  }
}

// Export singleton instance
export const progressPanel = new ProgressPanel();

// Auto-initialize on DOM ready
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', () => progressPanel.init());
} else {
  progressPanel.init();
}