/**
 * Status Indicator Component
 * Manages status dots (connected/disconnected)
 */

export class StatusIndicator {
  /**
   * Create a status indicator
   * @param {string} elementId - ID of the status dot element
   */
  constructor(elementId) {
    this.element = document.getElementById(elementId);

    if (!this.element) {
      console.warn(`StatusIndicator: element '${elementId}' not found`);
    }
  }

  /**
   * Set connected state
   */
  setConnected() {
    if (!this.element) return;

    this.element.classList.remove('disconnected', 'warning');
    this.element.classList.add('connected');
    this.element.style.backgroundColor = '';
    this.element.style.boxShadow = '';
  }

  /**
   * Set disconnected state
   */
  setDisconnected() {
    if (!this.element) return;

    this.element.classList.remove('connected', 'warning');
    this.element.classList.add('disconnected');
    this.element.style.backgroundColor = '';
    this.element.style.boxShadow = '';
  }

  /**
   * Set warning state (e.g., connecting)
   */
  setWarning() {
    if (!this.element) return;

    this.element.classList.remove('connected', 'disconnected');
    this.element.classList.add('warning');
    this.element.style.backgroundColor = '#ffc107';
    this.element.style.boxShadow = '0 0 10px #ffc107';
  }

  /**
   * Set custom state
   * @param {string} state - State name ('connected', 'disconnected', 'warning')
   */
  setState(state) {
    switch (state) {
      case 'connected':
        this.setConnected();
        break;
      case 'disconnected':
        this.setDisconnected();
        break;
      case 'warning':
      case 'connecting':
        this.setWarning();
        break;
      default:
        this.setDisconnected();
    }
  }

  /**
   * Toggle between connected and disconnected
   * @param {boolean} isConnected - Connection state
   */
  toggle(isConnected) {
    if (isConnected) {
      this.setConnected();
    } else {
      this.setDisconnected();
    }
  }

  /**
   * Check if element exists
   * @returns {boolean}
   */
  exists() {
    return !!this.element;
  }
}

/**
 * Create a status indicator instance
 * @param {string} elementId - ID of the status dot element
 * @returns {StatusIndicator}
 */
export function createStatusIndicator(elementId) {
  return new StatusIndicator(elementId);
}