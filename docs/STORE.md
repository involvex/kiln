# Store listing

Kiln stays off Google Play. A Play Console account costs a one-time US$25 fee, and the free Android Developer Console plan only reaches 20 devices. It does not publish a public store listing.

## Current distribution (decision)

**Primary path: GitHub Releases + Obtainium.** No store approval. Users add `https://github.com/involvex/kiln` in Obtainium (or install the Release APK directly).

| Channel | Status | Notes |
| --- | --- | --- |
| GitHub Releases | Active | Upload-signed APK via `KILN_*` secrets, e.g. `https://github.com/involvex/kiln/releases/download/v0.2.14/kiln-0.2.14.apk` |
| Obtainium | Active | No request; point at the GitHub repo |
| IzzyOnDroid | Closed | Declined under [AI policy](https://izzyondroid.org/docs/general/AppInclusionPolicy/) ([repodata#948](https://codeberg.org/IzzyOnDroid/repodata/issues/948)). Do not refile with a lower assistance level. |
| F-Droid main repo | Wait until ≈ **2027-02-08** | [fdroiddata!58920](https://gitlab.com/fdroid/fdroiddata/-/merge_requests/58920) closed by `linsui`: MR template not used; app &lt; 3 months old ([Inclusion Policy](https://f-droid.org/docs/Inclusion_Policy/) Quality §6); “no unique features.” First commit **2026-11-08** → earliest refile ≈ **2027-02-08**. Prep kit: [`docs/fdroid/`](./fdroid/). |
| Self-hosted F-Droid repo | Deferred | Free to host; not worth the ops cost while Obtainium covers updates. See below. |
| Google Play | Skipped | US$25 + policy overhead |

## Already in the repo

- Apache-2.0, public source, no analytics or proprietary tracking SDK
- Fastlane en/de title, short description, full description, and changelogs
- Icon: `fastlane/metadata/android/en-US/images/icon.png` (512×512)
- Phone screenshots: `fastlane/metadata/android/en-US/images/phoneScreenshots/`
- Privacy policy: `https://involvex.github.io/kiln/PRIVACY.md`
- Auto-install of updates is off until the user turns it on. Signature mismatches are never installed automatically
- Release builds set `dependenciesInfo.includeInApk/includeInBundle = false` (no Google dependency-metadata blob)
- F-Droid refile kit (M69): [fdroid/dev.involvex.kiln.yml](./fdroid/dev.involvex.kiln.yml) + [fdroid/MR-DRAFT.md](./fdroid/MR-DRAFT.md)

## Why the sensitive permissions are there

| Permission | Why Kiln has it |
| --- | --- |
| `QUERY_ALL_PACKAGES` | The app is a package manager. The installed-app list is its main screen. The list stays on the device. |
| `REQUEST_INSTALL_PACKAGES` | Updates and restores install through Android's package installer, which asks the user to confirm. This is the same need as other F-Droid clients. |
| `PACKAGE_USAGE_STATS` | Storage sizes. The system Usage Access screen grants it. Storage numbers stay empty until the user allows it. |

`com.termux.permission.RUN_COMMAND` is optional and off until the user opens the Termux screen and grants it. Shizuku permissions apply only after the user sets Shizuku up.

## IzzyOnDroid (closed)

See [repodata#948](https://codeberg.org/IzzyOnDroid/repodata/issues/948). Inclusion was declined because declared AI assistance was above their threshold. They still ran Cronos on `v0.2.7` and pointed us at F-Droid.

## F-Droid main repo

Closed MR: [fdroiddata!58920](https://gitlab.com/fdroid/fdroiddata/-/merge_requests/58920#note_4310272049).

Do **not** reopen until:

1. **≥3 months** of public history (first commit **2026-11-08** → window opens ≈ **2027-02-08**), **or** a clear “proven users” exception with evidence.
2. The official **Default** MR template is filled (Abstract + Checklist), including a uniqueness pitch — use [fdroid/MR-DRAFT.md](./fdroid/MR-DRAFT.md).
3. Recipe uses a **full commit SHA** in `commit:` (template forbids tag/branch there) — refresh [fdroid/dev.involvex.kiln.yml](./fdroid/dev.involvex.kiln.yml) to the latest tag before opening.

### Uniqueness pitch (for the MR Abstract)

> Kiln is an Apache-2.0 **device package workstation**, not another GitHub APK follower. In one app it combines: multi-repo F-Droid index clients + GitHub/GitLab/Codeberg Releases, signature-aware updates (never auto-install on mismatch), APK inspect/merge/resign/split install, storage cleanup with trash + treemap, backup/restore (SAF + HTTPS WebDAV; optional Shizuku Android/data; optional Magisk `/data/data`), optional Shizuku diagnostics / component control, and optional Termux `RUN_COMMAND`. Closest F-Droid peers cover slices of this (Obtainium ≈ update follow; F-Droid client ≈ one repo type; App Manager ≈ privileged inspect) but not this FLOSS all-in-one without Play Services or analytics.

Optional first step: open an [RFP](https://gitlab.com/fdroid/rfp/-/issues) before the fdroiddata MR. Soft appeal (`team@f-droid.org`) is only worth it with users + uniqueness evidence — not to waive the age rule.

Recipe / CI notes from !58920 (reuse): Summary/description come from upstream Fastlane (do not duplicate in the MR); `AutoUpdateMode: Version`; `UpdateCheckData` points at `KilnAndroid.kt`; auto-update fields after `MaintainerNotes`.

## Self-hosted F-Droid repo (deferred)

**Can it be free?** Yes. Typical stack: `fdroidserver` locally or in GitHub Actions → publish `repo/` (index + APKs or index pointing at Release URLs) to **GitHub Pages**, Codeberg Pages, or Cloudflare Pages. No hosting fee for a one-app repo.

**Effort (rough):**

| Phase | Effort | What |
| --- | --- | --- |
| One-time setup | ~4–8 hours | `fdroid init`, separate **repo signing key** (keep forever), metadata, Pages site, document “add this repo URL + fingerprint” |
| Automation | ~2–4 hours | Workflow on `release`: copy APK → `fdroid update` → deploy Pages |
| Ongoing | ~15–30 min / release (or fully automated) | Key backup, Pages size/bandwidth, fingerprint in README |

**Why defer:** Obtainium already gives signed updates from GitHub Releases with zero extra infrastructure. A private repo mainly helps people who insist on the F-Droid *client* UX; it does **not** put Kiln in the official catalogue and does **not** replace maintainer review later. Revisit only if users ask for an `fdroidrepos:` URL.

## Install blurb (README / site)

```text
Install: download the APK from GitHub Releases, or add
https://github.com/involvex/kiln
in Obtainium.
```
