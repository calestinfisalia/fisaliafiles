# Fisalia Files 📁🌐

Aplikasi Android File Manager modern dengan antarmuka (UI) yang dirancang menyerupai **Google Files (Files by Google)**, dilengkapi dengan pemutar media bawaan (*built-in player*), lokalisasi multi-bahasa yang otomatis mengikuti bahasa sistem perangkat, kemampuan menjelajahi penyimpanan lokal & kartu SD, serta protokol penyimpanan jaringan seperti **SMB (Samba / Windows Share)**, **FTP / FTPS**, **SFTP (SSH)**, dan **WebDAV**.

---

## ✨ Fitur Utama

### 🎬 1. Pemutar Media Bawaan (Built-in Players)
Tidak perlu menginstal aplikasi pihak ketiga untuk menikmati file multimedia:
* **Pemutar Video Bawaan (Video Player)**:
  * Dibangun menggunakan **AndroidX Media3 ExoPlayer** untuk performa streaming dan decoding video terbaik (MP4, MKV, WebM, MOV, 3GP, AVI, dll).
  * Kontrol pemutaran intuitif: Putar / Jeda (*Play / Pause*), Lompat Maju 10 detik, Mundur 10 detik.
  * Bilah durasi interaktif (*Seekbar / Slider*) dengan waktu saat ini & total durasi.
  * Opsi *"Buka dengan aplikasi lain"* (*Open with other app*) jika pengguna ingin beralih ke pemutar eksternal seperti VLC atau MX Player.
* **Pemutar Audio Bawaan (Audio Player)**:
  * Tampilan *Modal Bottom Sheet* modern yang elegan.
  * Dilengkapi visualizer ikon musik, kontrol Play/Pause, Rewind/Forward 10s, dan slider durasi.
  * Mendukung format MP3, AAC, FLAC, WAV, M4A, OGG.
* **Pratinjau Gambar Interaktif (Image Viewer)**:
  * Tampilan gambar layar penuh dengan kemampuan **Pinch-to-Zoom** (perbesar hingga 5x) dan **Pan / Geser**.
  * Dilengkapi tombol berbagi (*Share*) dan buka dengan aplikasi lain.

---

### 🌐 2. Bahasa Menyesuaikan Sistem HP (Multi-Language Localization)
* **Otomatis Beradaptasi**: Teks dan istilah di seluruh aplikasi secara dinamis menyesuaikan bahasa sistem Android pengguna tanpa perlu pengaturan manual:
  * 🇮🇩 **Bahasa Indonesia** (`values-in/strings.xml` & `values-id/strings.xml`): Ketika bahasa HP disetel ke Bahasa Indonesia.
  * 🇬🇧 **Bahasa Inggris / Default** (`values/strings.xml`): Ketika bahasa HP disetel ke Bahasa Inggris atau bahasa internasional lainnya.

---

### 🎨 3. Desain Antarmuka Mirip Google Files (Material 3)
* **Bilah Penelusuran (Search Bar)**: Kotak pencarian berbentuk kapsul khas Google Files untuk mencari file secara instan.
* **Bilah Navigasi Bawah (Bottom Navigation Bar)**:
  * **Bersihkan (Clean)**: Menganalisis file sampah (junk/cache), file berukuran besar (>50MB), dan file duplikat.
  * **Jelajahi (Browse)**: Tampilan dasbor utama dengan kartu status kapasitas memori (Internal & SD Card), ubin kategori berwarna, dan koleksi.
  * **Jaringan (Network)**: Manajemen server remote dan penyimpanan jaringan.
* **Ubin Kategori Berwarna Google**:
  * 📥 Unduhan (*Downloads*)
  * 🖼️ Gambar (*Images*)
  * 🎥 Video
  * 🎵 Audio
  * 📄 Dokumen & Lainnya (*Documents*)
  * 🤖 Aplikasi (*APKs*)
* **Indikator Penyimpanan**: Menampilkan kapasitas penyimpanan yang terpakai dan bebas secara visual dengan bilah progres.
* **Breadcrumb Navigation**: Navigasi remah roti interaktif saat menjelajahi folder bertingkat.

---

### 🌐 4. Akses Penyimpanan Jaringan (Network Storage)
Mendukung koneksi langsung ke PC, NAS (Network Attached Storage), router, atau server VPS melalui protokol:
* **SMB / Samba (Windows Share)**:
  * Mendukung SMB2 & SMB3 menggunakan library modern `smbj`.
  * Dukungan autentikasi akun (Username/Password), Domain Windows/Workgroup, atau mode Anonim/Guest.
  * Penjelajahan folder remote, pembuatan folder baru, penghapusan, unduh, dan unggah.
* **FTP & FTPS (Secure FTP)**:
  * Mendukung koneksi FTP standar dan FTPS (SSL/TLS) via `commons-net`.
  * Mode Pasif (Passive Mode) otomatis untuk stabilitas di balik firewall / WiFi NAT.
