<div align="center">

[<img src="../app/src/main/res/drawable/splash_logo.png" width="142" alt="KirinDL logo">](../README.md)

# KirinDL

### دانلودکنندهٔ ویدئو، صدا و گالری برای اندروید

[![Release](https://img.shields.io/github/v/release/Lanzkila/KirinDL?style=flat-square&label=KirinDL)](https://github.com/Lanzkila/KirinDL/releases)
[![Downloads](https://img.shields.io/github/downloads/Lanzkila/KirinDL/total?style=flat-square&label=Downloads)](https://github.com/Lanzkila/KirinDL/releases)
[![License](https://img.shields.io/github/license/Lanzkila/KirinDL?style=flat-square)](../LICENSE)

[English](../README.md) · [All languages](README.md) · **فارسی**

</div>

---

## ویژگی‌ها

- دریافت ویدئو و صدا از سایت‌های پشتیبانی‌شده توسط [yt-dlp](https://github.com/yt-dlp/yt-dlp).
- دانلود عکس‌ها و گالری‌ها با [gallery-dl](https://github.com/mikf/gallery-dl).
- پشتیبانی از فهرست‌های پخش، دانلود گروهی، صف و تاریخچه.
- پشتیبانی از زیرنویس، فراداده، تصویر بندانگشتی و SponsorBlock.
- پردازش با FFmpeg، دانلود با aria2 و فرمان‌های دلخواه yt-dlp.
- پیش‌نمایش و بررسی استخراج‌گر gallery-dl، کوکی‌ها و تنظیمات JSON.
- یکپارچگی Gallery DL با Global Feed و Kirin Search.
- دانلود در پس‌زمینه با نمایش پیشرفت ویدئو، صدا، قطعه‌ها، ادغام و پردازش.

## به‌روزرسانی موتورهای دانلود

موتور **yt-dlp** (نسخه‌های Stable و Nightly) و **gallery-dl** از منبع Codeberg مستقل از به‌روزرسانی APK قابل ارتقا هستند.

- [yt-dlp](https://github.com/yt-dlp/yt-dlp/releases)
- [gallery-dl](https://codeberg.org/mikf/gallery-dl/releases)

## دریافت برنامه

APK رسمی را از [صفحهٔ انتشار KirinDL](https://github.com/Lanzkila/KirinDL/releases) دریافت کنید. نسخهٔ Universal و معماری‌های arm64-v8a، armeabi-v7a، x86 و x86_64 همراه با SHA-256 در دسترس‌اند. شناسهٔ بسته: `com.kirin.downloader`.

## ساخت از کد منبع

برای ساخت نسخهٔ Debug یا Release از کد منبع:

```bash
./gradlew assembleGenericDebug --stacktrace --no-daemon --no-configuration-cache
./gradlew assembleGenericRelease --stacktrace --no-daemon --no-configuration-cache
```

## نکات

پشتیبانی از سایت‌ها به موتورهای دانلود بستگی دارد. برخی سایت‌ها به ورود به حساب، کوکی یا موتور جدیدتر نیاز دارند. فقط محتوایی را دریافت کنید که اجازهٔ ذخیرهٔ آن را دارید.

## ریشه و اعتبارها

KirinDL ادامهٔ مسیر **Seal → SealPlus → KirinDL** است و از yt-dlp، gallery-dl، youtubedl-android، aria2 و FFmpeg بهره می‌برد. حقوق نشر و اطلاعیه‌های مجوز پروژه‌های اصلی حفظ می‌شوند.

- [Seal](https://github.com/JunkFood02/Seal)
- [SealPlus](https://github.com/MaheshTechnicals/Sealplus)
- [yt-dlp](https://github.com/yt-dlp/yt-dlp)
- [gallery-dl](https://codeberg.org/mikf/gallery-dl)
- [youtubedl-android](https://github.com/yausername/youtubedl-android)
- [aria2](https://github.com/aria2/aria2)
- [FFmpeg](https://ffmpeg.org/)

## مجوز

KirinDL با مجوز [GNU GPL-3.0](../LICENSE) منتشر می‌شود.

---

[← KirinDL](../README.md) · [Translations](README.md) · [Releases](https://github.com/Lanzkila/KirinDL/releases)
