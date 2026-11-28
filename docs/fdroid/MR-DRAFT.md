# F-Droid fdroiddata MR draft (Kiln)

**Do not open before ≈ 2027-02-08.** First public commit: `2026-11-08` → Inclusion Policy Quality §6 age window. Closed attempt: [fdroiddata!58920](https://gitlab.com/fdroid/fdroiddata/-/merge_requests/58920).

## How to submit (on/after the date)

1. Fork [fdroid/fdroiddata](https://gitlab.com/fdroid/fdroiddata) (public fork, unprotected branch).
2. Branch name: `dev.involvex.kiln`.
3. Add **only** `metadata/dev.involvex.kiln.yml` (copy from [dev.involvex.kiln.yml](./dev.involvex.kiln.yml); bump `commit` / version to the latest tagged release if newer).
4. Open MR titled **`New app: Kiln`**, paste the Abstract + Checklist below (Default template), label New App.
5. Optional: open an [RFP](https://gitlab.com/fdroid/rfp/-/issues) first and put `Closes rfp#…` in the MR.
6. Do **not** attach Fastlane summary/description/images in the MR — they live in this upstream repo.

---

## Abstract for new app inclusion

### Please introduce the functions and usage of your app in summary, and also attach some screenshots.

Kiln (`dev.involvex.kiln`) is an Apache-2.0 **device package workstation** for Android (minSdk 26). Install from source builds or GitHub Releases today; this MR adds it to the official F-Droid catalogue.

In one app (no Play Services, no analytics):

- Multi-repo **F-Droid index** clients plus **GitHub / GitLab / Codeberg** release sources, with signature-aware updates (never auto-install on signer mismatch; auto-install off by default)
- APK inspect / split install / merge / resign tooling
- Storage cleanup with trash + treemap of large consumers
- Backup / restore (`.kilnbackup.zip`, optional AES-GCM; SAF + HTTPS WebDAV; optional Shizuku Android/data+obb; optional Magisk `/data/data`)
- Debloat with snapshots / denylist (ratings catalog empty until a license-cleared source; UNKNOWN + expert mode)
- Optional Shizuku: silent install, component toggles, process/diagnostics dumps, dual-user census
- Optional Termux `RUN_COMMAND` hooks (off by default)
- Phone + tablet/foldable layouts (NavigationRail / list-detail)

Screenshots: upstream Fastlane `fastlane/metadata/android/en-US/images/phoneScreenshots/` (also icon). Privacy: https://involvex.github.io/kiln/PRIVACY.md

### Please elaborate why you wrote this app. Please highlight the unique functions it provides over other similar apps in F-Droid.

I wanted one FLOSS sideload workstation instead of juggling Obtainium + an F-Droid client + App Manager + a backup tool. Closest F-Droid peers cover **slices** of this (Obtainium ≈ release follow; F-Droid client ≈ one repo type; App Manager ≈ privileged inspect) but not this all-in-one Apache-2.0 package without Play Services or analytics.

Unique vs those slices (evidence in upstream CHANGELOG through 0.2.14+): multi-forge release sources, signature-honest update policy, APK merge/resign in-app, SAF+WebDAV backup tiers with explicit privilege honesty, Shizuku diagnostics cards, and Magisk-opt-in `/data/data` backup behind an allowlisted privilege layer.

Previous MR !58920 was closed for missing Default template, age &lt; 3 months, and uniqueness. This refile waits for the age window and uses the Default template + the pitch above.

## Checklist

### Policy

* [x] The app complies with the [inclusion policy](https://f-droid.org/docs/Inclusion_Policy). _Pay special attention to Quality point 6._ — **submitting only after ≥3 months from first commit 2026-11-08 (≈ 2027-02-08).**
* [x] The original app author has been notified (and does not oppose the inclusion). If you are not the author, please paste the link of the reply from the author. — **I am the author (involvex).**
* [x] The upstream app source code repo contains the app metadata in a Fastlane folder structure. The `en-US` locale is included (de-DE too).

### Docs

* [x] Please read [the guide](https://gitlab.com/fdroid/fdroiddata/-/blob/master/CONTRIBUTING.md) first if this is your first contribution.
* [x] Please make sure your metadata follows the best practice in [our templates](https://gitlab.com/fdroid/fdroiddata/tree/master/templates).
* [x] Please read the [Build Metadata Reference](https://f-droid.org/docs/Build_Metadata_Reference/) and make sure your metadata is valid.
* [x] Please read the [Quick Start Guide](https://f-droid.org/en/docs/Submitting_to_F-Droid_Quick_Start_Guide/).

### Merge Request Setup

* [x] The title of this merge request should follow "New app: app name" format. → **New app: Kiln**
* [ ] Please make sure your fdroiddata fork is public and your branch is not protected. _(submitter)_
* [x] Please read [our Git guide](https://gitlab.com/fdroid/wiki/-/wikis/Tips-for-fdroiddata-contributors/Git-Usage) if you don't know how to rebase your branch. Don't rebase your branch if there is no conflict.
* [x] All related fdroiddata and RFP issues have been referenced — prior closed MR !58920; optional RFP when opened.
* [x] Please only submit one app in one MR.

### Metadata

* [x] Metadata must be put in `metadata/dev.involvex.kiln.yml`.
* [x] Metadata must be a valid YAML file.
* [x] Metadata must use LF as line ending.
* [x] Don't add summary/description/changelog/images in the MR (upstream Fastlane only).
* [x] Releases are tagged and auto update is enabled (`AutoUpdateMode: Version`, `UpdateCheckMode: Tags`).
* [x] There is an issue tracker and contact info of the author.
* [x] An AuthorName must be added (`involvex`).
* [x] No external srclibs required for the app recipe.
* [ ] Enable [Reproducible Builds](https://f-droid.org/docs/Reproducible_Builds). — **Not yet:** GitHub Release uses an upload keystore (`KILN_*`); F-Droid `assembleRelease` without those env vars signs debug then F-Droid re-signs. `Binaries` / `AllowedAPKSigningKeys` intentionally unset until a reproducible rebuild against the Release APK is proven. Reason left here per template.
* [x] Setup abi splits — N/A for current APK size / single ABI packaging.
* [x] Only the latest version kept in metadata before merge.
* [x] Don't add any disabled versions in the metadata.
* [x] The `commit` field is the full hash (see recipe; not a tag name).

### Pipeline

* [ ] All pipelines should pass. _(after CI)_
* [ ] All warnings and errors in the Reports tab should be fixed or explained. _(after CI)_
* [x] F-Droid CI runners are under GitLab's FOSS program — no paid CI from submitter.

---

Closes rfp# _(fill if RFP opened)_

/label ~"New App"
