/**
 * Live Stream Player (HLS) - SIMPLIFIED VERSION
 */

import { API } from '../core/api.js';
import { http, apiCall } from '../core/http.js';
import { showNotification } from '../core/notifications.js';

class LiveStreamPlayer {
  constructor() {
    this.hls = null;
    this.statusCheckInterval = null;
    this.elements = {};
  }

  init() {
    this.elements = {
      video: document.getElementById('videoPlayer'),
      channelSelect: document.getElementById('channelSelect'),
      startBtn: document.getElementById('startBtn'),
      stopBtn: document.getElementById('stopBtn'),
      streamDot: document.getElementById('stream-dot'),
      streamInfoInline: document.getElementById('streamInfoInline'),
      streamResolution: document.getElementById('streamResolution'),
      streamCodec: document.getElementById('streamCodec'),
    };

    if (this.elements.video) {
      this.elements.video.removeAttribute('disabled');
      this.elements.video.style.display = 'block';
      console.log('✅ Video element ready');
    }

    this.setupEventListeners();
    this.checkStreamStatus().catch(() => {});
    window.addEventListener('beforeunload', () => this.cleanup());
  }

  setupEventListeners() {
    this.elements.startBtn?.addEventListener('click', () => this.startStream());
    this.elements.stopBtn?.addEventListener('click', () => this.stopStream());
  }

  async startStream() {
    const channel = this.elements.channelSelect?.value || '102';

    console.log('🎬 Starting stream for channel:', channel);

    this.elements.startBtn.disabled = true;
    this.updateStatus('connecting');

    try {
      const data = await apiCall(
        http.action(API.live.start(channel)),
        'Failed to start stream'
      );

      if (data.status === 'success') {
        console.log('✅ Stream started:', data.playlistUrl);

        // Czekaj aż pliki będą gotowe
        await this.waitForStreamReady(data.playlistUrl);

        // Inicjalizuj player
        this.initializePlayer(data.playlistUrl);

        this.elements.stopBtn.disabled = false;
        this.updateStatus('connected');
        this.startStatusCheck();
      } else {
        throw new Error(data.message || 'Failed to start stream');
      }
    } catch (error) {
      console.error('❌ Stream error:', error);
      this.handleStreamError(error.message);
    }
  }

  async waitForStreamReady(playlistUrl, maxAttempts = 12) {
    console.log('⏳ Waiting for stream files...');

    // Poczekaj 5 sekundy przed pierwszą próbą (FFmpeg potrzebuje czasu)
    await new Promise(resolve => setTimeout(resolve, 5000));

    for (let i = 0; i < maxAttempts; i++) {
      try {
        const response = await fetch(playlistUrl, {
          method: 'GET',
          cache: 'no-cache'
        });

        if (response.ok) {
          const text = await response.text();
          if (text.includes('#EXTM3U') && text.includes('.ts')) {
            console.log(`✅ Stream ready after ${i + 1} attempts`);
            // Poczekaj jeszcze chwilę na pewność
            await new Promise(resolve => setTimeout(resolve, 1000));
            return;
          }
        }
      } catch (err) {
        // Ignoruj błędy - czekamy dalej
      }

      // odstęp między próbami
      await new Promise(resolve => setTimeout(resolve, 2500));
    }

    throw new Error('Stream not ready after 45 seconds');
  }

  async stopStream() {
    console.log('⏹️ Stopping stream');

    if (this.hls) {
      this.hls.destroy();
      this.hls = null;
    }

    if (this.elements.video) {
      this.elements.video.pause();
      this.elements.video.src = '';
    }

    // Ukryj stream info
    if (this.elements.streamInfoInline) {
      this.elements.streamInfoInline.style.display = 'none';
    }

    this.elements.stopBtn.disabled = true;
    this.updateStatus('stopped');
    this.stopStatusCheck();

    try {
      await apiCall(http.action(API.live.stop), 'Failed to stop stream');
      this.elements.startBtn.disabled = false;
    } catch (error) {
      this.elements.startBtn.disabled = false;
    }
  }

