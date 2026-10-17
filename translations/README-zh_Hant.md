<div align="center">

[<img src="../app/src/main/res/drawable/splash_logo.png" width="142" alt="KirinDL logo">](../README.md)

# KirinDL

### 適用於 Android 的影片、音訊與圖庫下載器

[![Release](https://img.shields.io/github/v/release/Lanzkila/KirinDL?style=flat-square&label=KirinDL)](https://github.com/Lanzkila/KirinDL/releases)
[![Downloads](https://img.shields.io/github/downloads/Lanzkila/KirinDL/total?style=flat-square&label=Downloads)](https://github.com/Lanzkila/KirinDL/releases)
[![License](https://img.shields.io/github/license/Lanzkila/KirinDL?style=flat-square)](../LICENSE)

[English](../README.md) · [All languages](README.md) · **繁體中文**

</div>

---

## 主要功能

- 透過 [yt-dlp](https://github.com/yt-dlp/yt-dlp) 下載支援網站的影片與音訊。
- 使用 [gallery-dl](https://github.com/mikf/gallery-dl) 下載圖片及圖庫。
- 支援播放清單、批次下載、任務佇列與下載紀錄。
- 支援字幕、中繼資料、縮圖與 SponsorBlock。
- 支援 FFmpeg 處理、aria2 與自訂 yt-dlp 指令。
- 支援 gallery-dl 擷取器預先檢查與預覽、Cookie 及 JSON 設定。
- Gallery DL 整合 Global Feed 與 Kirin Search。
- 支援背景下載，並顯示影片、音訊、分段、合併和處理階段的進度。

## 下載引擎更新

**yt-dlp**（Stable / Nightly）和來自 Codeberg 的 **gallery-dl** 可獨立更新，不必等待 APK 更新。

- [yt-dlp](https://github.com/yt-dlp/yt-dlp/releases)
- [gallery-dl](https://codeberg.org/mikf/gallery-dl/releases)

## 下載應用程式

請至 [KirinDL Releases](https://github.com/Lanzkila/KirinDL/releases) 下載官方 APK。提供 Universal、arm64-v8a、armeabi-v7a、x86、x86_64 版本及 SHA-256 校驗值。套件名稱：`com.kirin.downloader`。

## 從原始碼建置

從原始碼建置 Debug 或 Release 版本：

```bash
./gradlew assembleGenericDebug --stacktrace --no-daemon --no-configuration-cache
./gradlew assembleGenericRelease --stacktrace --no-daemon --no-configuration-cache
```

## 注意事項

網站支援情況取決於下載引擎。部分網站可能需要登入、Cookie 或更新引擎。請僅下載你有權儲存的內容。

## 專案來源與致謝

KirinDL 的專案沿革為 **Seal → SealPlus → KirinDL**，並使用 yt-dlp、gallery-dl、youtubedl-android、aria2 和 FFmpeg。原專案的著作權、授權與署名資訊均予以保留。

- [Seal](https://github.com/JunkFood02/Seal)
- [SealPlus](https://github.com/MaheshTechnicals/Sealplus)
- [yt-dlp](https://github.com/yt-dlp/yt-dlp)
- [gallery-dl](https://codeberg.org/mikf/gallery-dl)
- [youtubedl-android](https://github.com/yausername/youtubedl-android)
- [aria2](https://github.com/aria2/aria2)
- [FFmpeg](https://ffmpeg.org/)

## 授權條款

KirinDL 依 [GNU GPL-3.0](../LICENSE) 授權散布。

---

[← KirinDL](../README.md) · [Translations](README.md) · [Releases](https://github.com/Lanzkila/KirinDL/releases)
