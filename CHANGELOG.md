# Changelog

All notable changes to Kiln are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- **M63** Curated debloat catalog pipeline (`DebloatDbParser` + tests + source/version UI); shipped `debloat-db.json` still has `packages: []` (no invented ratings)
- **M62** Storage squarified treemap of largest consumers (apps + scanned leftovers/orphans/large files); replaces size bars
- **M61** GitLab.com + Codeberg release sources (Discover Releases tab + Sources share); optional forge tokens under Settings → Updates; Nightly stays GitHub-only

## [0.2.14] — 2026-11-27

### Added

- **M60** Device diagnostics (Settings → Tools): Shizuku pretty cards for battery, network usage, and CPU top (no raw dumps, no Root)

### Fixed

- Explore / Sources GitHub add: when a repo only publishes prereleases with APK assets, fall back to the Pre-release channel and persist it (STABLE still preferred when a stable APK exists; App Detail chips remain the manual override)
- HyperOS netstats checkin / UID map parsing for Device diagnostics

### Changed

- Version **0.2.14** (versionCode 17)

## [0.2.13] — 2026-11-26

### Fixed

- Census / dual-user honesty: remove stale “installs only for user 0” copy; best-effort Dual Apps wording on install, component, freeze, debloat, and backup pickers (EN+DE)

### Changed

- Version **0.2.13** (versionCode 16)

## [0.2.12] — 2026-11-26

### Added

- M58 best-effort dual-user backup/restore targeting (census picker on confirm; install queue `targetUserId`; metadata honesty note; Android/data stays current-user paths)

### Changed

- Version **0.2.12** (versionCode 15)

## [0.2.11] — 2026-11-26

### Added

- M57 best-effort dual-user debloat batch (disable / enable / uninstall-for-user / install-existing; census picker on confirm)

### Changed

- Version **0.2.11** (versionCode 14)

## [0.2.10] — 2026-11-26

### Added

- M56 best-effort dual-user component enable/disable and freeze/unfreeze (census picker)

### Changed

- Version **0.2.10** (versionCode 13)

## [0.2.9] — 2026-11-24

### Added

- M55 best-effort dual-user install and uninstall

### Fixed

- lower AppDetailViewModel detekt complexity for M55

### Changed

- Version **0.2.9** (versionCode 12)

## [0.2.8] — 2026-11-24

### Fixed

- Release APKs omit Google Play dependency-metadata from the signing block (`dependenciesInfo.includeInApk/includeInBundle = false`) for F-Droid / IzzyOnDroid scanners

### Changed

