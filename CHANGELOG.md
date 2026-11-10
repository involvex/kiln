# Changelog

All notable changes to Kiln are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Appearance: System theme, Ocean palette, optional Material You Dynamic Color (API 31+; ignored for Monokai/Ocean)
- Storage phase 2 (Manage): size bars, APK leftover scan/delete (SAF), orphaned Android/data+obb list/delete via Shizuku

### Changed

- Discover / Trending: SectionHeader shelves, EmptyState, compact Trending cards, GitHub results use Card layout
- Permission audit (Settings): dangerous-permission overview; privilege-gated bulk `pm revoke` and basic AppOps set (fixed allowlists)
- Home-screen Glance widget + QS tile: update count, Check now (WorkManager), widget Trim caches (Shizuku or open Manage)
- Activity log (Settings): searchable local Room log of installs, backups, cache clears, maintenance, privilege shell, debloat, Termux, and automation (cap 500; no paths/tokens)
- Kiln-Config export/import (Settings): SAF JSON of repos, managed apps/watchlist, and non-secret prefs; merge import; GitHub PAT never exported
- Scheduled maintenance (Settings → Maintenance, off by default): weekly WorkManager on Wi‑Fi + charging; optional system cache trim (Shizuku), prune download/rollback/backup temps; Run now + last-run protocol
- Intent/Broadcast automation API (Settings → Automation, off by default): signature permission `dev.involvex.kiln.permission.AUTOMATION`; actions `CHECK_UPDATES`, `INSTALL_UPDATE`, `INSTALL_FROM_URL`, `CLEAN_CACHE`; result broadcast + optional failure notification
- Release notes on App detail updates (GitHub release body / F-Droid whatsNew, plain text)
- Watchlist: Watch uninstalled catalog apps (and GitHub lookups); Sources list with new-version badge; background notify, no auto-install
- App detail: Open in Play Store for unmanaged apps installed via Play or Aurora (`market://` with https fallback)
- GitHub update channels on App detail: Stable, Pre-release, Nightly (Actions artifacts; PAT required for Nightly)
- F-Droid anti-feature chips on catalog App details (from index-v2 metadata)
- Auto-match UX: app icons, confidence chips, match rationale, localized accept feedback
- Manual source link from Match installed apps and App detail (F-Droid repo or GitHub `owner/repo`, pinned)
- WARNING matches can be accepted with an explicit confirm dialog (pinned)
- Catalog App details: share button (store / source URLs); re-tap active bottom tab scrolls to top (and pops nested screens)
- Pull-to-refresh on Discover and Trending (catalog sync)

### Changed

- Managed GitHub apps store `githubChannel` instead of a boolean pre-release flag (Obtainium `includePrereleases` maps to Pre-release)
- Auto-match prefers a signer-matching index repo over a higher-priority repo with a mismatched certificate
- Catalog App details layout (hero card, chips, links, top-bar back/share)
- Library pull-to-refresh runs a full update check (repo sync); Discover refresh-catalog control is a compact TextButton

### Fixed

- Lint: share chooser title via `stringResource`; `List.removeLast` NewApi on minSdk 26

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

[Unreleased]: https://github.com/involvex/kiln/compare/v0.2.2...HEAD
[0.2.2]: https://github.com/involvex/kiln/compare/v0.2.1...v0.2.2
[0.2.1]: https://github.com/involvex/kiln/compare/v0.2.0...v0.2.1
[0.2.0]: https://github.com/involvex/kiln/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/involvex/kiln/releases/tag/v0.1.0
