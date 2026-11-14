# Fisalia Files 📁🌐

Fisalia Files is an **Android file manager** with a clean interface inspired by Files by Google. It includes built-in media players, system-language localization, local and SD card browsing, and network storage support for **SMB, FTP/FTPS, SFTP, and WebDAV**.

> This repository contains the Android app only.

---

## Features

### 🎬 Built-in media players

Play media without installing a third-party app:

- **Video player** powered by AndroidX Media3 ExoPlayer, with play/pause, 10-second seek controls, a progress slider, and an option to open videos in another app.
- **Audio player** with album art, playback controls, and a progress slider. Supports common formats including MP3, AAC, FLAC, WAV, M4A, and OGG.
- **Image viewer** with pinch-to-zoom (up to 5x), panning, sharing, and an option to open images in another app.
- Swipe between media items while viewing or playing them.

### 🌐 System-language localization

The app follows the Android system language without requiring a manual language setting:

- **Indonesian** when the device language is Indonesian (`values-in` and `values-id`).
- **English** by default for other languages (`values`).

### 🎨 Material 3 interface

- Search files from the search bar.
- Navigate between **Clean**, **Browse**, and **Network Storage**.
- Browse Downloads, Images, Videos, Audio, Documents, and Apps categories.
- View storage capacity and usage for internal storage, SD cards, and network storage.
- Navigate nested folders with breadcrumbs.

### 🌐 Network storage

Connect to a PC, NAS, router, or server using:

- **SMB / Samba** for Windows shares, including SMB2 and SMB3.
- **FTP and FTPS** with passive mode.
- **SFTP** over SSH.
- **WebDAV**.

Save server profiles, test connections, and browse remote files. Supported operations include creating folders, deleting, downloading, and uploading files where available for the protocol.

### 🛠️ File management

- Open, share, rename, delete, copy, and move files.
- Create and extract ZIP archives.
- Sort by name, modification date, or file size.
- Use Android storage permissions for browsing local files.
- Open files in other apps through Android's `FileProvider` integration.

---

## Technology

- **Language:** Kotlin
- **UI:** Jetpack Compose and Material 3
- **Architecture:** MVVM with Kotlin Coroutines and StateFlow
- **Media playback:** AndroidX Media3 ExoPlayer
- **SMB:** `com.hierynomus:smbj`
- **FTP/FTPS:** Apache Commons Net
- **SFTP:** JSch
- **Images:** Coil
- **Navigation:** Navigation Compose
- **Serialization:** Gson

---

## Project structure

```text
fisaliafiles/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/fisalia/files/
│       │   ├── data/
│       │   │   ├── local/       # Local file operations and preferences
│       │   │   ├── model/       # File, storage, and connection models
│       │   │   └── network/     # Network storage clients
│       │   └── ui/
│       │       ├── components/
│       │       ├── player/
│       │       ├── screens/
│       │       └── theme/
│       └── res/
│           ├── values/          # Default (English) strings and themes
│           ├── values-in/       # Indonesian strings
│           ├── values-id/       # Indonesian strings (BCP 47)
│           ├── drawable/
│           ├── mipmap-anydpi-v26/
│           └── xml/
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## Build and run

1. Open this repository in Android Studio.
2. Allow Gradle synchronization to finish.
3. Connect an Android device with USB debugging enabled or start an Android emulator.
4. Select **Run** in Android Studio.

To build a debug APK from Windows:

```powershell
.\gradlew.bat assembleDebug
```

The APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Install it on a connected device or emulator with:

```powershell
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