- Store docs: IzzyOnDroid inclusion declined under their AI policy ([repodata#948](https://codeberg.org/IzzyOnDroid/repodata/issues/948)); F-Droid remains the free listing path

### Fixed

- omit Google dependency metadata from release APKs
- bump compileSdk minor to 37.2 for Compose BOM
- use APK package name for GitHub Discover installs

- Version **0.2.8** (versionCode 11)

## [0.2.7] — 2026-11-22

### Added

- App Detail: enable / disable package components via Shizuku (`pm enable` / `pm disable`); denylist; Tier NONE read-only; non-exported listed read-only (M51)
- Settings → Tools: in-app process list with force-stop confirm (same gates as Processes widget) (M52)
- Settings → Tools: read-only Android user census from `pm list users` (M53)

### Changed

- Version **0.2.7** (versionCode 10)

## [0.2.6] — 2026-11-22

### Added

- Storage: large files (≥ 50 MB) and same-size SHA-256 duplicates in the picked SAF folder; soft-delete into `.kiln-trash` with restore / permanent purge; 14-day maintenance prune (M48)
- Backup restore installs every APK in the archive (base + splits) via the existing multi-session installer; aborts if metadata claims splits but only one APK is present (M49)
- App Detail: exported services, receivers, and providers (read-only) (M50)
- Sources hub (Repositories, Watchlist, GitHub stars, Import, Match) like Settings
- Starred-repo search filter with clear; Apps Library search clear (X)
- `scripts/release.ps1` for patch-default version bump, changelog, tag, and push
- Discover vertical tiles with catalog icons; Install picks version + ABI (F-Droid and GitHub)

### Fixed

- App Detail “Update available” banner no longer crushed beside long version names

### Changed

- Version **0.2.6** (versionCode 9)

## [0.2.5] — 2026-11-17

### Added

- Home widgets: Cache (trim with confirm) and Processes (mem/CPU + force-stop via Shizuku) (M46, #81)
- Discover home shelves + GitHub starred → Sources; app activities viewer (M41–M43)
- Backup phase 2: SAF tree + keep-N + Shizuku Android/data+obb + optional AES-GCM (M40, #69)

### Fixed

- Processes widget ANR/freeze: early-exit meminfo snapshot + async refresh (no dumpsys on Glance/receiver bind path) (#85)
- Processes widget empty on HyperOS: parse `NNNK:` / RSS+PSS `dumpsys meminfo` formats
- Hilt ViewModel crashes (Storage, Background controls, …) from incomplete/stale KSP maps; `ksp.incremental=false` + `verifyHiltViewModelMapDebug` (M45, #79)
- Package list icons: decode to request size and mark Coil `MEMORY` (avoid full mipmap bitmaps)

### Changed

- GitHub Release workflow: require upload-keystore secrets, `apksigner verify`, attach `kiln-<version>.apk`
- Home widgets: Material glass chrome; Updates widget drops trim (Cache widget owns it)
- App Detail / Discover denser layouts; removed Exodus Privacy (M44–M45)
- Version **0.2.5** (versionCode 8)

## [0.2.4] — 2026-11-16

### Added

- Manage → APK tools **Install**: `.apks` / `.xapk` / `.apkm` (and single `.apk`) via PackageInstaller multi-session; device-aware split filter; Share/VIEW entry; silent `pm install-multiple` when capable (#66)
- Settings → Updates: rollback keep-N slider (1–5), ignored-updates overview; App detail per-app auto-install cooldown; Sources repo priority ↑↓ (M38, #63)
- Settings category hub, Updates PAT + check interval, Install anyway for signature WARNING (#61)
- Auto-match UX polish + Hacker theme (#62)
- Freeze / hibernate via `pm suspend` / `unsuspend` (#58)
- Universal APK merge via ARSCLib with mandatory resign (#56)
- Settings → Background controls: HyperOS Autostart (AppOps) + AOSP battery Unrestricted/Optimized/Restricted via Shizuku (#53)
- Catalog / App detail: Open Exodus report (browser); optional self-hosted Exodus API in Settings for tracker name chips (ODbL attribution) (#52)

### Fixed

- CI: warm KSP / `--no-parallel` compile and lint to avoid Hilt classpath flakes (#65)
- Depend on `work-runtime` for WorkManager 2.12 (#60)
- HyperOS Autostart display alignment + Background controls UI polish (#55)

### Changed

- Docs/ROADMAP: M33–M39 closed; publishing remains GitHub Release (F-Droid submit still deferred)

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

[Unreleased]: https://github.com/involvex/kiln/compare/v0.2.6...HEAD
[0.2.6]: https://github.com/involvex/kiln/compare/v0.2.5...v0.2.6
[0.2.5]: https://github.com/involvex/kiln/compare/v0.2.4...v0.2.5
[0.2.4]: https://github.com/involvex/kiln/compare/v0.2.3...v0.2.4
[0.2.3]: https://github.com/involvex/kiln/compare/v0.2.0...v0.2.3
[0.2.2]: https://github.com/involvex/kiln/compare/v0.2.0...v0.2.3
[0.2.1]: https://github.com/involvex/kiln/compare/v0.2.0...v0.2.3
[0.2.0]: https://github.com/involvex/kiln/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/involvex/kiln/releases/tag/v0.1.0
