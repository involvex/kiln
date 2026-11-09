# Changelog

All notable changes to Kiln are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- M13: AppCompat locales (`localeConfig`), early language apply, Termux in-app `RUN_COMMAND` request
- M14: 4-tab shell (Apps / Sources / Manage / Settings); Storage + Backup under Manage
- M15: Apps Library | Discover; catalog icons + detail (screenshots when present); Outfit font preference
- M16: Trending topic shelves (category / keyword heuristics — no fake download charts)
- GitHub Pages docs site (Actions → `involvex.github.io/kiln`)
- Supporting / funding docs (`docs/SUPPORTING.md`, Sponsor links from FUNDING.yml)
- Settings → About: Support this project link (DE/EN)

### Fixed

- Check-now expedited WorkManager crash (network-only constraints for immediate requests)
- Release workflow: do not reference `secrets` in `if:` (GitHub Actions validation)
- CI: explicit least-privilege `permissions` for CodeQL
- First-time GitHub Discover install when package is not already installed

### Changed

- Discover entry moved from Sources into Apps shell; Sources keeps repos / PAT / Obtainium
- Removed Bing Rewards URL from `.github/FUNDING.yml` Sponsor menu

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
- App Discovery: Sources → Discover; F-Droid catalog sync with categories; search + install queue
- GitHub browse in Discover: owner/repo lookup, saved sources, install queue
- Release polish: About, privacy policy, F-Droid fastlane metadata, tagged release workflow

### Security

- No telemetry; HTTPS-only network; signature checks on updates; tokens not logged

[Unreleased]: https://github.com/involvex/kiln/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/involvex/kiln/releases/tag/v0.1.0
