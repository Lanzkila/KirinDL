# KirinDownloader Final Rebrand + Gallery DL Patch

## Included

- User-facing Seal Plus / SealPlus branding changed to KirinDownloader where appropriate.
- Main repository, releases, issues, update links and release artifacts point to `Lanzkila/KirinDownloader-Seal`.
- Upstream Seal / SealPlus credits and GPL attribution are intentionally retained.
- Internal compatibility identifiers such as `com.junkfood.seal`, `Theme.SealPlus`, `.SealPlus`, legacy route/class names and authentication key aliases are intentionally not renamed in this patch because changing them can break saved data, resources or app compatibility.
- Added Gallery DL under **More Tools**.
- Added Chaquopy 17.0.0 with Python 3.11 to retain the existing 32-bit and 64-bit ABI coverage.
- Added runtime Gallery DL engine installer from official PyPI metadata, with SHA-256 verification before installation.
- gallery-dl itself is not bundled into the APK; it is installed into app-private storage only when the user presses Install / Update Engine.
- Gallery downloads are exported to `Downloads/KirinDownloader/GalleryDL`.
- GitHub build workflows set up Python 3.11 for Chaquopy builds.

## Static validation completed

- Parsed all Android XML resources successfully.
- Python bridge passes `py_compile`.
- Gallery DL string resource references validated.
- Gallery DL navigation / Koin wiring validated.
- Chaquopy Gradle wiring validated.
- Manifest label confirmed as `@string/app_name`.
- Active project links checked for the KirinDownloader fork.

## Build verification

A full local Gradle compile could not be completed in the patch environment because outbound Java/Gradle dependency downloads are blocked there. Run the existing **Kirin Build Test** GitHub Action after uploading this source. If that workflow reports a compile error, use the complete job log for the next minimal fix.
