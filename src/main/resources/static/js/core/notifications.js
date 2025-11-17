/**
 * Notification system for user feedback
 */

const NOTIFICATION_TYPES = {
  success: { icon: 'check-circle', class: 'alert-success' },
  error: { icon: 'x-circle', class: 'alert-danger' },
  warning: { icon: 'exclamation-triangle', class: 'alert-warning' },
  info: { icon: 'info-circle', class: 'alert-info' },
};

/**
 * Show notification toast
 */
export function showNotification(message, type = 'info', duration = 4000) {
  const config = NOTIFICATION_TYPES[type] || NOTIFICATION_TYPES.info;

  const alertDiv = document.createElement('div');
  alertDiv.className = `alert ${config.class} text-center mt-3 notification-toast`;
  alertDiv.innerHTML = `
    <i class="bi bi-${config.icon} me-2"></i> ${message}
  `;

  // Find main panel or body
  const container = document.querySelector('.main-panel') || document.body;
  container.prepend(alertDiv);

  // Auto remove
  if (duration > 0) {
    setTimeout(() => alertDiv.remove(), duration);
  }

  return alertDiv;
}

/**
 * Show confirmation dialog
 */
export function confirm(message, title = 'Confirm') {
  return window.confirm(`${title}\n\n${message}`);
}

/**
 * Show loading state
 */
export function showLoading(element, text = 'Loading...') {
  const spinner = document.createElement('div');
  spinner.className = 'spinner-border spinner-border-sm me-2';
  spinner.setAttribute('role', 'status');

  element.disabled = true;
  element.dataset.originalContent = element.innerHTML;
  element.innerHTML = '';
  element.appendChild(spinner);
  element.appendChild(document.createTextNode(text));
}

/**
 * Hide loading state
 */
export function hideLoading(element) {
  if (element.dataset.originalContent) {
    element.innerHTML = element.dataset.originalContent;
    delete element.dataset.originalContent;
  }
  element.disabled = false;
}