# Store listing

Kiln stays off Google Play. A Play Console account costs a one-time US$25 fee, and the free Android Developer Console plan only reaches 20 devices. It does not publish a public store listing.

Free listings to file (phone screenshots are in the repo):

- **IzzyOnDroid** ships the upload-signed GitHub Release APK. This is the faster listing.
- **F-Droid** rebuilds `v0.2.7` from source and signs its own copy. Review takes longer. A listing on one store does not remove the other.

Obtainium needs no request. People add `https://github.com/involvex/kiln` themselves.

Do not open store accounts from a shared session. The APK to submit is the GitHub asset, not a local debug-signed build:

`https://github.com/involvex/kiln/releases/download/v0.2.7/kiln-0.2.7.apk`

Tagged releases fail unless all four `KILN_*` upload-keystore secrets are set, so that asset is upload-signed.

## Already in the repo

- Apache-2.0, public source, no analytics or proprietary tracking SDK
- Fastlane en/de title, short description, full description, and `changelogs/10.txt`
- Icon: `fastlane/metadata/android/en-US/images/icon.png` (512×512)
- Phone screenshots: `fastlane/metadata/android/en-US/images/phoneScreenshots/` (Apps, Discover, Sources, Settings, App detail; Manage/Storage optional)
- Privacy policy: `https://involvex.github.io/kiln/PRIVACY.md`
- Auto-install of updates is off until the user turns it on. Signature mismatches are never installed automatically
- Draft recipe: [fdroid/dev.involvex.kiln.yml](./fdroid/dev.involvex.kiln.yml)

## Why the sensitive permissions are there

Izzy flags these until the maintainer explains them. Paste this with the inclusion request:

| Permission | Why Kiln has it |
| --- | --- |
| `QUERY_ALL_PACKAGES` | The app is a package manager. The installed-app list is its main screen. The list stays on the device. |
| `REQUEST_INSTALL_PACKAGES` | Updates and restores install through Android's package installer, which asks the user to confirm. This is the same need as other F-Droid clients. |
| `PACKAGE_USAGE_STATS` | Storage sizes. The system Usage Access screen grants it. Storage numbers stay empty until the user allows it. |

`com.termux.permission.RUN_COMMAND` is optional and off until the user opens the Termux screen and grants it. Shizuku permissions apply only after the user sets Shizuku up.

## IzzyOnDroid request

Open a public **App Inclusion Request** on Codeberg: [IzzyOnDroid/repo issues](https://codeberg.org/IzzyOnDroid/repo/issues). Use their template. Paste:

```text
App: Kiln
Application ID: dev.involvex.kiln
License: Apache-2.0
Source: https://github.com/involvex/kiln
Release APK: https://github.com/involvex/kiln/releases/download/v0.2.7/kiln-0.2.7.apk
Summary: Package manager, APK tools, storage cleanup, and optional Shizuku helpers
Fastlane: fastlane/metadata/android/ (en-US and de-DE), including images/icon.png
Privacy: https://involvex.github.io/kiln/PRIVACY.md

Kiln lists installed apps, checks F-Droid and GitHub for updates, exports and backs up APKs, and can clean storage. Shizuku and root are optional. There is no analytics SDK. It is not the macOS "Kiln" emulator.

QUERY_ALL_PACKAGES: required to list installed apps. The list is not uploaded.
REQUEST_INSTALL_PACKAGES: system installer for updates and restores. Auto-install is off by default and never accepts a signature mismatch.
PACKAGE_USAGE_STATS: optional storage sizes, granted only from the system Usage Access screen.

Not yet submitted to F-Droid. A draft fdroiddata recipe is in docs/fdroid/dev.involvex.kiln.yml and will be filed after this listing's screenshots are in the repo.
```

If the template asks whether the app downloads extra executables: installs happen only when the user asks (or after they opt in to auto-install of signature-matching updates). Kiln does not ship a silent self-updater.

## F-Droid request

After screenshots are committed and `main` contains this packet:

1. Fork [fdroiddata](https://gitlab.com/fdroid/fdroiddata).
2. Branch `dev.involvex.kiln` from `master`.
3. Add `metadata/dev.involvex.kiln.yml` with the contents of [fdroid/dev.involvex.kiln.yml](./fdroid/dev.involvex.kiln.yml).
4. Open the merge request. Let fdroiddata CI build `v0.2.7`.
5. If CI reports a recipe error (JDK, output path, tag pattern), fix that yml only. Do not claim a reproducible build. `Binaries` stays unset until a rebuild matches `kiln-0.2.7.apk`.

F-Droid builds from the git tag. It does not need the upload keystore. The buildserver currently knows Gradle 9.9.1, which is the wrapper this repo uses.
