<div align="center">

[<img src="../app/src/main/res/drawable/splash_logo.png" width="142" alt="KirinDL logo">](../README.md)

# KirinDL

### Downloader di video, audio e gallerie per Android

[![Release](https://img.shields.io/github/v/release/Lanzkila/KirinDL?style=flat-square&label=KirinDL)](https://github.com/Lanzkila/KirinDL/releases)
[![Downloads](https://img.shields.io/github/downloads/Lanzkila/KirinDL/total?style=flat-square&label=Downloads)](https://github.com/Lanzkila/KirinDL/releases)
[![License](https://img.shields.io/github/license/Lanzkila/KirinDL?style=flat-square)](../LICENSE)

[English](../README.md) · [All languages](README.md) · **Italiano**

</div>

---

## Funzionalità

- Scarica video e audio dai siti supportati da [yt-dlp](https://github.com/yt-dlp/yt-dlp).
- Scarica immagini e gallerie con [gallery-dl](https://github.com/mikf/gallery-dl).
- Supporta playlist, download multipli, coda e cronologia.
- Supporta sottotitoli, metadati, miniature e SponsorBlock.
- Elaborazione FFmpeg, integrazione aria2 e comandi yt-dlp personalizzati.
- Anteprima e verifica degli estrattori gallery-dl, cookie e configurazione JSON.
- Integrazione di Gallery DL con Global Feed e Kirin Search.
- Download in background con avanzamento per video, audio, frammenti, unione ed elaborazione.

## Aggiornamenti dei motori

**yt-dlp** (Stable e Nightly) e **gallery-dl** da Codeberg possono essere aggiornati indipendentemente dall'APK.

- [yt-dlp](https://github.com/yt-dlp/yt-dlp/releases)
- [gallery-dl](https://codeberg.org/mikf/gallery-dl/releases)

## Scarica l'app

Scarica l'APK ufficiale dalle [release di KirinDL](https://github.com/Lanzkila/KirinDL/releases). Sono disponibili Universal, arm64-v8a, armeabi-v7a, x86 e x86_64 con checksum SHA-256. Pacchetto: `com.kirin.downloader`.

## Compilazione dai sorgenti

Per compilare le versioni Debug o Release dai sorgenti:

```bash
./gradlew assembleGenericDebug --stacktrace --no-daemon --no-configuration-cache
./gradlew assembleGenericRelease --stacktrace --no-daemon --no-configuration-cache
```

## Note

Il supporto ai siti dipende dai motori di download. Alcuni siti richiedono accesso, cookie o una versione più recente del motore. Scarica solo contenuti che hai il diritto di salvare.

## Origini e riconoscimenti

KirinDL discende da **Seal → SealPlus → KirinDL** e utilizza yt-dlp, gallery-dl, youtubedl-android, aria2 e FFmpeg. Restano validi copyright, licenze e attribuzioni originali.

- [Seal](https://github.com/JunkFood02/Seal)
- [SealPlus](https://github.com/MaheshTechnicals/Sealplus)
- [yt-dlp](https://github.com/yt-dlp/yt-dlp)
- [gallery-dl](https://codeberg.org/mikf/gallery-dl)
- [youtubedl-android](https://github.com/yausername/youtubedl-android)
- [aria2](https://github.com/aria2/aria2)
- [FFmpeg](https://ffmpeg.org/)

## Licenza

KirinDL è distribuito sotto la licenza [GNU GPL-3.0](../LICENSE).

---

[← KirinDL](../README.md) · [Translations](README.md) · [Releases](https://github.com/Lanzkila/KirinDL/releases)
