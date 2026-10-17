<div align="center">

[<img src="../app/src/main/res/drawable/splash_logo.png" width="142" alt="KirinDL logo">](../README.md)

# KirinDL

### Android üçün video, audio və qalereya yükləyicisi

[![Release](https://img.shields.io/github/v/release/Lanzkila/KirinDL?style=flat-square&label=KirinDL)](https://github.com/Lanzkila/KirinDL/releases)
[![Downloads](https://img.shields.io/github/downloads/Lanzkila/KirinDL/total?style=flat-square&label=Downloads)](https://github.com/Lanzkila/KirinDL/releases)
[![License](https://img.shields.io/github/license/Lanzkila/KirinDL?style=flat-square)](../LICENSE)

[English](../README.md) · [All languages](README.md) · **Azərbaycanca**

</div>

---

## İmkanlar

- [yt-dlp](https://github.com/yt-dlp/yt-dlp) vasitəsilə dəstəklənən saytlardan video və audio yükləmə.
- [gallery-dl](https://github.com/mikf/gallery-dl) ilə şəkil və qalereyaları yükləmə.
- Pleylistlər, toplu yükləmələr, növbə və tarixçə.
- Altyazılar, metadata, kiçik şəkillər və SponsorBlock dəstəyi.
- FFmpeg emalı, aria2 və fərdi yt-dlp əmrləri.
- gallery-dl üçün ön yoxlama və önizləmə, kukilər və JSON konfiqurasiyası.
- Global Feed və Kirin Search ilə Gallery DL inteqrasiyası.
- Arxa fonda yükləmə və video, audio, hissələr, birləşdirmə və emal mərhələlərinin gedişi.

## Mühərrik yeniləmələri

**yt-dlp** (Stable və Nightly) və Codeberg mənbəli **gallery-dl** APK yeniləmələrindən asılı olmadan yenilənə bilər.

- [yt-dlp](https://github.com/yt-dlp/yt-dlp/releases)
- [gallery-dl](https://codeberg.org/mikf/gallery-dl/releases)

## Yükləmə

Rəsmi APK-ni [KirinDL buraxılışlarından](https://github.com/Lanzkila/KirinDL/releases) əldə edin. Universal, arm64-v8a, armeabi-v7a, x86 və x86_64 variantları və SHA-256 yoxlama cəmləri mövcuddur. Paket: `com.kirin.downloader`.

## Mənbədən qurma

Debug və Release versiyalarını mənbədən qurmaq üçün:

```bash
./gradlew assembleGenericDebug --stacktrace --no-daemon --no-configuration-cache
./gradlew assembleGenericRelease --stacktrace --no-daemon --no-configuration-cache
```

## Qeydlər

Sayt dəstəyi yükləmə mühərriklərindən asılıdır. Bəzi saytlar giriş, kuki və ya yeni mühərrik versiyası tələb edə bilər. Yalnız saxlamağa icazəniz olan məzmunu yükləyin.

## Mənşə və müəlliflər

KirinDL **Seal → SealPlus → KirinDL** xəttindən gəlir və yt-dlp, gallery-dl, youtubedl-android, aria2 və FFmpeg istifadə edir. İlkin müəllif hüquqları və lisenziya bildirişləri qüvvədə qalır.

- [Seal](https://github.com/JunkFood02/Seal)
- [SealPlus](https://github.com/MaheshTechnicals/Sealplus)
- [yt-dlp](https://github.com/yt-dlp/yt-dlp)
- [gallery-dl](https://codeberg.org/mikf/gallery-dl)
- [youtubedl-android](https://github.com/yausername/youtubedl-android)
- [aria2](https://github.com/aria2/aria2)
- [FFmpeg](https://ffmpeg.org/)

## Lisenziya

KirinDL [GNU GPL-3.0](../LICENSE) ilə paylanır.

---

[← KirinDL](../README.md) · [Translations](README.md) · [Releases](https://github.com/Lanzkila/KirinDL/releases)
