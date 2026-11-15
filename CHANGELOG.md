# Changelog

All notable changes to Kiln are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Settings → Background controls: HyperOS Autostart (AppOps) + AOSP battery Unrestricted/Optimized/Restricted via Shizuku (#53)
- Catalog / App detail: Open Exodus report (browser); optional self-hosted Exodus API in Settings for tracker name chips (ODbL attribution) (#52)

## [0.2.3] — 2026-11-15

### Added

- Apps: after Check updates / pull-to-refresh, View updates toggles the Updates filter; Update all confirms then queues signature-safe installs
- Update check: per-repo progress + cancel; parallel F-Droid sync; faster mirror failover after first failure (#50)
- Manage → APK tools: resign SAF APK with user PKCS12/keystore (apksig); export split `.apk` members from `.apks`/`.xapk` (no universal merge) (#49)
- Per-repo HTTPS mirrors + Settings Network HTTP/SOCKS/Orbot proxy; Kiln-Config schema v2 (#48)
- Manage → APK tools: inspect / signature compare (#47)
- Storage phase 2: size bars, APK leftovers, orphaned data/obb (#44)
- Permission audit; widgets / QS; activity log; config export; scheduled maintenance; automation intents (#36–#41)
- Watchlist, release notes, Play deep-link, GitHub channels, F-Droid anti-feature chips (#33–#35)
- Auto-match UX, Discover/Trending polish, Ocean/Dynamic Color themes

### Fixed

- Apps badge refresh no longer hits GitHub on every open/refresh (live probes only on Check updates / Sync all) (#50)
- Update-check hang: Room-only refresh, fail-fast mirrors, bounded parallel repo sync (#50)

### Changed

- Docs/About/store copy: clarify Kiln vs macOS Kiln emulator (#46)
- Library pull-to-refresh runs a full update check; Discover refresh is a compact TextButton

## [0.2.2] — 2026-11-09

### Fixed

- HyperOS continuous flicker: Compose applied default `AppearancePrefs()` (SYSTEM) before DataStore loaded, fighting the real language (`en` ↔ `de-DE`, several MainActivity relaunches per second). Locale apply is Application-only now.

## [0.2.1] — 2026-11-09

### Fixed

- HyperOS continuous black/default flicker: locale apply treated `en-US` / `de-DE` as different from `en` / `de` and recreated MainActivity in a loop; follow-system now uses an empty LocaleList; Material You dynamic color disabled at the root so wallpaper shifts cannot thrash the window

## [0.2.0] — 2026-11-09

### Added

- M13: AppCompat locales (`localeConfig`), early language apply, Termux in-app `RUN_COMMAND` request
- M14: 4-tab shell (Apps / Sources / Manage / Settings); Storage + Backup under Manage
- M15: Apps Library | Discover; catalog icons + detail (screenshots when present); Outfit font preference
- M16: Trending topic shelves (category / keyword heuristics — no fake download charts)
- GitHub Pages docs site (Actions → `involvex.github.io/kiln`)
- Supporting / funding docs (`docs/SUPPORTING.md`, Sponsor links from FUNDING.yml)
- Settings → About: Support this project link (DE/EN)
- M17: Termux package detection — finds renamed forks (e.g. `com.involvex.termux_app`) instead of only matching the official `com.termux` id; App Info shortcut for forks that declare their own `RUN_COMMAND` permission

### Fixed

- Check-now expedited WorkManager crash (network-only constraints for immediate requests)
- Release workflow: do not reference `secrets` in `if:` (GitHub Actions validation)
- CI: explicit least-privilege `permissions` for CodeQL
- First-time GitHub Discover install when package is not already installed
- Termux smoke test silently failing on renamed Termux forks: the package-id field defaulted to `com.termux`, so the fork was never detected and the RUN_COMMAND intent was never built
- Termux “No install found” stayed visible after a successful checklist: detection stopped at the first `*termux*` package (for example Termux:Boot) and the failure flag was never cleared
- HyperOS full-screen black/white flash: DayNight window theme fought the Compose theme, and locale apply compared list identity instead of language tags
- Apps Library showed a second title bar under Library / Discover / Trending, leaving a large empty gap above the list

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

[Unreleased]: https://github.com/involvex/kiln/compare/v0.2.3...HEAD
[0.2.3]: https://github.com/involvex/kiln/compare/v0.2.0...v0.2.3
[0.2.2]: https://github.com/involvex/kiln/compare/v0.2.0...v0.2.3
[0.2.1]: https://github.com/involvex/kiln/compare/v0.2.0...v0.2.3
[0.2.0]: https://github.com/involvex/kiln/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/involvex/kiln/releases/tag/v0.1.0
