<div align="center">

[<img src="../app/src/main/res/drawable/splash_logo.png" width="142" alt="KirinDL logo">](../README.md)

# KirinDL

### Преузимач видео-снимака, звука и галерија за Android

[![Release](https://img.shields.io/github/v/release/Lanzkila/KirinDL?style=flat-square&label=KirinDL)](https://github.com/Lanzkila/KirinDL/releases)
[![Downloads](https://img.shields.io/github/downloads/Lanzkila/KirinDL/total?style=flat-square&label=Downloads)](https://github.com/Lanzkila/KirinDL/releases)
[![License](https://img.shields.io/github/license/Lanzkila/KirinDL?style=flat-square)](../LICENSE)

[English](../README.md) · [All languages](README.md) · **Српски**

</div>

---

## Могућности

- Преузимање видеа и звука са сајтова које подржава [yt-dlp](https://github.com/yt-dlp/yt-dlp).
- Преузимање слика и галерија помоћу [gallery-dl](https://github.com/mikf/gallery-dl).
- Плејлисте, групно преузимање, ред чекања и историја.
- Подршка за титлове, метаподатке, сличице и SponsorBlock.
- Обрада помоћу FFmpeg, aria2 и прилагођене yt-dlp команде.
- Преглед и провера gallery-dl екстрактора, колачићи и JSON подешавања.
- Повезивање Gallery DL са Global Feed и Kirin Search.
- Преузимање у позадини и напредак за видео, звук, делове, спајање и обраду.

## Ажурирање механизама

**yt-dlp** (Stable и Nightly) и **gallery-dl** са Codeberg-а могу да се ажурирају независно од APK издања.

- [yt-dlp](https://github.com/yt-dlp/yt-dlp/releases)
- [gallery-dl](https://codeberg.org/mikf/gallery-dl/releases)

## Преузимање апликације

Званични APK је доступан у [KirinDL издањима](https://github.com/Lanzkila/KirinDL/releases). Постоје Universal, arm64-v8a, armeabi-v7a, x86 и x86_64 верзије са SHA-256 контролним сумама. Пакет: `com.kirin.downloader`.

## Изградња из изворног кода

За Debug или Release изградњу из изворног кода:

```bash
./gradlew assembleGenericDebug --stacktrace --no-daemon --no-configuration-cache
./gradlew assembleGenericRelease --stacktrace --no-daemon --no-configuration-cache
```

## Напомене

Подршка за сајтове зависи од механизама за преузимање. Неки сајтови траже пријаву, колачиће или новији механизам. Преузимајте само садржај који имате право да сачувате.

## Порекло и заслуге

KirinDL потиче из **Seal → SealPlus → KirinDL** и користи yt-dlp, gallery-dl, youtubedl-android, aria2 и FFmpeg. Оригинална ауторска права, лиценце и признања остају на снази.

- [Seal](https://github.com/JunkFood02/Seal)
- [SealPlus](https://github.com/MaheshTechnicals/Sealplus)
- [yt-dlp](https://github.com/yt-dlp/yt-dlp)
- [gallery-dl](https://codeberg.org/mikf/gallery-dl)
- [youtubedl-android](https://github.com/yausername/youtubedl-android)
- [aria2](https://github.com/aria2/aria2)
- [FFmpeg](https://ffmpeg.org/)

## Лиценца

KirinDL се дистрибуира под лиценцом [GNU GPL-3.0](../LICENSE).

---

[← KirinDL](../README.md) · [Translations](README.md) · [Releases](https://github.com/Lanzkila/KirinDL/releases)
