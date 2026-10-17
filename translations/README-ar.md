<div align="center">

[<img src="../app/src/main/res/drawable/splash_logo.png" width="142" alt="KirinDL logo">](../README.md)

# KirinDL

### تنزيل الفيديو والصوت ومعارض الصور لأندرويد

[![Release](https://img.shields.io/github/v/release/Lanzkila/KirinDL?style=flat-square&label=KirinDL)](https://github.com/Lanzkila/KirinDL/releases)
[![Downloads](https://img.shields.io/github/downloads/Lanzkila/KirinDL/total?style=flat-square&label=Downloads)](https://github.com/Lanzkila/KirinDL/releases)
[![License](https://img.shields.io/github/license/Lanzkila/KirinDL?style=flat-square)](../LICENSE)

[English](../README.md) · [All languages](README.md) · **العربية**

</div>

---

## المزايا

- تنزيل الفيديو والصوت من المواقع التي يدعمها [yt-dlp](https://github.com/yt-dlp/yt-dlp).
- تنزيل الصور والمعارض باستخدام [gallery-dl](https://github.com/mikf/gallery-dl).
- دعم قوائم التشغيل والتنزيلات المجمّعة وطابور المهام والسجل.
- دعم الترجمة والبيانات الوصفية والصور المصغّرة وSponsorBlock.
- معالجة الملفات باستخدام FFmpeg وتنزيلات aria2 والأوامر المخصّصة لـ yt-dlp.
- معاينة الروابط والتحقق من مستخرجات gallery-dl ودعم ملفات تعريف الارتباط وإعدادات JSON.
- تكامل Gallery DL مع Global Feed وKirin Search.
- تنزيلات في الخلفية مع عرض تقدّم الفيديو والصوت والأجزاء والدمج والمعالجة.

## تحديث محركات التنزيل

يمكن تحديث **yt-dlp** (Stable وNightly) و**gallery-dl** من مصدر Codeberg بشكل مستقل عن تحديثات APK.

- [yt-dlp](https://github.com/yt-dlp/yt-dlp/releases)
- [gallery-dl](https://codeberg.org/mikf/gallery-dl/releases)

## تنزيل التطبيق

نزّل إصدار APK الرسمي من [صفحة إصدارات KirinDL](https://github.com/Lanzkila/KirinDL/releases). تتوفر نسخة Universal ونسخ arm64-v8a وarmeabi-v7a وx86 وx86_64 مع ملفات التحقق SHA-256. اسم الحزمة: `com.kirin.downloader`.

## البناء من المصدر

لبناء إصدار Debug أو Release من الشيفرة المصدرية:

```bash
./gradlew assembleGenericDebug --stacktrace --no-daemon --no-configuration-cache
./gradlew assembleGenericRelease --stacktrace --no-daemon --no-configuration-cache
```

## ملاحظات

يعتمد دعم المواقع على محركات التنزيل، وقد تتطلب بعض المواقع تسجيل الدخول أو ملفات تعريف الارتباط أو تحديث المحرك. نزّل المحتوى الذي تملك حق حفظه فقط.

## المصدر والاعتمادات

KirinDL مشروع مشتق من **Seal ← SealPlus ← KirinDL**، ويعتمد على yt-dlp وgallery-dl وyoutubedl-android وaria2 وFFmpeg. تبقى حقوق النشر والتراخيص والإشعارات الأصلية محفوظة.

- [Seal](https://github.com/JunkFood02/Seal)
- [SealPlus](https://github.com/MaheshTechnicals/Sealplus)
- [yt-dlp](https://github.com/yt-dlp/yt-dlp)
- [gallery-dl](https://codeberg.org/mikf/gallery-dl)
- [youtubedl-android](https://github.com/yausername/youtubedl-android)
- [aria2](https://github.com/aria2/aria2)
- [FFmpeg](https://ffmpeg.org/)

## الترخيص

يوزَّع KirinDL وفق رخصة [GNU GPL-3.0](../LICENSE).

---

[← KirinDL](../README.md) · [Translations](README.md) · [Releases](https://github.com/Lanzkila/KirinDL/releases)
