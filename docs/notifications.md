# 🔔 Notification System

The Hikvision Manager supports sending backup completion and failure notifications through multiple channels. You can enable one or more notification providers simultaneously to receive alerts via your preferred platform.

## 📋 Table of Contents

- [Supported Providers](#supported-providers)
- [Quick Start](#quick-start)
- [Configuration Guide](#configuration-guide)
    - [NTFY](#ntfy-configuration)
    - [Discord](#discord-configuration)
    - [Generic Webhook](#generic-webhook-configuration)

---

## Supported Providers

| Provider | Type | Features | Best For |
|----------|------|----------|----------|
| **NTFY** | Push notifications | Self-hosted or public, mobile apps, markdown | Personal use, self-hosted setups |
| **Discord** | Webhook | Rich embeds, colored alerts, inline fields | Teams, communities, Discord users |
| **Generic Webhook** | HTTP POST | Universal, custom headers, JSON payload | Zapier, n8n, Make.com, custom APIs |

---

## Quick Start

### 1. Choose Your Provider

Pick one or more notification providers from the list above.

### 2. Get Credentials

- **NTFY**: Server URL and topic name (optionally username/password)
- **Discord**: Webhook URL from Discord server settings
- **Generic Webhook**: Your API endpoint URL (and auth tokens if needed)

### 3. Configure Environment Variables

Add to your `.env` file:

```bash
# Example: NTFY with authentication
NTFY=ntfy://username:password@ntfy.domain.com/hikvision-backups?title=Camera Backups&scheme=https

# Example: Discord webhook
DISCORD=discord://1234567890/abcdefghijklmnopqrstuvwxyz?username=Hikvision Manager

# Example: Generic webhook with auth
WEBHOOK=webhook://api.domain.com/notify?scheme=https&header_Authorization=Bearer token123
```

### 4. Enable Notifications for Backups

Set `notifyOnComplete: true` when creating or editing backup configurations via API or frontend.

---

## Configuration Guide

### NTFY Configuration

**NTFY** is a simple pub-sub notification service. You can use the public server at `ntfy.sh` or self-host your own instance.

#### URL Format

```
ntfy://[username:password@]host[:port]/topic?[params]
```

#### Components

- `username:password@` - Optional authentication (Basic Auth)
- `host` - NTFY server hostname (e.g., `ntfy.sh`, `ntfy.domain.com`)
- `:port` - Optional port (default: 80 for HTTP, 443 for HTTPS)
- `/topic` - Topic/channel name for notifications
- `?params` - Query parameters:
    - `scheme=https` - Use HTTPS instead of HTTP (recommended)
    - `title=My App` - Default notification title
    - `priority=default` - Default priority level

#### Examples

**Public NTFY server (no auth):**
```bash
NTFY=ntfy://ntfy.sh/my-backup-alerts?title=Hikvision Backup&scheme=https
```

**Self-hosted with authentication:**
```bash
NTFY=ntfy://admin:securepass@ntfy.domain.com/hikvision-backups?title=Camera Backups&scheme=https
```

**Custom port:**
```bash
NTFY=ntfy://user:pass@ntfy.domain.com:8080/backups?scheme=https
```

**Special characters in password:**
```bash
# Password: MyP@ss!
# URL encoded: MyP%40ss%21
NTFY=ntfy://user:MyP%40ss%21@ntfy.domain.com/backups?scheme=https
```

#### Notification Format

```
✅ Backup Completed Successfully

📊 Recordings: 45/50 completed
💾 Size: 2.3 GB
⏱️ Duration: 12 minutes

📂 Location: /backups/2024-11-23
```

---

### Discord Configuration

**Discord** webhooks allow sending rich embedded messages to Discord channels with colored sidebars and structured fields.

#### Getting Discord Webhook URL

1. Open Discord and go to your server
2. Click **Server Settings** → **Integrations** → **Webhooks**
3. Click **New Webhook** or edit existing one
4. Customize webhook name and avatar (optional)
5. Click **Copy Webhook URL**
6. You'll get: `https://discord.com/api/webhooks/1234567890/abcdefghijklmnopqrstuvwxyz`

#### URL Format

```
discord://webhook-id/webhook-token?[params]
```

#### Converting Discord Webhook URL

Given Discord URL:
```
https://discord.com/api/webhooks/1234567890/abcdefghijklmnopqrstuvwxyz
```

Extract:
- **Webhook ID**: `1234567890`
- **Webhook Token**: `abcdefghijklmnopqrstuvwxyz`

Format for Hikvision Manager:
```
discord://1234567890/abcdefghijklmnopqrstuvwxyz
```

#### Optional Parameters

- `username=Bot Name` - Override default webhook username
- `avatar_url=https://...` - Override default webhook avatar

#### Examples

**Basic webhook:**
```bash
DISCORD=discord://1234567890/abcdefghijklmnopqrstuvwxyz
```

**With custom bot name:**
```bash
DISCORD=discord://1234567890/abcdefghijklmnopqrstuvwxyz?username=Hikvision Manager
```

**With custom name and avatar:**
```bash
DISCORD=discord://1234567890/abcdefghijklmnopqrstuvwxyz?username=Camera Bot&avatar_url=https://domain.com/bot-avatar.png
```

#### Notification Format

Discord receives a rich embed with:

```
┌──────────────────────────────────┐
│ 🟢 ✅ Backup: Daily Backup       │ ← Title 
├──────────────────────────────────┤
│ Backup completed                 │ ← Description
│                                  │
│ 📊 Recordings    💾 Size        │ ← Inline fields
│ 45/50 completed  2.3 GB          │
│                                  │
│ ⏱️ Duration                     │
│ 12 minutes                       │
│                                  │
│ 📂 Location                      │ ← Full width field
│ /backups/2024-11-23              │
├──────────────────────────────────┤
│ Hikvision Manager • 2:30 PM      │ ← Footer + Timestamp
└──────────────────────────────────┘
```

---

### Generic Webhook Configuration

**Generic Webhook** sends JSON POST requests to any HTTP endpoint. This is useful for integrating with automation platforms like Zapier, Make.com, n8n, or custom APIs.

#### URL Format

```
webhook://host[:port]/path?scheme=https[&header_Key=Value]
```

#### Components

- `host` - Target server hostname
- `:port` - Optional port
- `/path` - API endpoint path
- `?scheme=https` - Use HTTPS (recommended)
- `&header_Key=Value` - Custom HTTP headers (prefix with `header_`)

#### Custom Headers

Add HTTP headers by prefixing query params with `header_`:

- `header_Authorization=Bearer token` → `Authorization: Bearer token`
- `header_X-API-Key=secret` → `X-API-Key: secret`
- `header_Content-Type=application/json` → `Content-Type: application/json`

#### Examples

**Basic webhook (HTTPS):**
```bash
WEBHOOK=webhook://api.domain.com/notify?scheme=https
```

**With Bearer token authentication:**
```bash
WEBHOOK=webhook://api.domain.com/webhook?scheme=https&header_Authorization=Bearer abc123xyz
```

**With multiple custom headers:**
```bash
WEBHOOK=webhook://api.domain.com/notify?scheme=https&header_Authorization=Bearer token&header_X-API-Key=secret&header_X-Source=hikvision
```

**Zapier webhook:**
```bash
WEBHOOK=webhook://hooks.zapier.com/hooks/catch/123456/abcdef?scheme=https
```

**n8n webhook:**
```bash
WEBHOOK=webhook://your-n8n.domain.com/webhook/backup?scheme=https&header_Authorization=Bearer your-n8n-token
```

#### JSON Payload

The webhook receives a JSON POST request:

```json
{
  "title": "✅ Backup: Daily Backup",
  "message": "📥 Backup Completed\n\n📊 Recordings: 50/50 completed\n💾 Size: 2.3 GB\n⏱️ Duration: 12 minutes\n\n📂Location: `/backups/2025-11-23`",
  "priority": "default",
  "timestamp": "2025-11-23T14:30:00Z",
  "source": "hikvision-manager",
  "metadata": {
    "custom_field": "custom_value"
  }
}
```
