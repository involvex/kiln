# Changelog

All notable changes to Kiln are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- GitHub browse in Discover (M12): F-Droid/GitHub tabs; owner/repo lookup; saved sources; install queue
- App Discovery (M11): Sources → Discover; full F-Droid catalog sync with categories; search + install queue
- Release polish (M10): About section, privacy policy, F-Droid fastlane metadata, tagged release workflow

## [0.1.0] — 2026-11-08

### Added

- Installed apps list, detail, APK export (`.apk` / `.apks` / `.xapk`)
- Sources: F-Droid/Izzy defaults, GitHub Releases, auto-match, Obtainium import
- Installer (system PackageInstaller + optional Shizuku silent install)
- Privilege / Shizuku setup assistant
- Storage analyzer and cache clear
- Debloat (denylist, snapshot, disable/restore)
- APK backup / restore (`.kilnbackup.zip`)
- Background update checks (WorkManager) with cooldown / major-bump gate
- Settings: theme, language, Wi‑Fi-only updates, installer preference, debug log
- Optional Termux `RUN_COMMAND` integration (feature-flagged)

### Security

- No telemetry; HTTPS-only network; signature checks on updates; tokens not logged

[Unreleased]: https://github.com/involvex/kiln/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/involvex/kiln/releases/tag/v0.1.0
