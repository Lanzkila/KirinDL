<div align="center">

[<img src="../app/src/main/res/drawable/splash_logo.png" width="142" alt="KirinDL logo">](../README.md)

# KirinDL

### Загрузчик видео, аудио и галерей для Android

[![Release](https://img.shields.io/github/v/release/Lanzkila/KirinDL?style=flat-square&label=KirinDL)](https://github.com/Lanzkila/KirinDL/releases)
[![Downloads](https://img.shields.io/github/downloads/Lanzkila/KirinDL/total?style=flat-square&label=Downloads)](https://github.com/Lanzkila/KirinDL/releases)
[![License](https://img.shields.io/github/license/Lanzkila/KirinDL?style=flat-square)](../LICENSE)

[English](../README.md) · [All languages](README.md) · **Русский**

</div>

---

## Возможности

- Загрузка видео и аудио с сайтов, поддерживаемых [yt-dlp](https://github.com/yt-dlp/yt-dlp).
- Загрузка изображений и галерей через [gallery-dl](https://github.com/mikf/gallery-dl).
- Плейлисты, пакетная загрузка, очередь и история.
- Субтитры, метаданные, миниатюры и SponsorBlock.
- Обработка FFmpeg, aria2 и собственные команды yt-dlp.
- Предпросмотр и предварительная проверка экстракторов gallery-dl, cookies и настройки JSON.
- Интеграция Gallery DL с Global Feed и Kirin Search.
- Фоновая загрузка с отображением этапов видео, аудио, фрагментов, объединения и обработки.

## Обновления движков

**yt-dlp** (Stable и Nightly) и **gallery-dl** из Codeberg обновляются независимо от выпуска APK.

- [yt-dlp](https://github.com/yt-dlp/yt-dlp/releases)
- [gallery-dl](https://codeberg.org/mikf/gallery-dl/releases)

## Скачать приложение

Официальный APK доступен в [релизах KirinDL](https://github.com/Lanzkila/KirinDL/releases). Есть сборки Universal, arm64-v8a, armeabi-v7a, x86 и x86_64, а также контрольные суммы SHA-256. Пакет: `com.kirin.downloader`.

## Сборка из исходников

Для сборки Debug или Release из исходного кода:

```bash
./gradlew assembleGenericDebug --stacktrace --no-daemon --no-configuration-cache
./gradlew assembleGenericRelease --stacktrace --no-daemon --no-configuration-cache
```

## Примечания

Поддержка сайтов зависит от движков загрузки. Некоторым сайтам нужны авторизация, cookies или обновлённый движок. Загружайте только контент, который имеете право сохранять.

## Происхождение и благодарности

KirinDL продолжает цепочку **Seal → SealPlus → KirinDL** и использует yt-dlp, gallery-dl, youtubedl-android, aria2 и FFmpeg. Исходные авторские права, лицензии и указания авторства сохраняются.

- [Seal](https://github.com/JunkFood02/Seal)
- [SealPlus](https://github.com/MaheshTechnicals/Sealplus)
- [yt-dlp](https://github.com/yt-dlp/yt-dlp)
- [gallery-dl](https://codeberg.org/mikf/gallery-dl)
- [youtubedl-android](https://github.com/yausername/youtubedl-android)
- [aria2](https://github.com/aria2/aria2)
- [FFmpeg](https://ffmpeg.org/)

## Лицензия

KirinDL распространяется по лицензии [GNU GPL-3.0](../LICENSE).

---

[← KirinDL](../README.md) · [Translations](README.md) · [Releases](https://github.com/Lanzkila/KirinDL/releases)
