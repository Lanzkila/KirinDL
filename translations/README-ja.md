<div align="center">

[<img src="../app/src/main/res/drawable/splash_logo.png" width="142" alt="KirinDL logo">](../README.md)

# KirinDL

### Android 向け動画・音声・画像ギャラリーダウンローダー

[![Release](https://img.shields.io/github/v/release/Lanzkila/KirinDL?style=flat-square&label=KirinDL)](https://github.com/Lanzkila/KirinDL/releases)
[![Downloads](https://img.shields.io/github/downloads/Lanzkila/KirinDL/total?style=flat-square&label=Downloads)](https://github.com/Lanzkila/KirinDL/releases)
[![License](https://img.shields.io/github/license/Lanzkila/KirinDL?style=flat-square)](../LICENSE)

[English](../README.md) · [All languages](README.md) · **日本語**

</div>

---

## 主な機能

- [yt-dlp](https://github.com/yt-dlp/yt-dlp) 対応サイトから動画と音声をダウンロード。
- [gallery-dl](https://github.com/mikf/gallery-dl) による画像・ギャラリーのダウンロード。
- プレイリスト、一括ダウンロード、キュー、履歴に対応。
- 字幕、メタデータ、サムネイル、SponsorBlock に対応。
- FFmpeg 処理、aria2、カスタム yt-dlp コマンドに対応。
- gallery-dl の抽出器の事前確認とプレビュー、Cookie、JSON 設定に対応。
- Gallery DL と Global Feed、Kirin Search の連携。
- バックグラウンドダウンロードと動画・音声・分割ファイル・結合・処理の進行状況表示。

## ダウンロードエンジンの更新

**yt-dlp**（Stable / Nightly）と Codeberg の **gallery-dl** は、APK の更新を待たずに個別に更新できます。

- [yt-dlp](https://github.com/yt-dlp/yt-dlp/releases)
- [gallery-dl](https://codeberg.org/mikf/gallery-dl/releases)

## ダウンロード

公式 APK は [KirinDL Releases](https://github.com/Lanzkila/KirinDL/releases) から入手してください。Universal、arm64-v8a、armeabi-v7a、x86、x86_64 と SHA-256 チェックサムを提供しています。パッケージ名: `com.kirin.downloader`。

## ソースからビルド

ソースから Debug または Release をビルドするには:

```bash
./gradlew assembleGenericDebug --stacktrace --no-daemon --no-configuration-cache
./gradlew assembleGenericRelease --stacktrace --no-daemon --no-configuration-cache
```

## 注意事項

対応サイトはダウンロードエンジンの仕様に依存します。ログイン、Cookie、エンジン更新が必要なサイトもあります。保存する権利のあるコンテンツのみダウンロードしてください。

## プロジェクトの系譜とクレジット

KirinDL の系譜は **Seal → SealPlus → KirinDL** です。yt-dlp、gallery-dl、youtubedl-android、aria2、FFmpeg を使用しています。元の著作権・ライセンス・クレジット表記は引き続き適用されます。

- [Seal](https://github.com/JunkFood02/Seal)
- [SealPlus](https://github.com/MaheshTechnicals/Sealplus)
- [yt-dlp](https://github.com/yt-dlp/yt-dlp)
- [gallery-dl](https://codeberg.org/mikf/gallery-dl)
- [youtubedl-android](https://github.com/yausername/youtubedl-android)
- [aria2](https://github.com/aria2/aria2)
- [FFmpeg](https://ffmpeg.org/)

## ライセンス

KirinDL は [GNU GPL-3.0](../LICENSE) で配布されています。

---

[← KirinDL](../README.md) · [Translations](README.md) · [Releases](https://github.com/Lanzkila/KirinDL/releases)
