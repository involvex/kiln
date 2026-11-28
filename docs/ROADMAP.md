# Kiln Roadmap

Living milestone tracker. Architecture and feature specs stay in [`PLAN.md`](./PLAN.md). Agent rules: [`../AGENTS.md`](../AGENTS.md).

**Current status:** **M65** Done on `m65-tablet-layouts` (NavigationRail + Apps list-detail). Next: **M66** WebDAV backup destination. Store: GitHub Releases + Obtainium. IzzyOnDroid declined ([repodata#948](https://codeberg.org/IzzyOnDroid/repodata/issues/948)); F-Droid [!58920](https://gitlab.com/fdroid/fdroiddata/-/merge_requests/58920) closed (age &lt; 3 months / uniqueness / template) — refile ≈ Feb 2026. Later: license-cleared debloat import, ROOT `/data/data`.

**License:** Apache-2.0 · **Package:** `dev.involvex.kiln`

**First useful release target:** M0–M5. M6–M8 stretch after Privilege (M4).

---

## Milestones

| ID | Name | Status | Branch / Prompt | Done when |
| --- | --- | --- | --- | --- |
| **M0** | Project setup | Done | [`prompts/M0-setup.md`](./prompts/M0-setup.md) · `m0-setup` | `spotlessCheck detekt lint testDebugUnitTest assembleDebug` green; 5-tab Nav3 shell on device |
| **M1** | Installed apps + APK export (Tier NONE) | Done | [`prompts/M1-installed-apps.md`](./prompts/M1-installed-apps.md) · `m1-installed-apps` | List matches `pm list packages -3`; exports install via `adb` / second device |
| **M2** | Sources, update check, auto-match | Done | PLAN §4.2 / §4.8 · `m2-sources` | Updates for ≥5 real apps; auto-match confidence tiers correct |
| **M3** | Installer (no privilege) | Done | PLAN §4.3 · `m3-installer` | F-Droid + GitHub update E2E |
| **M4** | Privilege layer + Shizuku setup | Done | PLAN §4.9 / §5 · [#11](https://github.com/involvex/kiln/pull/11) | Guided setup A→D; silent install; clean Tier NONE fallback |
| **M5** | Storage analyzer / cleaner | Done | PLAN §4.5 · [#12](https://github.com/involvex/kiln/pull/12) | Numbers plausible vs system storage UI |
| **M6** | Debloat mode | Done | PLAN §4.10 · [#13](https://github.com/involvex/kiln/pull/13) | Snapshot, denylist, deactivate/restore on ≥3 safe packages |
| **M7** | Backup / restore | Done | PLAN §4.6 · [#14](https://github.com/involvex/kiln/pull/14) | APK `.kilnbackup.zip` SAF round-trip; honest NONE/SHIZUKU/ROOT copy |
| **M8** | Background auto-update | Done | PLAN §8 · [#15](https://github.com/involvex/kiln/pull/15) | WorkManager + constraints + notifications |
| **M9** | Termux integration | Done | [`prompts/M9-termux.md`](./prompts/M9-termux.md) · [#20](https://github.com/involvex/kiln/pull/20) | Feature-flagged RUN_COMMAND + PendingIntent |
| **M10** | Polish & release | Done | [`prompts/M10-polish-release.md`](./prompts/M10-polish-release.md) · [#21](https://github.com/involvex/kiln/pull/21) | About, privacy, fastlane, GitHub Release workflow |
| **M11** | App Discovery | Done | PLAN §4.2 browse · [#22](https://github.com/involvex/kiln/pull/22) | Search + categories + catalog sync + install enqueue |
| **M12** | GitHub browse | Done | [`prompts/M12-github-browse.md`](./prompts/M12-github-browse.md) | Discover GitHub tab: lookup + saved sources + install queue |
| **M13** | Settings stability | Done | [`prompts/M13-settings-stability.md`](./prompts/M13-settings-stability.md) · [#27](https://github.com/involvex/kiln/pull/27) | Check-now no crash; EN/DE locales; Termux RequestPermission |
| **M14** | 4-tab nav + Manage | Done | [`prompts/M14-nav-manage.md`](./prompts/M14-nav-manage.md) · [#27](https://github.com/involvex/kiln/pull/27) | Apps / Sources / Manage / Settings; Storage+Backup under Manage |
| **M15** | Store-like Discover | Done | [`prompts/M15-store-ui.md`](./prompts/M15-store-ui.md) · [#27](https://github.com/involvex/kiln/pull/27) | Library\|Discover; catalog detail+icons; font pref |
| **M16** | Trending shelves | Done | [`prompts/M16-trending.md`](./prompts/M16-trending.md) · [#27](https://github.com/involvex/kiln/pull/27) | Topic shelves; no fake download counts |
| **M17** | HyperOS E2E + release prep | Done | [`prompts/M17-hyperos-release-prep.md`](./prompts/M17-hyperos-release-prep.md) · `m17-hyperos-release-prep` | Checklist + `0.2.0` (code 3); Termux multi-candidate detect; flicker + Apps spacing fixes; tag `v0.2.0` after merge; no store submit |
| **M18** | Auto-match UX | Done | `m18-auto-match-ux` · [#32](https://github.com/involvex/kiln/pull/32) | Icons/chips/rationale; prefer signer-matching repo; WARNING confirm + manual F-Droid/GitHub link (Match + App detail) |
| **M19** | GitHub channels + Anti-features | Done | [`prompts/M19-channels-antifeatures.md`](./prompts/M19-channels-antifeatures.md) · [#33](https://github.com/involvex/kiln/pull/33) | Stable/Pre-release/Nightly (Actions artifacts + PAT); anti-feature chips on catalog detail |
| **M20** | Play Store deep-link | Done | [`prompts/M20-play-deeplink.md`](./prompts/M20-play-deeplink.md) · [#34](https://github.com/involvex/kiln/pull/34) | App detail “Open in Play Store” for unmanaged Play/Aurora installs (`market://` + https fallback) |
| **M21** | Changelog & Watchlist | Done | [`prompts/M21-changelog-watchlist.md`](./prompts/M21-changelog-watchlist.md) · [#35](https://github.com/involvex/kiln/pull/35) | Release notes on updates; Watch from catalog; Sources watchlist |
| **M22** | Intent / Broadcast API | Done | [`prompts/M22-intent-api.md`](./prompts/M22-intent-api.md) · [#36](https://github.com/involvex/kiln/pull/36) | Opt-in AutomationReceiver: CHECK_UPDATES / INSTALL_UPDATE / INSTALL_FROM_URL / CLEAN_CACHE; signature permission |
| **M23** | Scheduled maintenance | Done | [`prompts/M23-scheduled-maintenance.md`](./prompts/M23-scheduled-maintenance.md) · [#37](https://github.com/involvex/kiln/pull/37) | Weekly WorkManager (Wi‑Fi+charging): trim caches, prune downloads/rollback/backup temps; run protocol |
| **M24** | Activity log | Done | [`prompts/M24-activity-log.md`](./prompts/M24-activity-log.md) · [#39](https://github.com/involvex/kiln/pull/39) | Searchable local log of installs, backups, cleaner, shell |
| **M25** | Config export/import | Done | [`prompts/M25-config-export.md`](./prompts/M25-config-export.md) · [#38](https://github.com/involvex/kiln/pull/38) | Kiln-Config JSON via SAF (Cloudflare sync later) |
| **M26** | Widgets / QS tile | Done | [`prompts/M26-widgets-qs.md`](./prompts/M26-widgets-qs.md) · [#40](https://github.com/involvex/kiln/pull/40) | Glance: update count, check-now, cache shortcut; thin QS tile |
| **M27** | Permission audit | Done | [`prompts/M27-permission-audit.md`](./prompts/M27-permission-audit.md) · [#41](https://github.com/involvex/kiln/pull/41) | Dangerous perms overview; AppOps / bulk revoke via privilege |
| **M28** | Storage phase 2 | Done | [`prompts/M28-storage-phase2.md`](./prompts/M28-storage-phase2.md) · [#44](https://github.com/involvex/kiln/pull/44) | Size bars, APK leftovers, orphaned Android/data+obb |
| **M29** | APK tools phase 2 | Done | [`prompts/M29-apk-tools.md`](./prompts/M29-apk-tools.md) · [#47](https://github.com/involvex/kiln/pull/47) | Inspect + signature compare; shared signing reader (merge/resign deferred) |
| **M30** | Mirrors / proxy | Done | [`prompts/M30-mirrors-proxy.md`](./prompts/M30-mirrors-proxy.md) · [#48](https://github.com/involvex/kiln/pull/48) | Per-repo mirrors + HTTP/SOCKS/Orbot proxy |
| **M31** | APK tools resign / split export | Done | [`prompts/M31-apk-tools-resign.md`](./prompts/M31-apk-tools-resign.md) · [#49](https://github.com/involvex/kiln/pull/49) | Resign via apksig + export splits from `.apks`/`.xapk` |
| **M32** | Update-check responsiveness | Done | [`prompts/M32-update-check-perf.md`](./prompts/M32-update-check-perf.md) · [#50](https://github.com/involvex/kiln/pull/50) | Room-only badge refresh; progress/cancel; mirror fail-fast; parallel repo sync |
| **M33** | Exodus privacy chips | Done (removed M44) | [`prompts/M33-exodus-privacy.md`](./prompts/M33-exodus-privacy.md) · [#52](https://github.com/involvex/kiln/pull/52) | Shipped deep-link + optional API; **removed in M44** (public reports often 404; no end-user API) |
| **M34** | Autostart + battery | Done | [`prompts/M34-autostart-battery.md`](./prompts/M34-autostart-battery.md) · [#53](https://github.com/involvex/kiln/pull/53) | HyperOS Autostart AppOps + AOSP battery tristate via Shizuku |
| **M35** | Universal APK merge | Done | [`prompts/M35-universal-apk-merge.md`](./prompts/M35-universal-apk-merge.md) · [#56](https://github.com/involvex/kiln/pull/56) | Merge `.apks`/`.xapk`/`.apkm` via ARSCLib + always resign (M31 keystore) |
| **M36** | Freeze / hibernate | Done | [`prompts/M36-freeze-hibernate.md`](./prompts/M36-freeze-hibernate.md) · [#58](https://github.com/involvex/kiln/pull/58) | `pm suspend`/`unsuspend` via privilege; Settings list separate from Debloat |
| **M37** | Settings hub + PAT + signature confirm | Done | [`prompts/M37-settings-hub.md`](./prompts/M37-settings-hub.md) · [#61](https://github.com/involvex/kiln/pull/61) | Category hub; Updates PAT + check interval; Install anyway for WARNING only |
| **M38** | Settings polish | Done | [`prompts/M38-settings-polish.md`](./prompts/M38-settings-polish.md) · [#63](https://github.com/involvex/kiln/pull/63) | Rollback N slider; per-app cooldown; ignored-updates overview; repo priority ↑↓ |
| **M39** | Split APK install | Done | [`prompts/M39-split-install.md`](./prompts/M39-split-install.md) · [#66](https://github.com/involvex/kiln/pull/66) | SAF + Share/VIEW for `.apks`/`.xapk`/`.apkm`; PackageInstaller multi-session; optional silent `install-multiple` |
| **M40** | Backup phase 2 | Done | [`prompts/M40-backup-phase2.md`](./prompts/M40-backup-phase2.md) · [#69](https://github.com/involvex/kiln/pull/69) | SAF tree + keep-N; Shizuku Android/data+obb; optional AES-GCM (no Root) |
| **M41** | Discover panel | Done | [`prompts/M41-discover-panel.md`](./prompts/M41-discover-panel.md) · [#71](https://github.com/involvex/kiln/pull/71) | Hilt Nav3 crash fix; F-Droid-style shelves in Discover; drop Trending segment |
| **M42** | GitHub starred → Sources | Done | [`prompts/M42-github-stars.md`](./prompts/M42-github-stars.md) · [#73](https://github.com/involvex/kiln/pull/73) | PAT `/user/starred` review → multi-select add; kilnHiltViewModel shell crash fix |
| **M43** | App activities viewer | Done | [`prompts/M43-app-activities.md`](./prompts/M43-app-activities.md) · [#75](https://github.com/involvex/kiln/pull/75) | Exported activities list + Start + pin shortcut; AppDetail AssistedInject crash fix |
| **M44** | Discover polish + remove Exodus | Done | [`prompts/M44-discover-polish.md`](./prompts/M44-discover-polish.md) · [#76](https://github.com/involvex/kiln/pull/76) | Denser shelves; catalog detail actions under hero; remove Exodus UI/API/Settings |
| **M45** | Hilt stability + App Detail overhaul | Done | [`prompts/M45-stability-app-detail.md`](./prompts/M45-stability-app-detail.md) · [#79](https://github.com/involvex/kiln/pull/79) | KSP incremental off + Hilt VM map guard; App Detail hero/actions/cards; kilnHiltViewModel harden |
| **M46** | Widget rework | Done | [`prompts/M46-widget-rework.md`](./prompts/M46-widget-rework.md) · [#81](https://github.com/involvex/kiln/pull/81) | Updates list+Check; Cache widget; Processes mem/CPU+kill (Shizuku) |
| **M47** | Polish + signed 0.2.5 | Done | [`prompts/M47-polish-signed-release.md`](./prompts/M47-polish-signed-release.md) · [#86](https://github.com/involvex/kiln/pull/86) | Require upload signing + apksigner; icon decode perf; tag `v0.2.5` |
| **M48** | Storage large files + trash | Done | [`prompts/M48-storage-large-files-trash.md`](./prompts/M48-storage-large-files-trash.md) · `m48-storage-large-files-trash` | SAF ≥50MB files; same-size+hash duplicates; `.kiln-trash` undo/purge; maintenance age-prune |
| **M49** | Split backup restore | Done | [`prompts/M49-split-backup-restore.md`](./prompts/M49-split-backup-restore.md) · `m49-split-backup-restore` | Restore enqueue all APKs from archive (M39 installer) |
| **M50** | App component lists | Done | [`prompts/M50-app-components.md`](./prompts/M50-app-components.md) · `m50-app-components` | Exported services/receivers/providers (read-only) |
| **M51** | Component enable / disable | Done | [`prompts/M51-component-toggles.md`](./prompts/M51-component-toggles.md) · `m51-component-toggles` | Shizuku `pm enable`/`disable` one component; denylist; NONE read-only |
| **M52** | In-app process list | Done | [`prompts/M52-manage-processes.md`](./prompts/M52-manage-processes.md) · `m52-manage-processes` | Settings → Tools processes; force-stop confirm; reuse widget snapshot |
| **M53** | Read-only user census | Done | [`prompts/M53-user-census.md`](./prompts/M53-user-census.md) · `m53-user-census` | `pm list users` + current user id; no `--user N` installs |
| **M54** | Store listing packet | Done (packet); listings closed | [STORE.md](./STORE.md) · [repodata#948](https://codeberg.org/IzzyOnDroid/repodata/issues/948) · [fdroiddata!58920](https://gitlab.com/fdroid/fdroiddata/-/merge_requests/58920) | Fastlane + screenshots; Izzy AI decline; F-Droid closed until ≥3 months; Distribute via GitHub + Obtainium |
| **M55** | Dual-user installs | Done | [`prompts/M55-dual-user-installs.md`](./prompts/M55-dual-user-installs.md) · `m55-dual-user-installs` | Best-effort `--user N` on install/uninstall when M53 census has ≥2 users; single-user = unchanged |
| **M56** | Per-user component / freeze | Done | [`prompts/M56-per-user-state.md`](./prompts/M56-per-user-state.md) · `m56-per-user-state` | Best-effort `--user N` on component enable/disable + freeze/unfreeze |
| **M57** | Debloat batch per user | Done | [`prompts/M57-debloat-per-user.md`](./prompts/M57-debloat-per-user.md) · `m57-debloat-per-user` | Best-effort `--user N` on debloat disable/enable/uninstall-for-user/install-existing; snapshot restore stays current user |
| **M58** | Backup / restore user targeting | Done | [`prompts/M58-backup-per-user.md`](./prompts/M58-backup-per-user.md) · `m58-backup-per-user` | Best-effort restore install `--user N` + backup metadata note when census ≥2 + silent install; Android/data stays current-user paths |
| **M59** | HyperOS phone polish | Done | [`prompts/M59-hyperos-polish.md`](./prompts/M59-hyperos-polish.md) · `m59-hyperos-polish` | Dual-user honesty copy (no stale “user 0 only”); docs sync; optional short HyperOS checklist (not a gate) |
| **M60** | Device diagnostics | Done | [`prompts/M60-device-diagnostics.md`](./prompts/M60-device-diagnostics.md) · [#111](https://github.com/involvex/kiln/pull/111) | Shizuku pretty cards: battery / netstats / top; no Root; no raw dumps |
| **M61** | GitLab + Codeberg sources | Done | [`prompts/M61-gitlab-codeberg.md`](./prompts/M61-gitlab-codeberg.md) · `m61-gitlab-codeberg` | Release sources for GitLab/Codeberg; same channel/APK heuristic as GitHub |
| **M62** | Storage treemap | Done | [`prompts/M62-storage-treemap.md`](./prompts/M62-storage-treemap.md) · `m62-storage-treemap` | Squarified treemap of largest consumers; replaces size bars |
| **M63** | Curated debloat ratings | Done | [`prompts/M63-curated-debloat-ratings.md`](./prompts/M63-curated-debloat-ratings.md) · `m63-curated-debloat-ratings` | Parser + tests + catalog meta UI; `packages: []` until license-cleared import |
| **M64** | Diagnostics extras | Done | [`prompts/M64-diagnostics-extras.md`](./prompts/M64-diagnostics-extras.md) · `m64-diagnostics-extras` | `dumpsys cpuinfo` + `thermalservice` pretty cards; no raw dump; no `cmd battery` |
| **M65** | Tablet / foldable layouts | Done | [`prompts/M65-tablet-layouts.md`](./prompts/M65-tablet-layouts.md) · `m65-tablet-layouts` | NavigationRail at medium+; Apps `ListDetailSceneStrategy`; phone bar unchanged |

---

## PLAN §12 backlog status

| Item | Status | Notes |
| --- | --- | --- |
| Rollback / Downgrade | Done | ≤3/pkg cache; M23 age/1 GiB prune; M38 Settings N slider (1–5) |
| Update cooldown | Done | Global days + major skip; M38 per-app override + ignored overview |
| Repo priority UI | Done (M38) | Sources ↑↓; ensureKnownRepos preserves priority |
| GitHub Nightly channels | Done | M19 #33 |
| Anti-feature chips | Done | M19 F-Droid chips; Exodus M33 removed in M44 |
| HyperOS Autostart / battery | Done | M34 (#53) — AppOps 10008/10021 + deviceidle whitelist |
| Changelog & Watchlist | Done | M21 #35 |
| Intent / Broadcast API | Done | M22 #36 |
| Scheduled maintenance | Done | M23 #37 |
| Activity log | Done | M24 #39 |
| Config export/import | Done | M25 #38 (Obtainium import already exists; separate format) |
| Widgets / QS | Done (M26); rework Done (M46 #81) | M26 #40; M46: three Glance widgets |
| Permission audit | Done | M27 #41 |
| Storage phase 2 | Done | M28 #44 |
| SAF backup rotation / data tiers / encryption | Done (M40 #69) | SAF tree + keep-N + Shizuku Android/data+obb + optional AES-GCM; ROOT / cloud deferred |
| APK tools 2 (inspect + compare) | Done | M29 (#47); M31 (#49); universal merge M35 (#56) |
| Mirrors / proxy | Done | M30 #48 |
| Freeze/hibernate, dual-user, tablet | Done | M36 freeze (#58); M53–**M58** dual-user; **M59** polish; **M65** tablet/foldable rail + Apps list-detail |
| Publishing / F-Droid + Izzy | Closed for now | Izzy [repodata#948](https://codeberg.org/IzzyOnDroid/repodata/issues/948); F-Droid !58920 closed (3‑month rule). Ship via GitHub + Obtainium; F-Droid refile ≈ Feb 2026. Play skipped (US$25) |
| HyperOS M1–M8 checklist sweep | Optional / low priority | Older acceptance lists; **M59** short checklist in prompt only — not a gate. Run ad hoc if something regresses |
| Device diagnostics (battery / netstats / top) | Done (M60 #111) | Shizuku-only pretty cards; observe adbcaptain commands, no AGPL copy |
| GitLab / Codeberg release sources | Done (M61) | Same Stable/Pre-release + APK heuristic as GitHub |
| Discover home shelves (absorb Trending) | Done (M41 #71) | Nav3 Hilt fix; shelves + categories on Discover; Library\|Discover only |
| GitHub starred repos → Sources | Done (M42 #73) | Review list + multi-select add; APK heuristic; no silent install |
| App activity viewer / launcher | Done (M43 #75) | Exported-only list under App Detail; Tier NONE Start + pin shortcut |
| Discover UI density + Exodus removal | Done (M44 #76) | Compact shelves; detail Install under hero; drop Exodus |
| Hilt/KSP ViewModel map + App Detail IA | Done (M45 #79) | Guard incomplete Hilt maps; denser App Detail like Catalog |
| Widget rework (Updates / Cache / Processes) | Done (M46 #81) | Drop trim from Updates; dedicated Cache + Processes Glance |

---

## Capability matrix

Update after each milestone. Tier = active `PrivilegedExecutor`.

| Feature | Now (main / 0.2.14) | Needs tier |
| --- | --- | --- |
| Navigation shell / theme | ✅ Nav3, bottom bar (compact) / NavigationRail (medium+), Apps list-detail, design system | NONE |
| List installed apps | ✅ Summary list, search, User/System/All, sort; update badges | NONE |
| App detail | ✅ Version, installer, SHA-256, paths, permissions; **M43** activities + Start/pin; **M50/M51** services/receivers/providers with enable/disable (Shizuku + denylist; non-exported read-only); **M56** component toggle can target census user; rollback + Shizuku downgrade | NONE / SHIZUKU |
| APK export (single / batch) | ✅ `.apk` / `.apks` STORED / `.xapk`; SAF + share + WorkManager batch | NONE |
| Uninstall (system dialog) | ✅ `ACTION_DELETE`; system apps disabled | NONE |
| Sources / updates | ✅ F-Droid/Izzy/Guardian defaults + FP upsert; `fdroidrepo(s):` / `?fingerprint=` deep links; index-v2 + entry.jar verify; GitHub + **M61** GitLab.com / Codeberg Releases; **M42** starred import (PAT review → multi-select); Obtainium import; Room cache; per-repo mirrors (M30) | NONE (+ network) |
| Network proxy | ✅ HTTP/SOCKS + Orbot SOCKS via `KilnHttpClient` (M30) | NONE |
| Auto-match | ✅ Confidence tiers; icons/chips/rationale; signer-preferring multi-repo pick; WARNING confirm + manual F-Droid/GitHub link (pinned) | NONE |
| GitHub channels | ✅ Stable / Pre-release / Nightly (Actions artifacts; PAT required for Nightly) | NONE (+ network; PAT for Nightly) |
| Anti-features | ✅ F-Droid `metadata.antiFeatures` chips on catalog detail | NONE (+ catalog) |
| Play Store deep-link | ✅ App detail CTA when unmanaged + Play/Aurora installer (`market://` → https) | NONE |
| Changelog / watchlist | ✅ Release notes on App detail; Watch from catalog; Sources watchlist (notify, no auto-install) | NONE (+ network) |
| Automation Intent API | ✅ Settings opt-in + signature `AUTOMATION`; CHECK_UPDATES / INSTALL_* / CLEAN_CACHE + result broadcast | NONE (+ privilege for CLEAN_CACHE) |
| Scheduled maintenance | ✅ weekly Wi‑Fi+charging; trim / prune downloads / rollback / backup temps; protocol | NONE (+ privilege for trim) |
| Kiln-Config export/import | ✅ SAF JSON merge of repos, managed apps, non-secret prefs (no PAT) | NONE |
| Activity log | ✅ Room-backed searchable log (install/backup/cache/maintenance/shell/…) | NONE |
| Widgets / QS | ✅ M46: Updates (preview+Check); Cache (confirm/`CLEAR_CACHE`); Processes (mem/CPU+`FORCE_STOP`); QS Check now | NONE / SHIZUKU (`CLEAR_CACHE`, `RUN_SHELL_DIAGNOSTIC`, `FORCE_STOP`) |
| In-app processes | ✅ **M52** Settings → Tools → Processes (same meminfo + force-stop gate as widget) | NONE / SHIZUKU (`RUN_SHELL_DIAGNOSTIC`, `FORCE_STOP`) |
| Device diagnostics | ✅ **M60** + **M64** Settings → Tools → Device diagnostics (battery / netstats / top / cpuinfo / thermals; truncated dumps; no `cmd battery`) | NONE / SHIZUKU (`RUN_SHELL_DIAGNOSTIC`) |
| Android user census | ✅ **M53** Settings → Tools → Android users (`pm list users` read-only); **M59** copy reflects best-effort dual-user targeting | NONE / SHIZUKU (`RUN_SHELL_DIAGNOSTIC`) |
| Dual-user install/uninstall | ✅ **M55** optional `--user N` when census ≥2 + silent Shizuku; else unchanged | NONE / SHIZUKU (`SILENT_INSTALL` / `SILENT_UNINSTALL`) |
| Dual-user component / freeze | ✅ **M56** optional `--user N` on component enable/disable + suspend/unsuspend when census ≥2 + DEBLOAT | NONE / SHIZUKU (`DEBLOAT`) |
| Dual-user debloat batch | ✅ **M57** optional `--user N` on disable/enable/uninstall-for-user/install-existing when census ≥2 + DEBLOAT; snapshot restore uses current user | NONE / SHIZUKU (`DEBLOAT`) |
| Dual-user backup / restore | ✅ **M58** optional census user on restore confirm (silent install) + backup metadata note; Android/data+obb stay current-user paths | NONE / SHIZUKU (`SILENT_INSTALL`) |
| Permission audit | ✅ Dangerous overview; `pm revoke` + basic AppOps set | NONE / SHIZUKU (`MANAGE_APP_OPS`) |
| Background controls | ✅ HyperOS Autostart + battery Unrestricted/Optimized/Restricted | SHIZUKU (`MANAGE_APP_OPS`) |
| Privilege / Shizuku setup | ✅ Settings → A→D assistant; UserService self-test; capability matrix | NONE → SHIZUKU |
| Installer | ✅ PackageInstaller (NONE) or silent `pm install -r` when `SILENT_INSTALL`; Settings prefers Default vs Shizuku; ✅ M39 multi-session + `install-multiple` for `.apks`/`.xapk`/`.apkm` (SAF + Share/VIEW) | NONE / SHIZUKU |
| Storage / cleaner | ✅ **M62** squarified treemap of largest consumers (apps + scanned leftovers/orphans/large files); APK leftovers; **M48** large files (≥50MB) + same-size/hash duplicates; `.kiln-trash` undo/purge (14d maintenance prune); orphan Android/data+obb; cache clear | NONE / SHIZUKU (`CLEAR_CACHE`, `ACCESS_ANDROID_DATA`) |
| APK tools (inspect / compare / resign / merge) | ✅ M29 inspect+compare; ✅ M31 resign + split export; ✅ M35 universal merge (#56) | NONE |
| Freeze / hibernate | ✅ M36 (#58) — `pm suspend`/`unsuspend`; Settings list (separate from Debloat); **M56** optional census user | ≥ SHIZUKU (`DEBLOAT`) |
| Debloat | ✅ Settings entry; denylist; snapshot; disable/enable/user-uninstall via privilege; **M57** optional census user; **M63** curated catalog pipeline (`debloat-db.json` still empty; UNKNOWN + expert mode) | ≥ SHIZUKU |
| Backup (APK) | ✅ `.kilnbackup.zip`; **M40** SAF tree + keep-N; Shizuku Android/data+obb when capable; optional AES-GCM; **M49** restore installs all split APKs; **M58** optional restore `--user N` + backup user tag; ROOT `/data/data` deferred | NONE (APK) / SHIZUKU (`ACCESS_ANDROID_DATA`, `SILENT_INSTALL` for dual-user) |
| Background auto-update | ✅ Settings hub → Updates; 12h / daily / weekly PeriodicWork; notify-only default; optional auto-enqueue for signatureMatches; global + per-app cooldown; APK rollback keep-N (1–5); ignored-updates overview; GitHub PAT under Updates | NONE (+ network; silent install/downgrade if SHIZUKU) |
| Repo priority | ✅ Sources ↑↓ reorder; `ensureKnownRepos` preserves user priority; Auto-match / update conflict tie-break | NONE |
| Termux RUN_COMMAND | ✅ feature flag, RequestPermission UX, configurable packageId, echo smoke test + PendingIntent; **M17: package detection** (`TermuxPackageDetector` finds renamed forks e.g. `com.involvex.termux_app`, verifies the `RunCommandService`), App Info shortcut for forks that declare their own `RUN_COMMAND` permission | NONE (Termux + permission + allow-external-apps) |
| Appearance locales | ✅ AppCompat + localeConfig; EN/DE chips | NONE |
| Release / store prep | ✅ version `0.2.14` (code 17); fastlane + screenshots; [STORE.md](./STORE.md); Izzy + F-Droid closed; Obtainium | — |
| App Discovery | ✅ Apps → Library\|Discover; home shelves + categories; catalog refresh; icons; detail; search; GitHub tab; install queue | NONE (+ network) |
| GitHub browse (Discover) | ✅ F-Droid/GitHub tabs; owner/repo lookup; saved sources; install queue | NONE (+ network; PAT optional) |
| Navigation (4-tab) | ✅ Apps / Sources / Manage / Settings; Storage+Backup under Manage; **M65** rail + Apps list-detail on medium+; **M42** `kilnHiltViewModel` Activity scope | NONE |
| Trending shelves | ✅ Category/keyword shelves (honest: no download charts) | NONE (+ catalog) |

---

## M9 manual acceptance (HyperOS)

Code on `m9-termux` / #20:

- [ ] Settings → Termux opens the feature screen
- [ ] Enable flag off: smoke-test button refuses to start a command
- [ ] Package ID field accepts a fork id and updates path checks
- [ ] With Termux installed: checklist shows installed; grant `RUN_COMMAND` under Additional permissions
- [ ] In Termux: `allow-external-apps=true` in `~/.termux/termux.properties`
- [ ] Echo smoke test returns stdout containing `kiln-termux-ok` (background)
- [ ] Confirm Termux cannot replace Shizuku (copy visible on screen)

---

## M10 manual acceptance

Code on main (post-#21):

- [ ] Settings → About shows version name/code
- [ ] Source / privacy / license buttons open the GitHub URLs
- [ ] `docs/PRIVACY.md` and `CHANGELOG.md` match the shipped story
- [ ] Tag `v0.1.0` creates a GitHub Release with an APK (optional; publishing deferred)
- [x] Capture phone screenshots into `fastlane/metadata/android/en-US/images/phoneScreenshots/` before F-Droid submit

---

## M8 manual acceptance (HyperOS)

Code on `m8-auto-update`:

- [ ] Settings → enable background checks; grant notifications when prompted
- [ ] “Check now” posts a notification when updates exist (or no-op message when none)
- [ ] Auto-install off: no InstallQueue jobs from background/check-now
- [ ] Auto-install on: only signature-matching updates enqueue; WARNUNG never auto
- [ ] Disable background checks cancels periodic work
- [ ] Wi‑Fi only toggle maps to UNMETERED vs CONNECTED constraints

---

## M2 manual acceptance (HyperOS)

Code for M2 is on `main` (post-#9). Tick on device after install (network required):

- [x] Sources tab lists preconfigured F-Droid + IzzyOnDroid; sync succeeds; last sync time updates
- [ ] Failed fingerprint / bad `entry.jar` does not poison cache (re-sync after fixing still works)
- [x] Auto-match groups HOCH / MITTEL / WARNUNG / KEINE; accept-all-safe only takes HOCH (+ optional MITTEL if UI allows)
- [ ] Apps list shows update badges for ≥5 real packages vs live indexes and/or GitHub map (re-check with #10 “Nach Updates suchen”)
- [x] Detail / list show signature WARNUNG copy when certs differ (#10)
- [x] Obtainium/Discoverium JSON file import creates managed GitHub apps; PAT optional (rate-limit string visible without PAT)
- [ ] Installed-app list never leaves the device (no bulk upload); only mapped/managed apps hit GitHub

---

## M7 manual acceptance (HyperOS)

Code on `m7-backup`. Tier NONE is enough for APK round-trip; Shizuku optional for silent restore:

- [ ] Backup tab lists user apps; search and select work
- [ ] Honesty banner: APK-only; no claim of `/data/data` without Root; Shizuku Android/data marked deferred
- [ ] Backup APK writes `.kilnbackup.zip` via SAF; archive contains APK(s) + `metadata.json`
- [ ] Restore confirms, then queues install; system PackageInstaller dialog (or silent when Shizuku ready)
- [ ] Split app: archive includes splits; restore installs **all** APKs via multi-session / install-multiple (M49)
- [ ] Destructive restore always confirms before SAF picker

Deferred: Android/data + OBB (SHIZUKU), full `/data/data` (ROOT), encryption, cloud targets.

---

## M6 manual acceptance (HyperOS)

Code merged via [#13](https://github.com/involvex/kiln/pull/13). Needs Shizuku/Shizuku+ with DEBLOAT (shell uid 2000 verified on device):

- [ ] Settings → Debloat opens; without privilege shows setup CTA
- [ ] Hard denylist blocks Kiln, Shizuku+, SystemUI, Settings, launcher/IME/dialer/SMS
- [ ] Snapshot save before first change; restore returns prior enabled/disabled/uninstalled states
- [ ] Disable ≥3 harmless test packages, re-enable them; per-package failures do not abort the batch
- [ ] UNKNOWN packages require expert mode + typed `I UNDERSTAND` / `ICH VERSTEHE`
- [ ] Default action is disable, not uninstall-for-user
- [ ] No invented package ratings in `debloat-db.json` (empty curated list for now)

---

## M5 manual acceptance (HyperOS)

Code on `m5-storage`. Usage access + optional Shizuku:

- [ ] Storage tab shows usage-access explain screen; after grant, list loads without crash
- [ ] Totals and top apps look plausible vs system Settings → Storage (same order of magnitude)
- [ ] User / Include-system filter and search work
- [ ] Without Shizuku: Clear/App info opens system app details (confirmation first)
- [ ] With Shizuku + CLEAR_CACHE: per-app cache clear and trim-all confirm, then sizes refresh (HyperOS may free less than shown)
- [ ] No silent delete; destructive actions always confirm

Deferred in this PR: treemap, orphaned `Android/data`, APK leftover scanner.

---

## M17 manual acceptance (HyperOS) — done

Shipped with `0.2.0` and follow-up flicker hotfixes through `0.2.2`. Prompt: [`prompts/M17-hyperos-release-prep.md`](./prompts/M17-hyperos-release-prep.md).

- [x] Language EN/DE chips: first frame + bottom nav labels switch — verified on device, no crash
- [x] Settings → Check now: no crash; update notification / empty-result path OK
- [x] Exactly 4 tabs; Manage → Storage | Backup only
- [x] Apps → Library | Discover (Trending absorbed into Discover home in M41)
- [x] Catalog refresh: icons + detail (+ screenshots when index has media)
- [x] Trending ≥3 shelves with honest copy (no fake download charts)
- [x] CHANGELOG / `0.2.0` bump done in PR
- [x] Termux: smoke test now runs — **was failing**: device ships a renamed fork (`com.involvex.termux_app`), not the official `com.termux`, so the package-id field defaulted to `com.termux` and nothing matched. Fixed with `TermuxPackageDetector` (scan + service-class verification) + "Detect Termux package" button + App Info shortcut for forks that declare their own `RUN_COMMAND` permission. Follow-up: multi-candidate detect (skip Termux:Boot add-ons) + clear sticky “No install found” when checklist is green. Re-run Detect after rebuild.
- [x] HyperOS flicker: DayNight window + locale identity compare — fixed with dark AppCompat theme + language-tag equality
- [x] Apps Library whitespace: embedded shell drops duplicate “Apps” TopAppBar

Older M1/M3/M4/M5 checklists below remain open for a fuller pass when convenient.

---

## M4 manual acceptance (HyperOS)

Code on `main`. Needs Shizuku or ShizukuPlus installed for privileged path; Tier NONE must still work:

- [ ] Settings → Privilege setup opens A→D assistant; status re-checks on resume
- [ ] Phase A when manager missing; install link opens Shizuku releases
- [ ] Phase B when binder down: open manager / developer options / copy ADB start command
- [ ] Phase C: request permission; grant in Shizuku dialog → Phase D
- [ ] Self-test shows UID (2000 shell or 0 root), SELinux, API; matrix lists silent install Available
- [ ] With Shizuku ready: update install completes without system confirm (silent `pm install -r`)
- [ ] Without Shizuku: same install still uses PackageInstaller system confirm (Tier NONE fallback)
- [ ] No crash when Shizuku stops mid-session; banner/status returns to B/C

---

## M3 manual acceptance (HyperOS)

Code for M3 is on `main`. Network + unknown-sources permission:

- [ ] Allow “Install unknown apps” for Kiln when prompted
- [ ] Sync Sources; open an app with a signature-matching update; Download & install runs FGS notification
- [ ] System confirm dialog appears; after confirm, version updates in Kiln list
- [ ] F-Droid-sourced APK with wrong SHA-256 fails closed (no install)
- [x] Signature WARNUNG: primary Install disabled; optional Install anyway confirm (M37); auto-install never
- [ ] GitHub-mapped app with matching flow can install (PAT optional)
- [ ] Split bundles not required for M3 (single APK only)

---

## M1 manual acceptance (HyperOS)

Code and CI for M1 are on `main`. Tick these on the device when convenient:

- [ ] User-app count in Kiln (User filter) matches `adb shell pm list packages -3` (explain any deltas); System filter shows system apps
- [ ] List scrolls smoothly; icons load; search / filter / sort work
- [ ] Detail: version, installer label, SHA-256 spot-check vs `apksigner` / `keytool`
- [ ] Non-split `.apk` export installs via `adb install` or a second device
- [ ] Split app `.apks` / `.xapk` installs via `adb install-multiple` (or second device)
- [ ] Batch export of ≥5 apps: progress, cancel, summary
- [ ] Uninstall via system dialog; list refreshes; system apps not uninstallable
- [ ] Batch still works if notification permission is denied

---

## Suggested sequence

```mermaid
flowchart LR
  M17[M17 HyperOS E2E] --> M18[M18 Auto-match UX]
  M18 --> M19[M19 Channels + Anti-features]
  M19 --> M20[M20 Play deeplink]
  M20 --> M21[M21 Changelog + Watchlist]
  M21 --> M22[M22 Intent API]
  M22 --> M23[M23 Scheduled maintenance]
  M23 --> M24[M24 Activity log]
  M24 --> M25[M25 Config export]
  M25 --> M26[M26 Widgets]
  M26 --> M27[M27 Permission audit]
  M27 --> M28[M28 Storage phase 2]
  M28 --> M29[M29 APK tools]
  M29 --> M30[M30 Mirrors proxy]
  M30 --> M31[M31 APK resign]
  M31 --> M32[M32 Update check perf]
  M32 --> M33[M33 Exodus chips]
  M33 --> M34[M34 Autostart battery]
  M34 --> M35[M35 Universal merge]
  M35 --> M36[M36 Freeze hibernate]
  M36 --> M37[M37 Settings hub]
  M37 --> M38[M38 Settings polish]
  M38 --> M39[M39 Split install]
  M39 --> M40[M40 Backup phase 2]
  M40 --> M41[M41 Discover panel]
  M41 --> M42[M42 GitHub stars]
  M42 --> M43[M43 App activities]
  M43 --> M44[M44 Discover polish]
  M44 --> M45[M45 Hilt + App Detail]
  M45 --> M46[M46 Widget rework]
  M46 --> M47[M47 Polish signed 0.2.5]
  M47 --> M48[M48 Large files trash]
  M48 --> M49[M49 Split restore]
  M49 --> M50[M50 Components]
  M50 --> v026[v0.2.6]
  v026 --> M51[M51 Component toggles]
  M51 --> M52[M52 In-app processes]
  M52 --> M53[M53 User census]
  M53 --> v027[v0.2.7]
```

1. **M66** WebDAV backup destination (not Cloudflare/R2).
2. Store: GitHub + Obtainium; F-Droid refile only after ≈2026-02-08 ([STORE.md](./STORE.md)).
3. Later: license-cleared debloat package import, ROOT `/data/data`.

---

## Docs layout

| Path | Role |
| --- | --- |
| [`PLAN.md`](./PLAN.md) | Vision, architecture, feature specs, security, backlog |
| [`ROADMAP.md`](./ROADMAP.md) | This file — status and order |
| [`prompts/`](./prompts/) | Copy-paste agent prompts per milestone |
| [`reference.md`](./reference.md) | Local reference checkouts + SPDX (read-only / clean-room) |
