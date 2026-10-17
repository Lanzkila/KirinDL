<div align="center">

[<img src="../app/src/main/res/drawable/splash_logo.png" width="142" alt="KirinDL logo">](../README.md)

# KirinDL

### 适用于 Android 的视频、音频与图库下载器

[![Release](https://img.shields.io/github/v/release/Lanzkila/KirinDL?style=flat-square&label=KirinDL)](https://github.com/Lanzkila/KirinDL/releases)
[![Downloads](https://img.shields.io/github/downloads/Lanzkila/KirinDL/total?style=flat-square&label=Downloads)](https://github.com/Lanzkila/KirinDL/releases)
[![License](https://img.shields.io/github/license/Lanzkila/KirinDL?style=flat-square)](../LICENSE)

[English](../README.md) · [All languages](README.md) · **简体中文**

</div>

---

## 主要功能

- 通过 [yt-dlp](https://github.com/yt-dlp/yt-dlp) 下载受支持网站的视频和音频。
- 使用 [gallery-dl](https://github.com/mikf/gallery-dl) 下载图片和图库。
- 支持播放列表、批量下载、任务队列和下载历史。
- 支持字幕、元数据、缩略图与 SponsorBlock。
- 支持 FFmpeg 处理、aria2 以及自定义 yt-dlp 命令。
- 支持 gallery-dl 提取器预检查与预览、Cookie 和 JSON 配置。
- Gallery DL 集成 Global Feed 与 Kirin Search。
- 支持后台下载，并分别显示视频、音频、分片、合并及处理阶段的进度。

## 下载引擎更新

**yt-dlp**（Stable / Nightly）和来自 Codeberg 的 **gallery-dl** 可独立更新，无需等待 APK 更新。

- [yt-dlp](https://github.com/yt-dlp/yt-dlp/releases)
- [gallery-dl](https://codeberg.org/mikf/gallery-dl/releases)

## 下载应用

请从 [KirinDL Releases](https://github.com/Lanzkila/KirinDL/releases) 获取官方 APK。提供 Universal、arm64-v8a、armeabi-v7a、x86 和 x86_64 版本及 SHA-256 校验值。应用包名：`com.kirin.downloader`。

## 从源码构建

从源码构建 Debug 或 Release 版本：

```bash
./gradlew assembleGenericDebug --stacktrace --no-daemon --no-configuration-cache
./gradlew assembleGenericRelease --stacktrace --no-daemon --no-configuration-cache
```

## 注意事项

网站支持情况取决于下载引擎。某些网站可能需要登录、Cookie 或更新引擎。请仅下载你有权保存的内容。

## 项目来源与致谢

KirinDL 的项目沿革为 **Seal → SealPlus → KirinDL**，使用 yt-dlp、gallery-dl、youtubedl-android、aria2 和 FFmpeg。原项目的版权、许可证与署名信息继续保留。

- [Seal](https://github.com/JunkFood02/Seal)
- [SealPlus](https://github.com/MaheshTechnicals/Sealplus)
- [yt-dlp](https://github.com/yt-dlp/yt-dlp)
- [gallery-dl](https://codeberg.org/mikf/gallery-dl)
- [youtubedl-android](https://github.com/yausername/youtubedl-android)
- [aria2](https://github.com/aria2/aria2)
- [FFmpeg](https://ffmpeg.org/)

## 许可证

KirinDL 以 [GNU GPL-3.0](../LICENSE) 许可证发布。

---

[← KirinDL](../README.md) · [Translations](README.md) · [Releases](https://github.com/Lanzkila/KirinDL/releases)
