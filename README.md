
<div align="center">  <img src="images/logo.png"> </div>

Hikvision Manager is a Spring Boot application designed to manage Hikvision Cameras, execute backup jobs, store backup history, and provide a UI for browsing and downloading recordings.
It integrates with Hikvision ISAPI endpoints, schedules recording backups, stores metadata in PostgreSQL, and exposes a management dashboard. Also provides HTTP Live Streaming by HLS and FFmpeg.

![Java](https://img.shields.io/badge/Java-24-2496ED)
![SpringBoot](https://img.shields.io/badge/Spring_Boot_3.5.7-555555?style=flat-square&logo=springboot&logoColor=3fb950)
![FFmpeg](https://img.shields.io/badge/FFmpeg-555555?style=flat-square&logo=ffmpeg&logoColor=3fb950)
![Maven](https://img.shields.io/badge/Maven-555555?style=flat-square&logo=apache&logoColor=3fb950)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL_16-555555?style=flat-square&logo=postgresql&logoColor=3fb950)
![Thymeleaf](https://img.shields.io/badge/Thymeleaf-555555?style=flat-square&logo=thymeleaf&logoColor=3fb950)
![Bootstrap](https://img.shields.io/badge/Bootstrap-555555?logo=bootstrap&logoColor=3fb950)
![Javascript](https://img.shields.io/badge/JavaScript-555555?logo=javascript&logoColor=3fb950)
![Docker](https://img.shields.io/badge/Docker_Ready-2496ED?logo=docker&logoColor=fff)

## ✨ Features

### 🎥 Live Preview
- Real-time RTSP stream viewing via HLS (HTTP Live Streaming)
- Channel switching (Main stream 101, Sub stream 102)
- Automatic session cleanup
- No transcoding (camera must provide H.264/H.265)

### 📼 Recordings Management
- Browse and search camera recordings
- Direct download links for recordings
- Real-time statistics for downloads

### 💾 Backup System
- Automated scheduled backups
- Manual on-demand backups
- Live backup progress tracking 
- Detailed logs for every backup
- Retention and cleanup policies

### 🛠️ Camera Management
- Camera restart functionality
- System information display (model, firmware, serial number)
- Real-time status monitoring (uptime, temperature, CPU, memory)
- Storage information (HDD capacity, free space, usage percentage)
- Time and network settings access


## 🏗️ Architecture

### Technology Stack
- **Backend**: Spring Boot 3.5.7, Java 24
- **Cache**: Caffeine
- **Database**: PostgreSQL 16
- **Frontend**: Thymeleaf, Bootstrap 5.3, JavaScript
- **Streaming**: FFmpeg, HLS.js
- **Build Tool**: Maven

### Key Components
- **ISAPI Integration**: Direct communication with Hikvision camera API
- **HLS Streaming**: FFmpeg-based RTSP to HLS conversion
- **Scheduled Tasks**: Automated backups and cleanup
- **Flyway** automatic database migrations

## 🚀 Quick Start
You can run the application either locally without Docker or via Docker Compose (recommended).

## ⚙️ Local Development (without Docker)
### Prerequisites
- JDK 24
- Maven
- PostgreSQL 16 installed locally
- FFmpeg installed locally

### Installation

1. **Clone the repository**
```bash
git clone https://github.com/kCn3333/hikvision-manager.git
cd hikvision-manager
```
2. **Make sure, you have a PostgreSQL installed**
```bash
# Ubuntu/Debian
sudo apt install postgresql-16

```
3. **Create Database**
```sql
CREATE DATABASE camera_db;
CREATE USER postgres WITH PASSWORD 'password';
GRANT ALL PRIVILEGES ON DATABASE camera_db TO postgres;
```
4. **Make sure, you have a FFmpeg installed**
```bash
# Ubuntu/Debian
sudo apt install ffmpeg
```
5. **Configure environment variables**
```bash
cp .env.example .env
# Edit .env with your camera credentials and settings
```
6. **Start with Maven**
```bash
mvn clean spring-boot:run
```
7. **Access the application**
```
http://localhost:8081
```

## 🐳 Docker Deployment (recommended)

1. **Clone the repository**
```bash
git clone https://github.com/kCn3333/hikvision-manager.git
cd hikvision-manager
```
2. **Configure environment variables**
```bash
cp .env.example .env
# Edit .env with your camera credentials and settings
```
3. **Start services**
```bash
docker compose --env-file .env up -d
```
4. **Access the application**
```
http://localhost:8081
```
 logs: 
```bash
docker logs -f hikvision-manager
```
## ⚙️ Configuration

### Environment Variables

Create a `.env` file in the project root:

```env
# --- Database ---
DB_PASSWORD=changeme

# --- Camera ---
CAMERA_IP=192.168.0.2
CAMERA_PORT=80
CAMERA_USERNAME=admin
CAMERA_PASSWORD=password
CAMERA_RTSP_PORT=554
TIMEZONE=UTC

# --- Optional ---
# APP_FRONTEND_URL=http://localhost:8081
```

### Camera Setup

Ensure your Hikvision camera has:
- ISAPI enabled (usually enabled by default)
- HTTP authentication set to Digest
- RTSP enabled on port 554


### Supported Camera Models

Tested with:
- DS-2CD2xxx series
- DS-2DE series


## 📁 Project Structure

```
hikvision-manager/
├── src/main/
│   ├── java/com/kcn/hikvision-manager/
│   │   ├── client/               # HTTP client for Hikvision ISAPI
│   │   ├── config/               # Spring configuration classes
│   │   ├── controller/           # REST and MVC controllers
│   │   ├── domain/               # Domain models 
│   │   ├── dto/                  # API request/response DTOs
│   │   ├── entity/               # JPA entities mapped to PostgreSQL
│   │   ├── events/               # Domain events
│   │   ├── exception/            # Custom exceptions + global handler
│   │   ├── mapper/               # Mappers
│   │   ├── repository/           # Repositories for DB and Cache access
│   │   ├── service/              # Business logic and device operations
│   │   ├── scheduler/            # Scheduled backup jobs
│   │   └── util/                 # Utility classes
│   └── resources/
│       ├── db/migration/         # Flyway scripts
│       ├── static/
│       │   ├── css/              # CSS style
│       │   └── js/               # Java Script
│       ├── templates/            # Thymeleaf HTML templates
│       └── application.properties
├── Dockerfile
├── docker-compose.yml
└── README.md                     # this file
```


## 🔐 Security Considerations

- Always change default PostgreSQL and camera passwords
- Consider running behind reverse proxy (nginx, caddy)
- Enable HTTPS for production deployment
- Restrict network access to camera


## 📝 License

This project is licensed under the MIT License - see the LICENSE file for details.


