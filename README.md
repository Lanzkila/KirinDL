<div align="center">

<img src="app/src/main/res/drawable/splash_logo.png" width="140" alt="KirinDL Logo">

# KirinDL

### Media • Gallery • Batch

A modern Android downloader built around **yt-dlp** and **gallery-dl**, with queueing, history, configurable themes, updateable engines, and a Kirin-focused interface.

[![Android](https://img.shields.io/badge/Android-7.0%2B-3DDC84?logo=android&logoColor=white)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-Android-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![yt-dlp](https://img.shields.io/badge/Engine-yt--dlp-FFCC00)](https://github.com/yt-dlp/yt-dlp)
[![gallery-dl](https://img.shields.io/badge/Gallery-gallery--dl-4AA3DF)](https://codeberg.org/mikf/gallery-dl)
[![License](https://img.shields.io/badge/License-GPL--3.0-blue)](LICENSE)

**Fork lineage:** Seal → SealPlus → KirinDL

</div>

---

## ✦ What is KirinDL?

**KirinDL** is a customized Android downloader focused on keeping media and gallery downloading in one app.

The project combines the mature Android foundation inherited from **Seal / SealPlus** with additional Kirin-specific work such as:

- dedicated Gallery DL hub
- gallery batch queue and history
- Codeberg-based gallery-dl engine updates
- Kirin visual identity
- responsive theme-aware Gallery UI
- Universal APK builds
- stable debug signing
- private release signing through GitHub Actions
- Kirin-specific release and update flow

> The visible application name is **KirinDL**.  
> The Android package ID remains `com.kirin.downloader` so existing signed release updates remain compatible.

---

## ✦ Main Features

### 🎬 Media Downloader

Powered by **yt-dlp** through the Android downloader runtime.

- Video and audio downloads
- Audio extraction
- Format selection and conversion
- Playlist processing
- Subtitle support
- Metadata and thumbnail options
- SponsorBlock support
- Custom yt-dlp commands
- Reusable command templates
- Download history
- Download queue
- aria2 integration
- FFmpeg processing
- Background downloading
- Share-to-KirinDL support
- Configurable download directories

yt-dlp supports a large number of websites. Actual support changes over time as the upstream project updates its extractors.

---

## ✦ Gallery DL

KirinDL includes a separate **Gallery DL** workflow for image galleries and collections supported by `gallery-dl`.

### Gallery Hub

The Gallery interface includes:

- **Download**
- **Queue**
- **History**
- Single URL downloads
- Batch URL input
- Sequential queue processing
- Extractor preflight / compatibility check
- Persistent history
- Persistent cache
- Config import / export
- Optional `cookies.txt`
- Expert `gallery-dl` JSON configuration

Downloaded gallery files are exported into an organized user-visible structure under:

```text
Download/
└── GalleryDL/
    └── Site/
        └── Gallery/
            ├── image_001.jpg
            ├── image_002.jpg
            └── ...
```

### Gallery Appearance

Gallery DL follows the active KirinDL application theme instead of using the Android system theme directly.

Available Gallery accent styles:

| Theme | Behaviour |
|---|---|
| Follow app | Uses the active KirinDL accent |
| Kirin Cyan | Kirin cyan accent |
| Ocean Blue | Blue accent |
| Emerald | Green accent |
| Violet | Violet accent |

Background, surface, and text colours remain tied to the active app theme to preserve readable contrast.

---

## ✦ Engine Updates

KirinDL keeps the downloader engines independent from the APK release cycle.

### yt-dlp

The application can update yt-dlp from inside the app.

Available channels:

- **Stable**
- **Nightly**

The updater continues to use the official yt-dlp Android update mechanism.

Upstream:

- https://github.com/yt-dlp/yt-dlp
- https://github.com/yt-dlp/yt-dlp-nightly-builds

### gallery-dl

Gallery DL uses the active upstream development source from **Codeberg**.

When **Install / Update Engine** is selected, KirinDL:

1. requests metadata for the current `master` branch
2. resolves the exact commit ID
3. downloads that immutable commit archive
4. extracts only the `gallery_dl` Python package
5. validates the expected package structure
6. installs it into app-private storage
7. records the installed version and source commit

Upstream:

- https://codeberg.org/mikf/gallery-dl

This means Gallery DL can be refreshed independently without requiring a new KirinDL APK.

---

## ✦ Updates & Releases

KirinDL's own APK updater is intentionally separate from engine updates.

The app checks the official KirinDL GitHub releases page and opens the release in the browser when a new application version is available.

KirinDL does **not** require Android package-install permission for self-updating.

### Download builds

Releases and development builds are produced from:

**Repository**

https://github.com/Lanzkila/KirinDownloader-Seal

**Releases**

https://github.com/Lanzkila/KirinDownloader-Seal/releases

**Issues**

https://github.com/Lanzkila/KirinDownloader-Seal/issues

---

## ✦ Universal APK

The Kirin build workflow produces Universal APK artifacts containing the supported Android ABIs.

Normal release artifact:

```text
KirinDL-Universal-Release.apk
```

Debug artifact:

```text
KirinDL-Universal-Debug.apk
```

For normal use, prefer the **Universal Release** build.

Supported native targets currently include:

```text
arm64-v8a
armeabi-v7a
x86
x86_64
```

---

## ✦ Release Signing

Release builds are signed using a private KirinDL keystore stored through GitHub Actions secrets.

The private `.jks` file is **not committed to the repository**.

The build runner temporarily reconstructs the keystore, validates it, signs the release, verifies the APK with `apksigner`, and removes temporary signing files afterwards.

Keeping the same release key is required so future release APKs can update previously installed KirinDL releases.

---

## ✦ Technology

KirinDL currently uses:

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

## ✦ Building

Requirements include a compatible Android development environment, JDK 21, and the project Android SDK requirements.

### Universal debug

```bash
./gradlew assembleGenericDebug --no-daemon --no-configuration-cache
```

### Universal release

A signed release requires the configured release keystore:

```bash
./gradlew assembleGenericRelease --no-daemon --no-configuration-cache
```

GitHub Actions automates the project's normal test and signed release build flow.

---

## ✦ Project Structure

```text
KirinDL
├── Media Downloader
│   ├── yt-dlp
│   ├── FFmpeg
│   └── aria2
│
├── Gallery DL
│   ├── Codeberg Engine
│   ├── Download
│   ├── Batch Queue
│   ├── History
│   ├── Cookies
│   └── Expert Config
│
├── Appearance
│   ├── Material Theme
│   ├── Dynamic Colour
│   └── Gallery Theme Variants
│
└── Updates
    ├── KirinDL Releases
    ├── yt-dlp Stable / Nightly
    └── gallery-dl Codeberg Master
```

---

## ✦ Important Notes

KirinDL is a downloader interface. Website availability, extractor behaviour, authentication requirements, and supported formats can change independently of the app.

Some websites may require:

- authentication
- cookies from an existing account session
- updated yt-dlp extractors
- updated gallery-dl extractors
- optional runtime helpers

Only download content that you have permission or the rights to save.

---

## ✦ Open Source & Credits

KirinDL exists because of the open-source projects it builds upon.

### Seal

Original Android downloader project by **JunkFood02**:

https://github.com/JunkFood02/Seal

Seal provides the main Android downloader foundation inherited by this project.

### SealPlus

KirinDownloader-Seal was originally forked from **SealPlus** by **MaheshTechnicals**:

https://github.com/MaheshTechnicals/Sealplus

Improvements inherited from SealPlus remain credited to their original authors and contributors.

### Core upstream projects

- **yt-dlp** — https://github.com/yt-dlp/yt-dlp
- **youtubedl-android** — https://github.com/yausername/youtubedl-android
- **gallery-dl** — https://codeberg.org/mikf/gallery-dl
- **aria2** — https://github.com/aria2/aria2
- **FFmpeg** — https://ffmpeg.org/

All third-party components retain their respective licenses, copyright notices, and attribution requirements.

---

## ✦ License

KirinDL is distributed under the **GNU General Public License v3.0 (GPL-3.0)** according to the licensing requirements of the inherited codebase.

See:

[LICENSE](LICENSE)

Do not remove copyright, attribution, or license notices from code derived from upstream projects.

---

<div align="center">

### KirinDL

**Media • Gallery • Batch**

Built on open source.  
Powered by yt-dlp + gallery-dl.

</div>
