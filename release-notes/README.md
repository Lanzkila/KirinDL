# KirinDL Release Notes

Each KirinDL version has its own GitHub release note file.

Examples:

```text
3.1.0 -> release-notes/v3.1.0.md
3.1.1 -> release-notes/v3.1.1.md
3.2.0 -> release-notes/v3.2.0.md
```

The `KirinDL Release` workflow reads the current app version from `buildSrc/src/main/kotlin/Version.kt` and automatically selects the matching file.

If `publish_release = true` and that file is missing, empty, or has the wrong version heading, publishing stops.
