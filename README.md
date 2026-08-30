# KirinDownloader

A modern Android video/audio downloader based on the open-source **Seal / SealPlus** codebase and powered by **yt-dlp**.

> KirinDownloader is a fork and rebrand. Original upstream projects and contributors remain credited below.

## Features

- Download video and audio from sites supported by yt-dlp
- Audio extraction and format conversion
- Playlist downloads
- Subtitle support
- Custom yt-dlp commands and templates
- Download history and task management
- aria2c integration for supported download flows
- Auto-update support from KirinDownloader releases
- Material Design 3 interface
- Android 7.0+ support

## Download

Get the latest KirinDownloader builds from:

**GitHub Releases:**  
https://github.com/Lanzkila/KirinDownloader-Seal/releases

Preview and pre-release builds, when available, are published on the same releases page.

## Repository

**Source:**  
https://github.com/Lanzkila/KirinDownloader-Seal

**Issues / bug reports:**  
https://github.com/Lanzkila/KirinDownloader-Seal/issues


## Gallery DL

KirinDownloader includes an optional **Gallery DL** tool for image galleries and collections supported by gallery-dl.

The APK includes the Chaquopy Python runtime and the Requests dependency, but **does not bundle gallery-dl itself**. When the user chooses **Install / Update Engine**, the app downloads the current pure-Python gallery-dl wheel from official PyPI, verifies the SHA-256 digest published by PyPI, and installs it into app-private storage.

This keeps gallery-dl independently updateable and avoids combining its GPL-2.0-only distribution directly into the GPL-3.0 APK. gallery-dl remains a separate upstream project: https://github.com/mikf/gallery-dl

Only save content you have permission or rights to download.

## Supported Sites

KirinDownloader uses **yt-dlp** as its main extraction engine.

Supported site information is maintained by the yt-dlp project:

https://github.com/yt-dlp/yt-dlp/blob/master/supportedsites.md

Please download only content you have permission or rights to save.

## Main Technology

- Kotlin
- Jetpack Compose
- Material Design 3
- yt-dlp / youtubedl-android
- aria2c
- FFmpeg
- Room
- Koin
- Coil
- OkHttp

## Build

The project uses Gradle Kotlin DSL.

A normal release build can be produced with:

```bash
./gradlew assembleGenericRelease
```

KirinDownloader also includes a GitHub Actions build workflow for testing release builds.

## Updating yt-dlp

The app can update the yt-dlp component independently.

The yt-dlp updater continues to use the official yt-dlp project/build source and is separate from KirinDownloader's own APK update system.

## Open Source Credits

KirinDownloader exists because of the work of the projects and contributors it is based on.

### Seal

Original project by **JunkFood02**:

https://github.com/JunkFood02/Seal

Seal provides the original Android downloader foundation and remains an important upstream project.

### SealPlus

KirinDownloader-Seal was forked from **SealPlus**, maintained by **MaheshTechnicals**:

https://github.com/MaheshTechnicals/Sealplus

Features and improvements inherited from SealPlus remain credited to their original authors and contributors.

### Other Core Projects

- yt-dlp — https://github.com/yt-dlp/yt-dlp
- youtubedl-android — https://github.com/yausername/youtubedl-android
- aria2 — https://github.com/aria2/aria2
- FFmpeg — https://ffmpeg.org/

Additional third-party libraries retain their own licenses and copyright notices in the source project.

## License

KirinDownloader is distributed under the **GNU General Public License v3.0 (GPL-3.0)** in accordance with the license of its upstream codebase.

See:

https://github.com/Lanzkila/KirinDownloader-Seal/blob/main/LICENSE

Do not remove upstream copyright, attribution, or license notices from source files derived from the original projects.

---

**KirinDownloader**  
Fork lineage: **Seal → SealPlus → KirinDownloader**
