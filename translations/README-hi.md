<div align="center">

[<img src="../app/src/main/res/drawable/splash_logo.png" width="142" alt="KirinDL logo">](../README.md)

# KirinDL

### Android के लिए वीडियो, ऑडियो और गैलरी डाउनलोडर

[![Release](https://img.shields.io/github/v/release/Lanzkila/KirinDL?style=flat-square&label=KirinDL)](https://github.com/Lanzkila/KirinDL/releases)
[![Downloads](https://img.shields.io/github/downloads/Lanzkila/KirinDL/total?style=flat-square&label=Downloads)](https://github.com/Lanzkila/KirinDL/releases)
[![License](https://img.shields.io/github/license/Lanzkila/KirinDL?style=flat-square)](../LICENSE)

[English](../README.md) · [All languages](README.md) · **हिन्दी**

</div>

---

## सुविधाएँ

- [yt-dlp](https://github.com/yt-dlp/yt-dlp) समर्थित साइटों से वीडियो और ऑडियो डाउनलोड।
- [gallery-dl](https://github.com/mikf/gallery-dl) द्वारा तस्वीरें और गैलरी डाउनलोड।
- प्लेलिस्ट, बैच डाउनलोड, कतार और डाउनलोड इतिहास।
- सबटाइटल, मेटाडेटा, थंबनेल और SponsorBlock समर्थन।
- FFmpeg प्रोसेसिंग, aria2 तथा कस्टम yt-dlp कमांड।
- gallery-dl प्रीव्यू और एक्सट्रैक्टर जाँच, कुकी तथा JSON सेटिंग्स।
- Global Feed और Kirin Search के साथ Gallery DL एकीकरण।
- बैकग्राउंड डाउनलोड और वीडियो, ऑडियो, फ़्रैगमेंट, मर्ज व प्रोसेसिंग की प्रगति।

## डाउनलोड इंजन अपडेट

**yt-dlp** (Stable और Nightly) तथा Codeberg से **gallery-dl** को APK रिलीज़ से अलग अपडेट किया जा सकता है।

- [yt-dlp](https://github.com/yt-dlp/yt-dlp/releases)
- [gallery-dl](https://codeberg.org/mikf/gallery-dl/releases)

## डाउनलोड करें

आधिकारिक APK [KirinDL रिलीज़](https://github.com/Lanzkila/KirinDL/releases) से प्राप्त करें। Universal, arm64-v8a, armeabi-v7a, x86 और x86_64 संस्करण तथा SHA-256 चेकसम उपलब्ध हैं। पैकेज: `com.kirin.downloader`।

## स्रोत से बिल्ड करें

स्रोत से Debug या Release बिल्ड करने के लिए:

```bash
./gradlew assembleGenericDebug --stacktrace --no-daemon --no-configuration-cache
./gradlew assembleGenericRelease --stacktrace --no-daemon --no-configuration-cache
```

## ध्यान दें

किसी साइट का समर्थन डाउनलोड इंजन पर निर्भर है। कुछ साइटों पर लॉगिन, कुकी या नया इंजन ज़रूरी हो सकता है। केवल वही सामग्री डाउनलोड करें जिसे सहेजने का आपको अधिकार है।

## मूल परियोजनाएँ और श्रेय

KirinDL का विकास क्रम **Seal → SealPlus → KirinDL** है। इसमें yt-dlp, gallery-dl, youtubedl-android, aria2 और FFmpeg का उपयोग होता है। मूल कॉपीराइट और लाइसेंस नोटिस बरकरार हैं।

- [Seal](https://github.com/JunkFood02/Seal)
- [SealPlus](https://github.com/MaheshTechnicals/Sealplus)
- [yt-dlp](https://github.com/yt-dlp/yt-dlp)
- [gallery-dl](https://codeberg.org/mikf/gallery-dl)
- [youtubedl-android](https://github.com/yausername/youtubedl-android)
- [aria2](https://github.com/aria2/aria2)
- [FFmpeg](https://ffmpeg.org/)

## लाइसेंस

KirinDL [GNU GPL-3.0](../LICENSE) के अंतर्गत वितरित है।

---

[← KirinDL](../README.md) · [Translations](README.md) · [Releases](https://github.com/Lanzkila/KirinDL/releases)