  initializePlayer(playlistUrl) {
    const video = this.elements.video;
    if (!video) {
      console.error('❌ Video element not found');
      return;
    }

    console.log('🎥 Initializing HLS player');

    if (Hls.isSupported()) {
      this.hls = new Hls({
        debug: false,
        enableWorker: true,
        lowLatencyMode: true,
        backBufferLength: 10,
        maxBufferLength: 30,
        liveSyncDurationCount: 3,
        liveMaxLatencyDurationCount: 10,
      });

      this.hls.loadSource(playlistUrl);
      this.hls.attachMedia(video);

      this.hls.on(Hls.Events.MANIFEST_PARSED, () => {
        console.log('✅ Manifest loaded, starting playback');

        // Autoplay with mute trick
        video.muted = true;
        video.play().then(() => {
          console.log('✅ Playing (muted)');
          setTimeout(() => {
            video.muted = false;
            console.log('🔊 Audio enabled');
          }, 1000);
        }).catch(err => {
          console.error('❌ Play error:', err);
          showNotification('Click on video to start playback', 'warning');
        });
      });

      // ✅ Wywołaj update info tylko raz, po pierwszym odtwarzaniu
      let infoUpdated = false;
      this.hls.on(Hls.Events.LEVEL_LOADED, () => {
        if (!infoUpdated && video.videoWidth > 0) {
          console.log('📊 Level loaded with video data, updating stream info');
          this.updateStreamInfo();
          infoUpdated = true;
        }
      });

      this.hls.on(Hls.Events.ERROR, (event, data) => {
        this.handleHlsError(data);
      });

      video.addEventListener('playing', () => {
        console.log('▶️ Video is playing');
        this.updateStatus('connected');
      });

      video.addEventListener('waiting', () => {
        console.log('⏸️ Video buffering');
      });

    } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
      // Safari native HLS
      console.log('Using native HLS (Safari)');
      video.src = playlistUrl;
      video.addEventListener('loadedmetadata', () => {
        video.play().catch(err => console.error('Play error:', err));
      });
    } else {
      showNotification('HLS not supported in your browser', 'error');
    }
  }

  handleHlsError(data) {
    if (!data.fatal) {
      // Non-fatal errors (buffering issues) - ignore
      return;
    }

    console.error('❌ HLS fatal error:', data);

    switch (data.type) {
      case Hls.ErrorTypes.NETWORK_ERROR:
        console.log('Network error, attempting recovery...');
        setTimeout(() => {
          if (this.hls) this.hls.startLoad();
        }, 1000);
        break;

      case Hls.ErrorTypes.MEDIA_ERROR:
        console.log('Media error, attempting recovery...');
        if (this.hls) this.hls.recoverMediaError();
        break;

      default:
        console.error('Fatal error - cannot recover');
        this.handleStreamError('Stream playback failed');
        break;
    }
  }

  handleStreamError(message) {
    showNotification(message || 'Stream failed', 'error');
    this.elements.startBtn.disabled = false;
    this.updateStatus('error');
  }

  async checkStreamStatus() {
    try {
      const data = await http.get(API.live.status);

      if (data.active) {
        this.elements.startBtn.disabled = true;
        this.elements.stopBtn.disabled = false;
        this.updateStatus('connected');
      } else {
        this.elements.startBtn.disabled = false;
        this.elements.stopBtn.disabled = true;
        this.updateStatus('stopped');
      }
    } catch (error) {
      // Ignore errors - stream probably not started
      this.elements.startBtn.disabled = false;
      this.elements.stopBtn.disabled = true;
      this.updateStatus('stopped');
    }
  }

  startStatusCheck() {
    this.statusCheckInterval = setInterval(() => {
      this.checkStreamStatus();
    }, 5000);
  }

  stopStatusCheck() {
    if (this.statusCheckInterval) {
      clearInterval(this.statusCheckInterval);
      this.statusCheckInterval = null;
    }
  }

  updateStatus(state) {
    const dot = this.elements.streamDot;
    if (!dot) return;

    // Usuń wszystkie klasy
    dot.classList.remove('connected', 'disconnected');
    // Reset inline styles
    dot.style.backgroundColor = '';
    dot.style.boxShadow = '';

    switch (state) {
      case 'connected':
        dot.classList.add('connected');
        console.log('✅ Status indicator: CONNECTED (green)');
        break;
      case 'connecting':
        dot.classList.add('disconnected'); // Używamy disconnected jako base
        dot.style.backgroundColor = '#ffc107';
        dot.style.boxShadow = '0 0 10px #ffc107';
        console.log('⏳ Status indicator: CONNECTING (yellow)');
        break;
      case 'stopped':
      case 'error':
      default:
        dot.classList.add('disconnected');
        console.log('🔴 Status indicator: DISCONNECTED (red)');
        break;
    }
  }

  updateStreamInfo() {
    if (!this.hls || !this.elements.streamInfoInline) return;

    try {
      setTimeout(() => {
        const video = this.elements.video;
        const currentLevel = this.hls.currentLevel;

        if (currentLevel === -1 || !this.hls.levels[currentLevel]) {
          return;
        }

        const level = this.hls.levels[currentLevel];

        // Resolution
        const width = video.videoWidth || level.width || 0;
        const height = video.videoHeight || level.height || 0;
        const resolution = `${width}x${height}`;

        if (this.elements.streamResolution) {
          this.elements.streamResolution.textContent = resolution;
        }

        // Codec
        let codec = 'H.264';
        if (level.videoCodec) {
          codec = level.videoCodec;
        } else if (level.attrs?.CODECS) {
          codec = level.attrs.CODECS.split(',')[0].trim();
        }

        if (this.elements.streamCodec) {
          this.elements.streamCodec.textContent = codec;
        }

        // Pokaż info inline
        this.elements.streamInfoInline.style.display = 'inline';

        console.log('📊 Stream info:', { resolution, codec });
      }, 1000);
    } catch (err) {
      console.error('❌ Could not update stream info:', err);
    }
  }

  cleanup() {
    this.stopStream();
    this.stopStatusCheck();
  }
}

export const liveStreamPlayer = new LiveStreamPlayer();

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', () => liveStreamPlayer.init());
} else {
  liveStreamPlayer.init();
}