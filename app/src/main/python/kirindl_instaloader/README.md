# KirinDL Instaloader Integration

This directory is reserved for KirinDL-owned Python bridge code that integrates the third-party Instaloader library.

Keep this integration isolated from the existing `gallery_bridge.py`. The Instaloader package itself remains a Chaquopy dependency; this folder is for KirinDL's adapter and preview logic, not a copied third-party package.
