<div align="center">

[<img src="../app/src/main/res/drawable/splash_logo.png" width="142" alt="KirinDL logo">](../README.md)

# KirinDL

### แอปดาวน์โหลดวิดีโอ เสียง และแกลเลอรีสำหรับ Android

[![Release](https://img.shields.io/github/v/release/Lanzkila/KirinDL?style=flat-square&label=KirinDL)](https://github.com/Lanzkila/KirinDL/releases)
[![Downloads](https://img.shields.io/github/downloads/Lanzkila/KirinDL/total?style=flat-square&label=Downloads)](https://github.com/Lanzkila/KirinDL/releases)
[![License](https://img.shields.io/github/license/Lanzkila/KirinDL?style=flat-square)](../LICENSE)

[English](../README.md) · [All languages](README.md) · **ภาษาไทย**

</div>

---

## คุณสมบัติ

- ดาวน์โหลดวิดีโอและเสียงจากเว็บไซต์ที่ [yt-dlp](https://github.com/yt-dlp/yt-dlp) รองรับ
- ดาวน์โหลดรูปภาพและแกลเลอรีด้วย [gallery-dl](https://github.com/mikf/gallery-dl)
- รองรับเพลย์ลิสต์ การดาวน์โหลดหลายรายการ คิว และประวัติ
- รองรับคำบรรยาย ข้อมูลเมตา ภาพปก และ SponsorBlock
- ประมวลผลด้วย FFmpeg ใช้ aria2 และคำสั่ง yt-dlp แบบกำหนดเอง
- ดูตัวอย่างและตรวจสอบตัวแยกข้อมูล gallery-dl รองรับคุกกี้และการตั้งค่า JSON
- เชื่อมต่อ Gallery DL กับ Global Feed และ Kirin Search
- ดาวน์โหลดเบื้องหลังพร้อมแสดงความคืบหน้าของวิดีโอ เสียง ชิ้นส่วน การรวมไฟล์ และการประมวลผล

## การอัปเดตเอนจิน

สามารถอัปเดต **yt-dlp** (Stable และ Nightly) และ **gallery-dl** จาก Codeberg แยกจากการอัปเดต APK ได้

- [yt-dlp](https://github.com/yt-dlp/yt-dlp/releases)
- [gallery-dl](https://codeberg.org/mikf/gallery-dl/releases)

## ดาวน์โหลดแอป

ดาวน์โหลด APK อย่างเป็นทางการจาก [KirinDL Releases](https://github.com/Lanzkila/KirinDL/releases) มีรุ่น Universal, arm64-v8a, armeabi-v7a, x86 และ x86_64 พร้อมค่า SHA-256 ชื่อแพ็กเกจ: `com.kirin.downloader`

## สร้างจากซอร์สโค้ด

สำหรับสร้าง Debug หรือ Release จากซอร์สโค้ด:

```bash
./gradlew assembleGenericDebug --stacktrace --no-daemon --no-configuration-cache
./gradlew assembleGenericRelease --stacktrace --no-daemon --no-configuration-cache
```

## หมายเหตุ

การรองรับเว็บไซต์ขึ้นอยู่กับเอนจินดาวน์โหลด บางเว็บไซต์ต้องเข้าสู่ระบบ ใช้คุกกี้ หรืออัปเดตเอนจิน ดาวน์โหลดเฉพาะเนื้อหาที่คุณมีสิทธิ์บันทึกเท่านั้น

## ที่มาและเครดิต

KirinDL สืบทอดจาก **Seal → SealPlus → KirinDL** และใช้ yt-dlp, gallery-dl, youtubedl-android, aria2 และ FFmpeg โดยยังคงประกาศลิขสิทธิ์และสัญญาอนุญาตต้นฉบับ

- [Seal](https://github.com/JunkFood02/Seal)
- [SealPlus](https://github.com/MaheshTechnicals/Sealplus)
- [yt-dlp](https://github.com/yt-dlp/yt-dlp)
- [gallery-dl](https://codeberg.org/mikf/gallery-dl)
- [youtubedl-android](https://github.com/yausername/youtubedl-android)
- [aria2](https://github.com/aria2/aria2)
- [FFmpeg](https://ffmpeg.org/)

## สัญญาอนุญาต

KirinDL เผยแพร่ภายใต้ [GNU GPL-3.0](../LICENSE)

---

[← KirinDL](../README.md) · [Translations](README.md) · [Releases](https://github.com/Lanzkila/KirinDL/releases)
