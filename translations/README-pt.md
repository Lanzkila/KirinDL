<div align="center">

[<img src="../app/src/main/res/drawable/splash_logo.png" width="142" alt="KirinDL logo">](../README.md)

# KirinDL

### Baixador de vídeos, áudio e galerias para Android

[![Release](https://img.shields.io/github/v/release/Lanzkila/KirinDL?style=flat-square&label=KirinDL)](https://github.com/Lanzkila/KirinDL/releases)
[![Downloads](https://img.shields.io/github/downloads/Lanzkila/KirinDL/total?style=flat-square&label=Downloads)](https://github.com/Lanzkila/KirinDL/releases)
[![License](https://img.shields.io/github/license/Lanzkila/KirinDL?style=flat-square)](../LICENSE)

[English](../README.md) · [All languages](README.md) · **Português**

</div>

---

## Recursos

- Baixe vídeos e áudios de sites compatíveis com o [yt-dlp](https://github.com/yt-dlp/yt-dlp).
- Baixe imagens e galerias usando o [gallery-dl](https://github.com/mikf/gallery-dl).
- Suporte a playlists, downloads em lote, fila e histórico.
- Suporte a legendas, metadados, miniaturas e SponsorBlock.
- Processamento com FFmpeg, integração com aria2 e comandos personalizados do yt-dlp.
- Prévia e verificação de extratores do gallery-dl, cookies e configuração JSON.
- Integração do Gallery DL com Global Feed e Kirin Search.
- Downloads em segundo plano com progresso por vídeo, áudio, fragmentos, mesclagem e processamento.

## Atualizações dos mecanismos

O **yt-dlp** (Stable e Nightly) e o **gallery-dl** do Codeberg podem ser atualizados sem aguardar uma nova versão do APK.

- [yt-dlp](https://github.com/yt-dlp/yt-dlp/releases)
- [gallery-dl](https://codeberg.org/mikf/gallery-dl/releases)

## Baixar o aplicativo

Baixe o APK oficial nas [versões do KirinDL](https://github.com/Lanzkila/KirinDL/releases). Há versões Universal, arm64-v8a, armeabi-v7a, x86 e x86_64, além de verificações SHA-256. Pacote: `com.kirin.downloader`.

## Compilar do código-fonte

Para compilar as versões Debug ou Release a partir do código-fonte:

```bash
./gradlew assembleGenericDebug --stacktrace --no-daemon --no-configuration-cache
./gradlew assembleGenericRelease --stacktrace --no-daemon --no-configuration-cache
```

## Observações

A compatibilidade com sites depende dos mecanismos de download. Alguns sites exigem login, cookies ou versões mais recentes. Baixe apenas conteúdo que você tem direito de salvar.

## Origem e créditos

O KirinDL segue a linhagem **Seal → SealPlus → KirinDL** e usa yt-dlp, gallery-dl, youtubedl-android, aria2 e FFmpeg. Direitos autorais, licenças e atribuições originais continuam válidos.

- [Seal](https://github.com/JunkFood02/Seal)
- [SealPlus](https://github.com/MaheshTechnicals/Sealplus)
- [yt-dlp](https://github.com/yt-dlp/yt-dlp)
- [gallery-dl](https://codeberg.org/mikf/gallery-dl)
- [youtubedl-android](https://github.com/yausername/youtubedl-android)
- [aria2](https://github.com/aria2/aria2)
- [FFmpeg](https://ffmpeg.org/)

## Licença

O KirinDL é distribuído sob a licença [GNU GPL-3.0](../LICENSE).

---

[← KirinDL](../README.md) · [Translations](README.md) · [Releases](https://github.com/Lanzkila/KirinDL/releases)
