<div align="center">

<img src="app/src/main/res/drawable/splash_logo.png" width="142" alt="KirinDL Logo">

# KirinDL

### Media • Gallery • Batch

A modern Android downloader powered by **yt-dlp** and **gallery-dl**, with a Kirin-focused interface, batch workflows, updateable engines, and signed Universal builds.

[![Build](https://github.com/Lanzkila/KirinDL/actions/workflows/kirin-build-test.yml/badge.svg)](https://github.com/Lanzkila/KirinDL/actions/workflows/kirin-build-test.yml)
[![Stars](https://img.shields.io/github/stars/Lanzkila/KirinDL?style=flat-square&logo=github)](https://github.com/Lanzkila/KirinDL/stargazers)
[![Forks](https://img.shields.io/github/forks/Lanzkila/KirinDL?style=flat-square&logo=github)](https://github.com/Lanzkila/KirinDL/forks)
[![Release](https://img.shields.io/github/v/release/Lanzkila/KirinDL?style=flat-square&label=KirinDL)](https://github.com/Lanzkila/KirinDL/releases)
[![License](https://img.shields.io/github/license/Lanzkila/KirinDL?style=flat-square)](LICENSE)

[![yt-dlp stable](https://img.shields.io/github/v/release/yt-dlp/yt-dlp?style=flat-square&label=yt-dlp%20stable)](https://github.com/yt-dlp/yt-dlp/releases/latest)
[![yt-dlp nightly](https://img.shields.io/github/v/release/yt-dlp/yt-dlp-nightly-builds?style=flat-square&label=yt-dlp%20nightly)](https://github.com/yt-dlp/yt-dlp-nightly-builds/releases/latest)
[![gallery-dl stable](https://img.shields.io/github/v/release/mikf/gallery-dl?style=flat-square&label=gallery-dl%20stable)](https://codeberg.org/mikf/gallery-dl/releases)

**Fork lineage:** Seal → SealPlus → KirinDL

</div>

---

## ✦ Overview

**KirinDL** brings media downloads and gallery downloads into one Android app while keeping the two extraction engines independently updateable.

Kirin-specific additions include:

- KirinDL visual identity
- dedicated Gallery DL hub
- single + batch Gallery URLs
- persistent Gallery queue and history
- theme-aware Gallery interface
- optional Gallery accent variants
- Codeberg-based gallery-dl engine updater
- yt-dlp Stable / Nightly updater
- Universal APK output
- stable debug certificate
- private signed release workflow
- KirinDL GitHub release checks

> **Visible app name:** `KirinDL`  
> **Release package:** `com.kirin.downloader`  
> The package ID is intentionally kept stable so future APK releases signed with the same key can update the installed app.

---

## ✦ Media Downloader

KirinDL uses **yt-dlp** as the main media extraction engine.

### Features

- video and audio downloads
- audio extraction and conversion
- quality / format selection
- playlist processing
- subtitle support
- metadata and thumbnails
- SponsorBlock support
- reusable yt-dlp command templates
- custom commands
- download history
- queue management
- background downloading
- FFmpeg processing
- aria2 integration
- share-to-KirinDL intents

Website and format support follows upstream yt-dlp extractor support and can change over time.

---

## ✦ Gallery DL

KirinDL also provides a separate **Gallery DL** workflow powered by `gallery-dl`.

### Gallery Hub

The Gallery home is organized into:

| Tab | Purpose |
|---|---|
| **Download** | Download one Gallery URL |
| **Queue** | Process queued and batch URLs sequentially |
| **History** | Reuse and inspect recent Gallery jobs |

Additional Gallery tools include:

- batch URL input
- extractor preflight
- persistent cache
- persistent queue
- persistent history
- optional `cookies.txt`
- config import / export
- raw expert JSON configuration
- compatibility diagnostics

### Organized output

```text
Download/
└── GalleryDL/
    └── Site/
        └── Gallery/
            ├── image_001.jpg
            ├── image_002.jpg
            └── ...
```

### Gallery appearance

Gallery DL follows the active **KirinDL Material theme** for background, surface and text colours.

Optional accents:

- Follow app
- Kirin Cyan
- Ocean Blue
- Emerald
- Violet

This prevents mismatches such as a light Gallery panel with unreadable light text.

---

## ✦ Engine Updates

The APK, yt-dlp, and gallery-dl have separate update lifecycles.

### yt-dlp

KirinDL can update yt-dlp without reinstalling the APK.

Channels:

- **Stable**
- **Nightly**

Upstream sources:

- https://github.com/yt-dlp/yt-dlp
- https://github.com/yt-dlp/yt-dlp-nightly-builds

Recent upstream work continues to include extractor fixes, parser fixes, authentication/token fixes, and site compatibility updates. Selecting **Nightly** lets KirinDL receive newer upstream fixes before the next stable yt-dlp release.

### gallery-dl

Gallery DL follows active development on **Codeberg**:

https://codeberg.org/mikf/gallery-dl

When **Install / Update Engine** is used, KirinDL:

1. resolves the current Codeberg `master` commit
2. validates the commit ID
3. downloads the immutable commit archive
4. extracts only the `gallery_dl` Python package
5. validates package structure
6. stages the update with backup / restore safety
7. records the installed version and source commit

This means newer extractor and compatibility fixes can arrive independently from the KirinDL APK.

### Upstream status checked — 2026-09-01

| Engine | Current upstream state checked |
|---|---|
| yt-dlp Stable | `2026.08.19` |
| yt-dlp Nightly | `2026.08.30.232658` |
| gallery-dl Stable | `1.32.10` |
| gallery-dl Development | KirinDL follows Codeberg `master` |

Notable safe gallery-dl 1.32.10 changes include additional gallery support, GoFile fixes, image-host Referer handling, TikTok HTTP fingerprint improvements, Tumblr inline media improvements, Twitter bookmark-history URL support, dependency updates, and a yt-dlp impersonation fix.

Because KirinDL already updates these engines dynamically, these upstream engine fixes normally **do not require a KirinDL source-code merge**.

---

## ✦ YTDLnis Review

KirinDL also watches useful ideas from the wider Android yt-dlp ecosystem.

Recent YTDLnis development includes:

- BGUtils PO-token service work
- NodeJS integration for BGUtils
- terminal rework
- modular runtime packages for Python / JavaScript runtimes / FFmpeg / aria2

These are **not bundled into KirinDL automatically**. KirinDL recently reached a clean install state with its current signed release, so large runtime additions should be evaluated separately before being merged.

---

## ✦ App Updates

KirinDL checks its own releases here:

https://github.com/Lanzkila/KirinDL/releases

When a newer KirinDL release is available, the app opens the official release page in the browser.

KirinDL does not request Android package-install permission for an in-app self-installer.

Repository:

https://github.com/Lanzkila/KirinDL

Issues:

https://github.com/Lanzkila/KirinDL/issues

---

## ✦ Universal Builds

The **Kirin Build Test** workflow produces:

```text
KirinDL-Universal-Debug.apk
KirinDL-Universal-Release.apk
```

For normal use, choose:

```text
KirinDL-Universal-Release.apk
```

Supported native targets:

```text
arm64-v8a
armeabi-v7a
x86
x86_64
```

The signed Universal Release is verified with Android `apksigner` during GitHub Actions.

---

## ✦ Release Signing

Release signing uses a private KirinDL keystore stored through GitHub Actions secrets.

The private `.jks` is not stored in the public repository.

During CI, the workflow:

1. reconstructs the keystore on the temporary runner
2. validates the configured alias
3. builds the Universal Release
4. verifies the APK signature with `apksigner`
5. removes temporary signing files

Keep the same release key for future versions. Android requires the same signing identity to update an already installed release package.

---

## ✦ Build

### Universal Debug

```bash
./gradlew assembleGenericDebug --stacktrace --no-daemon --no-configuration-cache
```

### Signed Universal Release

```bash
./gradlew assembleGenericRelease --stacktrace --no-daemon --no-configuration-cache
```

The normal GitHub Actions workflow is:

```text
Kirin Build Test
```

---

## ✦ Stack

- Kotlin
- Jetpack Compose
- Material Design 3
- yt-dlp
- youtubedl-android
- gallery-dl
- Chaquopy / Python
- FFmpeg
- aria2
- Room
- Koin
- Coil
- OkHttp

---

## ✦ Project Map

```text
KirinDL
├── Media
│   ├── yt-dlp
│   ├── Stable / Nightly updater
│   ├── FFmpeg
│   └── aria2
│
├── Gallery DL
│   ├── Codeberg engine
│   ├── Download
│   ├── Batch Queue
│   ├── History
│   ├── Cookies
│   ├── Cache
│   └── Expert Config
│
├── Appearance
│   ├── App Material theme
│   ├── Dynamic colour
│   └── Gallery accent variants
│
└── Distribution
    ├── Universal Debug
    ├── Signed Universal Release
    └── GitHub release checks
```

---

## ✦ Notes

KirinDL is an interface around upstream extraction engines. Site support can change without a KirinDL APK update.

Some ordinary sites may require:

- authentication
- cookies from an existing account session
- a newer yt-dlp version
- a newer gallery-dl version
- optional runtime helpers

Only save content that you have permission or the rights to download.

---

## ✦ Open Source Credits

KirinDL is built on the work of multiple open-source projects.

### Seal

Original Android downloader foundation by **JunkFood02**:

https://github.com/JunkFood02/Seal

### SealPlus

This fork lineage also includes work inherited from **SealPlus** by **MaheshTechnicals**:

https://github.com/MaheshTechnicals/Sealplus

### Core upstream

- **yt-dlp** — https://github.com/yt-dlp/yt-dlp
- **youtubedl-android** — https://github.com/yausername/youtubedl-android
- **gallery-dl** — https://codeberg.org/mikf/gallery-dl
- **aria2** — https://github.com/aria2/aria2
- **FFmpeg** — https://ffmpeg.org/

Upstream copyright, license notices and attribution remain with their respective projects and contributors.

---

## ✦ License

KirinDL is distributed under the **GNU General Public License v3.0 (GPL-3.0)** according to the requirements of the inherited codebase.

See [LICENSE](LICENSE).

Do not remove upstream copyright, attribution, or license notices from derived source files.

---

<div align="center">

### KirinDL

**Media • Gallery • Batch**

Built on open source.  
Powered by yt-dlp + gallery-dl.

</div>
