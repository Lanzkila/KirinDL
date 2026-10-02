<div align="center">

<img src="app/src/main/res/drawable/splash_logo.png" width="142" alt="KirinDL Logo">

# KirinDL

### Media • Gallery • Batch

Android downloader powered by **yt-dlp** and **gallery-dl**, with batch workflows, updateable engines, Gallery DL support, and signed multi-architecture releases.

[![Stars](https://img.shields.io/github/stars/Lanzkila/KirinDL?style=flat-square&logo=github)](https://github.com/Lanzkila/KirinDL/stargazers)
[![Forks](https://img.shields.io/github/forks/Lanzkila/KirinDL?style=flat-square&logo=github)](https://github.com/Lanzkila/KirinDL/forks)
[![Downloads](https://img.shields.io/github/downloads/Lanzkila/KirinDL/total?style=flat-square&logo=github&label=Downloads)](https://github.com/Lanzkila/KirinDL/releases)
[![Release](https://img.shields.io/github/v/release/Lanzkila/KirinDL?style=flat-square&label=KirinDL)](https://github.com/Lanzkila/KirinDL/releases)
[![Kirin Pre-Release](https://img.shields.io/github/v/release/Lanzkila/KirinDL?include_prereleases&sort=date&filter=*devpatch*&style=flat-square&label=Kirin%20Pre-Release)](https://github.com/Lanzkila/KirinDL/releases)
[![License](https://img.shields.io/github/license/Lanzkila/KirinDL?style=flat-square)](LICENSE)

[![yt-dlp stable](https://img.shields.io/github/v/release/yt-dlp/yt-dlp?style=flat-square&label=yt-dlp%20stable)](https://github.com/yt-dlp/yt-dlp/releases/latest)
[![gallery-dl stable](https://img.shields.io/github/v/release/mikf/gallery-dl?style=flat-square&label=gallery-dl%20stable)](https://codeberg.org/mikf/gallery-dl/releases)

**Fork lineage:** Seal → SealPlus → KirinDL

</div>

---

## ✦ Features

### Media Downloader
Powered by **yt-dlp**.

- Video and audio downloads
- Quality / format selection
- Playlists and batch workflows
- Subtitles, metadata and thumbnails
- SponsorBlock
- FFmpeg processing
- aria2 integration
- Custom yt-dlp commands
- Download queue, history and background downloading
- Phase-aware progress for Video, Audio, Fragments, Merge and Processing

### Gallery DL
Powered by **gallery-dl**.

- Single and batch Gallery URLs
- Persistent queue and history
- Extractor preflight and preview
- Cookies support
- Config import / export
- Expert JSON configuration
- Gallery DL engine updates from Codeberg
- Global Feed and Kirin Search integration

---

## ✦ Engine Updates

KirinDL keeps its engines separate from the APK release cycle.

**yt-dlp**
- Stable
- Nightly

**gallery-dl**
- Updateable from the active Codeberg source

This allows extractor and compatibility fixes to arrive without waiting for a new KirinDL APK.

---

## ✦ Releases

Official releases:

https://github.com/Lanzkila/KirinDL/releases

Stable releases support:

- Universal
- arm64-v8a
- armeabi-v7a
- x86
- x86_64
- SHA-256 checksums

**Package:** `com.kirin.downloader`

Release APKs are signed and verified in GitHub Actions before publishing.

---

## ✦ Build

### Debug

```bash
./gradlew assembleGenericDebug --stacktrace --no-daemon --no-configuration-cache
```

### Release

```bash
./gradlew assembleGenericRelease --stacktrace --no-daemon --no-configuration-cache
```

Native targets:

```text
arm64-v8a
armeabi-v7a
x86
x86_64
```

---

## ✦ Stack

Kotlin • Jetpack Compose • Material 3 • yt-dlp • gallery-dl • Chaquopy • FFmpeg • aria2 • Room • Koin • Coil • OkHttp

---

## ✦ Notes

Site support follows the upstream extraction engines and can change over time. Some sites may require authentication, cookies, newer engine versions, or additional runtime helpers.

Only download content that you have permission or the rights to save.

---

## ✦ Open Source Credits

KirinDL builds on:

- **Seal** — https://github.com/JunkFood02/Seal
- **SealPlus** — https://github.com/MaheshTechnicals/Sealplus
- **yt-dlp** — https://github.com/yt-dlp/yt-dlp
- **youtubedl-android** — https://github.com/yausername/youtubedl-android
- **gallery-dl** — https://codeberg.org/mikf/gallery-dl
- **aria2** — https://github.com/aria2/aria2
- **FFmpeg** — https://ffmpeg.org/

Upstream copyright, license notices and attribution remain with their respective projects and contributors.

---

## ✦ License

KirinDL is distributed under the **GNU General Public License v3.0 (GPL-3.0)**.

See [LICENSE](LICENSE).

---

<div align="center">

### KirinDL

**Media • Gallery • Batch**

Powered by yt-dlp + gallery-dl.

</div>
