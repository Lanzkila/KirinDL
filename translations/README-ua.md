<div align="center">

[<img src="../app/src/main/res/drawable/splash_logo.png" width="142" alt="KirinDL logo">](../README.md)

# KirinDL

### Завантажувач відео, аудіо та галерей для Android

[![Release](https://img.shields.io/github/v/release/Lanzkila/KirinDL?style=flat-square&label=KirinDL)](https://github.com/Lanzkila/KirinDL/releases)
[![Downloads](https://img.shields.io/github/downloads/Lanzkila/KirinDL/total?style=flat-square&label=Downloads)](https://github.com/Lanzkila/KirinDL/releases)
[![License](https://img.shields.io/github/license/Lanzkila/KirinDL?style=flat-square)](../LICENSE)

[English](../README.md) · [All languages](README.md) · **Українська**

</div>

---

## Можливості

- Завантаження відео й аудіо із сайтів, які підтримує [yt-dlp](https://github.com/yt-dlp/yt-dlp).
- Завантаження зображень і галерей за допомогою [gallery-dl](https://github.com/mikf/gallery-dl).
- Плейлисти, пакетні завантаження, черга та історія.
- Підтримка субтитрів, метаданих, мініатюр і SponsorBlock.
- Обробка через FFmpeg, aria2 та власні команди yt-dlp.
- Попередній перегляд і перевірка екстракторів gallery-dl, cookie та налаштування JSON.
- Інтеграція Gallery DL з Global Feed і Kirin Search.
- Фонове завантаження з індикацією етапів відео, аудіо, фрагментів, об’єднання та обробки.

## Оновлення рушіїв

**yt-dlp** (Stable і Nightly) та **gallery-dl** з Codeberg можна оновлювати незалежно від випусків APK.

- [yt-dlp](https://github.com/yt-dlp/yt-dlp/releases)
- [gallery-dl](https://codeberg.org/mikf/gallery-dl/releases)

## Завантаження застосунку

Офіційний APK доступний у [випусках KirinDL](https://github.com/Lanzkila/KirinDL/releases). Є збірки Universal, arm64-v8a, armeabi-v7a, x86 та x86_64 і контрольні суми SHA-256. Пакет: `com.kirin.downloader`.

## Збирання з вихідного коду

Щоб зібрати Debug або Release з вихідного коду:

```bash
./gradlew assembleGenericDebug --stacktrace --no-daemon --no-configuration-cache
./gradlew assembleGenericRelease --stacktrace --no-daemon --no-configuration-cache
```

## Примітки

Підтримка сайтів залежить від рушіїв завантаження. Деякі сайти потребують входу, cookie чи новішого рушія. Завантажуйте лише вміст, який маєте право зберігати.

## Походження та подяки

KirinDL походить від **Seal → SealPlus → KirinDL** і використовує yt-dlp, gallery-dl, youtubedl-android, aria2 та FFmpeg. Авторські права, ліцензії й атрибуції початкових проєктів збережено.

- [Seal](https://github.com/JunkFood02/Seal)
- [SealPlus](https://github.com/MaheshTechnicals/Sealplus)
- [yt-dlp](https://github.com/yt-dlp/yt-dlp)
- [gallery-dl](https://codeberg.org/mikf/gallery-dl)
- [youtubedl-android](https://github.com/yausername/youtubedl-android)
- [aria2](https://github.com/aria2/aria2)
- [FFmpeg](https://ffmpeg.org/)

## Ліцензія

KirinDL поширюється за ліцензією [GNU GPL-3.0](../LICENSE).

---

[← KirinDL](../README.md) · [Translations](README.md) · [Releases](https://github.com/Lanzkila/KirinDL/releases)
