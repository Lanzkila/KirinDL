<div align="center">

[<img src="../app/src/main/res/drawable/splash_logo.png" width="142" alt="KirinDL logo">](../README.md)

# KirinDL

### Pengunduh video, audio, dan galeri untuk Android

[![Release](https://img.shields.io/github/v/release/Lanzkila/KirinDL?style=flat-square&label=KirinDL)](https://github.com/Lanzkila/KirinDL/releases)
[![Downloads](https://img.shields.io/github/downloads/Lanzkila/KirinDL/total?style=flat-square&label=Downloads)](https://github.com/Lanzkila/KirinDL/releases)
[![License](https://img.shields.io/github/license/Lanzkila/KirinDL?style=flat-square)](../LICENSE)

[English](../README.md) · [All languages](README.md) · **Bahasa Indonesia**

</div>

---

## Fitur

- Unduh video dan audio dari situs yang didukung [yt-dlp](https://github.com/yt-dlp/yt-dlp).
- Unduh gambar dan galeri dengan [gallery-dl](https://github.com/mikf/gallery-dl).
- Mendukung playlist, unduhan massal, antrean, dan riwayat.
- Mendukung subtitle, metadata, thumbnail, dan SponsorBlock.
- Pemrosesan FFmpeg, integrasi aria2, serta perintah yt-dlp khusus.
- Pratinjau dan pemeriksaan extractor gallery-dl, cookie, serta konfigurasi JSON.
- Integrasi Gallery DL dengan Global Feed dan Kirin Search.
- Unduhan latar belakang dengan progres tahap video, audio, fragmen, penggabungan, dan pemrosesan.

## Pembaruan mesin unduhan

**yt-dlp** (Stable dan Nightly) serta **gallery-dl** dari Codeberg dapat diperbarui tanpa menunggu pembaruan APK.

- [yt-dlp](https://github.com/yt-dlp/yt-dlp/releases)
- [gallery-dl](https://codeberg.org/mikf/gallery-dl/releases)

## Unduh aplikasi

Unduh APK resmi di [Rilis KirinDL](https://github.com/Lanzkila/KirinDL/releases). Tersedia versi Universal, arm64-v8a, armeabi-v7a, x86, dan x86_64 beserta checksum SHA-256. Nama paket: `com.kirin.downloader`.

## Build dari sumber

Untuk membuat build Debug atau Release dari sumber:

```bash
./gradlew assembleGenericDebug --stacktrace --no-daemon --no-configuration-cache
./gradlew assembleGenericRelease --stacktrace --no-daemon --no-configuration-cache
```

## Catatan

Dukungan situs bergantung pada mesin pengunduh. Beberapa situs memerlukan login, cookie, atau mesin yang lebih baru. Unduh hanya konten yang boleh Anda simpan.

## Asal proyek dan kredit

KirinDL berasal dari **Seal → SealPlus → KirinDL** dan menggunakan yt-dlp, gallery-dl, youtubedl-android, aria2, serta FFmpeg. Hak cipta, lisensi, dan atribusi proyek asal tetap berlaku.

- [Seal](https://github.com/JunkFood02/Seal)
- [SealPlus](https://github.com/MaheshTechnicals/Sealplus)
- [yt-dlp](https://github.com/yt-dlp/yt-dlp)
- [gallery-dl](https://codeberg.org/mikf/gallery-dl)
- [youtubedl-android](https://github.com/yausername/youtubedl-android)
- [aria2](https://github.com/aria2/aria2)
- [FFmpeg](https://ffmpeg.org/)

## Lisensi

KirinDL didistribusikan dengan lisensi [GNU GPL-3.0](../LICENSE).

---

[← KirinDL](../README.md) · [Translations](README.md) · [Releases](https://github.com/Lanzkila/KirinDL/releases)