* **SFTP (SSH File Transfer Protocol)** & **WebDAV**:
  * Modul transfer file melalui SSH port 22 dan endpoint WebDAV.
* **Manajer Server Tersimpan**:
  * Simpan profil koneksi server favorit.
  * Fitur **Uji Koneksi (Test Connection)** sebelum menyimpan server.
  * Masuk cepat ke folder remote hanya dengan satu ketukan.

---

### 🛠️ 5. Fitur Umum File Manager
* **Manajemen File Lengkap**: Buka (Open), Bagikan (Share), Ganti Nama (Rename), Hapus (Delete), Salin (Copy), Pindahkan (Move/Cut).
* **Kompresi & Ekstraksi Arsip**:
  * Kompres beberapa file atau folder menjadi file `.zip`.
  * Ekstrak isi file `.zip` langsung di perangkat.
* **Penanganan Izin Android Modern**:
  * Mendukung Android 11+ (API 30+) menggunakan `MANAGE_EXTERNAL_STORAGE` untuk akses penuh seluruh direktori.
  * Fallback otomatis untuk Android 10 kebawah dan Android 13+ MediaStore permissions.
* **Integrasi FileProvider**: Membuka berbagai jenis file dengan aplikasi pihak ketiga secara aman tanpa kendala `FileUriExposedException`.
* **Clipboard (Cut / Copy / Paste)**: Bilah aksi mengambang (Floating Action Button) untuk menempel file di direktori tujuan.
* **Pengurutan (Sort)**: Berdasarkan Nama (A-Z / Z-A), Tanggal Modifikasi, dan Ukuran File.

---

## 🏗️ Arsitektur & Teknologi

* **Bahasa**: Kotlin 2.0+
* **UI Toolkit**: Jetpack Compose dengan Material Design 3 (M3)
* **Arsitektur**: MVVM (Model - View - ViewModel) dengan Kotlin Coroutines & StateFlow
* **Library Utama**:
  * `androidx.media3:media3-exoplayer`: Engine pemutar video dan audio bawaan berkinerja tinggi.
  * `androidx.media3:media3-ui`: Komponen view untuk rendering video.
  * `com.hierynomus:smbj`: Klien SMB2/SMB3 Java murni.
  * `commons-net:commons-net`: Klien FTP & FTPS Apache.
  * `com.jcraft:jsch`: Klien SSH & SFTP.
  * `io.coil-kt:coil-compose`: Pemuatan thumbnail gambar dan video berkinerja tinggi.
  * `androidx.navigation:navigation-compose`: Navigasi halaman Compose.
  * `com.google.code.gson:gson`: Serialisasi profil koneksi jaringan.

---

## 📁 Struktur Direktori Proyek

```
fisaliafiles/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml
│           ├── java/com/fisalia/files/
│           │   ├── FisaliaApplication.kt
│           │   ├── MainActivity.kt
│           │   ├── data/
│           │   │   ├── local/          # LocalFileRepository, FileOperationsHelper, ConnectionPreferences
│           │   │   ├── model/          # FileItem, StorageItem, NetworkConnection, FileCategory, SortOption
│           │   │   └── network/        # SmbService, FtpService, NetworkClientManager
│           │   └── ui/
│           │       ├── components/     # TopBar, BottomNav, StorageCard, CategoryGrid, FileListItem, BreadcrumbBar, Dialogs
│           │       ├── player/         # VideoPlayerScreen, AudioPlayerSheet, ImageViewerScreen, MediaPlayerState
│           │       ├── screens/
│           │       │   ├── browse/     # BrowseScreen, ExplorerScreen, BrowseViewModel
│           │       │   ├── clean/      # CleanScreen, CleanViewModel
│           │       │   └── network/    # NetworkScreen, NetworkViewModel
│           │       └── theme/          # Color, Theme, Type (Google Files styling)
│           └── res/
│               ├── values/             # strings.xml (Default / English)
│               ├── values-in/          # strings.xml (Bahasa Indonesia)
│               ├── values-id/          # strings.xml (Bahasa Indonesia BCP-47)
│               ├── drawable/
│               ├── mipmap-anydpi-v26/
│               └── xml/
├── gradle/
│   ├── libs.versions.toml             # Gradle Version Catalog
│   └── wrapper/
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## 🚀 Cara Menjalankan Proyek di Android Studio

1. **Buka Proyek**:
   * Buka Android Studio.
   * Pilih **File** -> **Open**, lalu pilih folder `c:\Users\remielledan\Documents\fisaliafiles`.
2. **Sinkronisasi Gradle**:
   * Tunggu hingga Android Studio selesai melakukan `Gradle Sync`.
3. **Pilih Perangkat / Emulator**:
   * Hubungkan perangkat fisik Android melalui USB Debugging atau jalankan Android Emulator.
4. **Jalankan Aplikasi**:
   * Klik tombol **Run** (segitiga hijau) atau tekan `Shift + F10`.
