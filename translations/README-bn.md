<div align="center">

[<img src="../app/src/main/res/drawable/splash_logo.png" width="142" alt="KirinDL logo">](../README.md)

# KirinDL

### অ্যান্ড্রয়েডের জন্য ভিডিও, অডিও ও গ্যালারি ডাউনলোডার

[![Release](https://img.shields.io/github/v/release/Lanzkila/KirinDL?style=flat-square&label=KirinDL)](https://github.com/Lanzkila/KirinDL/releases)
[![Downloads](https://img.shields.io/github/downloads/Lanzkila/KirinDL/total?style=flat-square&label=Downloads)](https://github.com/Lanzkila/KirinDL/releases)
[![License](https://img.shields.io/github/license/Lanzkila/KirinDL?style=flat-square)](../LICENSE)

[English](../README.md) · [All languages](README.md) · **বাংলা**

</div>

---

## বৈশিষ্ট্য

- [yt-dlp](https://github.com/yt-dlp/yt-dlp) সমর্থিত ওয়েবসাইট থেকে ভিডিও ও অডিও ডাউনলোড।
- [gallery-dl](https://github.com/mikf/gallery-dl) দিয়ে ছবি ও গ্যালারি ডাউনলোড।
- প্লেলিস্ট, একাধিক ডাউনলোড, কিউ ও ডাউনলোড ইতিহাস।
- সাবটাইটেল, মেটাডেটা, থাম্বনেইল ও SponsorBlock সমর্থন।
- FFmpeg প্রসেসিং, aria2 এবং নিজের মতো yt-dlp কমান্ড।
- gallery-dl প্রিভিউ ও এক্সট্র্যাক্টর যাচাই, কুকি ও JSON কনফিগারেশন।
- Global Feed ও Kirin Search-এর সঙ্গে Gallery DL সংযোগ।
- ব্যাকগ্রাউন্ড ডাউনলোড এবং ভিডিও, অডিও, ফ্র্যাগমেন্ট, মার্জ ও প্রসেসিংয়ের অগ্রগতি।

## ইঞ্জিন আপডেট

**yt-dlp** (Stable ও Nightly) এবং Codeberg থেকে **gallery-dl**-এর আপডেট APK আপডেট ছাড়াই ইনস্টল করা যায়।

- [yt-dlp](https://github.com/yt-dlp/yt-dlp/releases)
- [gallery-dl](https://codeberg.org/mikf/gallery-dl/releases)

## ডাউনলোড

[KirinDL Releases](https://github.com/Lanzkila/KirinDL/releases) থেকে অফিসিয়াল APK নিন। Universal, arm64-v8a, armeabi-v7a, x86 ও x86_64 সংস্করণ এবং SHA-256 চেকসাম পাওয়া যায়। প্যাকেজ: `com.kirin.downloader`।

## সোর্স থেকে বিল্ড

সোর্স থেকে Debug বা Release বিল্ড করতে:

```bash
./gradlew assembleGenericDebug --stacktrace --no-daemon --no-configuration-cache
./gradlew assembleGenericRelease --stacktrace --no-daemon --no-configuration-cache
```

## গুরুত্বপূর্ণ তথ্য

কোন সাইট কাজ করবে তা ডাউনলোড ইঞ্জিনের ওপর নির্ভর করে। কিছু সাইটে লগইন, কুকি বা নতুন ইঞ্জিন লাগতে পারে। শুধু অনুমতি থাকা কনটেন্ট ডাউনলোড করুন।

## উৎস ও কৃতজ্ঞতা

KirinDL-এর উৎস **Seal → SealPlus → KirinDL**। এটি yt-dlp, gallery-dl, youtubedl-android, aria2 ও FFmpeg ব্যবহার করে। মূল কপিরাইট ও লাইসেন্স নোটিশ বহাল আছে।

- [Seal](https://github.com/JunkFood02/Seal)
- [SealPlus](https://github.com/MaheshTechnicals/Sealplus)
- [yt-dlp](https://github.com/yt-dlp/yt-dlp)
- [gallery-dl](https://codeberg.org/mikf/gallery-dl)
- [youtubedl-android](https://github.com/yausername/youtubedl-android)
- [aria2](https://github.com/aria2/aria2)
- [FFmpeg](https://ffmpeg.org/)

## লাইসেন্স

KirinDL [GNU GPL-3.0](../LICENSE) লাইসেন্সের অধীনে বিতরণ করা হয়।

---

[← KirinDL](../README.md) · [Translations](README.md) · [Releases](https://github.com/Lanzkila/KirinDL/releases)
