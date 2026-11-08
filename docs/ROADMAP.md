# Kiln Roadmap

Living milestone tracker. Architecture and feature specs stay in [`PLAN.md`](./PLAN.md). Agent rules: [`../AGENTS.md`](../AGENTS.md).

**Current status:** M6 merged (#13). M7 on branch `m7-backup` (Tier NONE APK `.kilnbackup.zip` round-trip; data tiers deferred with honest UI). Shizuku+ self-test on device: uid=2000 shell. M3 HyperOS E2E still open.

**License:** Apache-2.0 · **Package:** `dev.involvex.kiln`

**First useful release target:** M0–M5. M6–M7 stretch after Privilege (M4).

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
| **M7** | Backup / restore | In progress | PLAN §4.6 · `m7-backup` | APK `.kilnbackup.zip` SAF round-trip; honest NONE/SHIZUKU/ROOT copy |
| **M8** | Background auto-update | Planned | PLAN §8 | WorkManager + constraints + notifications |
| **M9** | Termux integration | Planned | PLAN §4.7 | Feature-flagged hooks |
| **M10** | Polish & release | Planned | PLAN §8 | F-Droid/GitHub Releases, screenshots, a11y |

---

## Capability matrix

Update after each milestone. Tier = active `PrivilegedExecutor`.

| Feature | Now (M4 branch) | Needs tier |
| --- | --- | --- |
| Navigation shell / theme | ✅ Nav3, bottom bar, design system | NONE |
| List installed apps | ✅ Summary list, search, User/System/All, sort; update badges | NONE |
| App detail | ✅ Version, installer, SHA-256, paths, permissions | NONE |
| APK export (single / batch) | ✅ `.apk` / `.apks` STORED / `.xapk`; SAF + share + WorkManager batch | NONE |
| Uninstall (system dialog) | ✅ `ACTION_DELETE`; system apps disabled | NONE |
| Sources / updates | ✅ F-Droid/Izzy index-v2 + entry.jar verify; GitHub releases; Obtainium import; Room cache | NONE (+ network) |
| Auto-match | ✅ Confidence tiers HOCH/MITTEL/WARNUNG/KEINE; ManagedApp persistence | NONE |
| Privilege / Shizuku setup | ✅ Settings → A→D assistant; UserService self-test; capability matrix | NONE → SHIZUKU |
| Installer | ✅ PackageInstaller (NONE) or silent `pm install -r` when `SILENT_INSTALL`; splits deferred | NONE / SHIZUKU |
| Storage / cleaner | ✅ Usage-access gate; App/Data/Cache list; clear via Shizuku or app-info deep-link | NONE / SHIZUKU |
| Debloat | ✅ Settings entry; denylist; snapshot; disable/enable/user-uninstall via privilege (ratings UNKNOWN until curated DB) | ≥ SHIZUKU |
| Backup (APK) | ✅ `.kilnbackup.zip` (STORED + metadata.json); restore via InstallQueue local; splits backed up, base APK restored; `/data/data` + Android/data deferred | NONE (APK) … ROOT later |

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
- [ ] Split app: archive includes splits; restore installs base APK only (documented in UI)
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

## M4 manual acceptance (HyperOS)

Code on `m4-privilege`. Needs Shizuku or ShizukuPlus installed for privileged path; Tier NONE must still work:

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
- [ ] Signature WARNUNG update keeps Install button disabled
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
  M0[M0 Setup] --> M1[M1 Apps Export]
  M1 --> M2[M2 Sources Match]
  M2 --> M3[M3 Installer]
  M3 --> M4[M4 Privilege]
  M4 --> M5[M5 Storage]
  M4 --> M6[M6 Debloat]
  M5 --> M7[M7 Backup]
  M6 --> M7
  M7 --> M8[M8 AutoUpdate]
  M8 --> M9[M9 Termux]
  M9 --> M10[M10 Release]
```

1. Finish **M0** → merge → install debug APK on HyperOS phone.
2. **M1** for immediate personal value (list + export) — code Done; finish device checklist above.
3. **M2–M3** for updates without leaving Kiln.
4. **M4** unlocks silent install, cache clear, later debloat/backup depth.
5. Ship when M0–M5 feel solid; treat M6+ as stretch.

Backlog ideas (P1–P3) live in [`PLAN.md`](./PLAN.md) §12 — do not pull them into the current milestone.

---

## Docs layout

| Path | Role |
| --- | --- |
| [`PLAN.md`](./PLAN.md) | Vision, architecture, feature specs, security, backlog |
| [`ROADMAP.md`](./ROADMAP.md) | This file — status and order |
| [`prompts/`](./prompts/) | Copy-paste agent prompts per milestone |
| [`reference.md`](./reference.md) | Local reference checkouts + SPDX (read-only / clean-room) |
