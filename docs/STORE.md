# Store listing

Kiln stays off Google Play. A Play Console account costs a one-time US$25 fee, and the free Android Developer Console plan only reaches 20 devices. It does not publish a public store listing.

Free listings:

- **IzzyOnDroid** — filed then **declined** under their [AI policy](https://izzyondroid.org/docs/general/AppInclusionPolicy/) ([repodata#948](https://codeberg.org/IzzyOnDroid/repodata/issues/948)). Cronos APK scan was otherwise fine aside from Google dependency-metadata (fixed in app builds). Do not refile with a lower assistance level.
- **F-Droid** — preferred free listing. Rebuilds from the git tag; no LLM policy. Recipe draft: [fdroid/dev.involvex.kiln.yml](./fdroid/dev.involvex.kiln.yml).

Obtainium needs no request. People add `https://github.com/involvex/kiln` themselves.

The APK to point reviewers at is the GitHub Release asset (upload-signed via `KILN_*` secrets), for example:

`https://github.com/involvex/kiln/releases/download/v0.2.8/kiln-0.2.8.apk`

(Adjust the version to the tag you are submitting.)

## Already in the repo

- Apache-2.0, public source, no analytics or proprietary tracking SDK
- Fastlane en/de title, short description, full description, and changelogs
- Icon: `fastlane/metadata/android/en-US/images/icon.png` (512×512)
- Phone screenshots: `fastlane/metadata/android/en-US/images/phoneScreenshots/`
- Privacy policy: `https://involvex.github.io/kiln/PRIVACY.md`
- Auto-install of updates is off until the user turns it on. Signature mismatches are never installed automatically
- Release builds set `dependenciesInfo.includeInApk/includeInBundle = false` (no Google dependency-metadata blob)
- Draft recipe: [fdroid/dev.involvex.kiln.yml](./fdroid/dev.involvex.kiln.yml)

## Why the sensitive permissions are there

| Permission | Why Kiln has it |
| --- | --- |
| `QUERY_ALL_PACKAGES` | The app is a package manager. The installed-app list is its main screen. The list stays on the device. |
| `REQUEST_INSTALL_PACKAGES` | Updates and restores install through Android's package installer, which asks the user to confirm. This is the same need as other F-Droid clients. |
| `PACKAGE_USAGE_STATS` | Storage sizes. The system Usage Access screen grants it. Storage numbers stay empty until the user allows it. |

`com.termux.permission.RUN_COMMAND` is optional and off until the user opens the Termux screen and grants it. Shizuku permissions apply only after the user sets Shizuku up.

## IzzyOnDroid (closed)

See [repodata#948](https://codeberg.org/IzzyOnDroid/repodata/issues/948). Inclusion was declined because declared AI assistance was above their threshold. They still ran Cronos on `v0.2.7` and pointed us at F-Droid.

## F-Droid request

1. Fork [fdroiddata](https://gitlab.com/fdroid/fdroiddata).
2. Branch `dev.involvex.kiln` from `master`.
3. Add `metadata/dev.involvex.kiln.yml` with the contents of [fdroid/dev.involvex.kiln.yml](./fdroid/dev.involvex.kiln.yml) (keep version/tag in sync with the submitted release).
4. Open the merge request. Let fdroiddata CI build the tagged commit.
5. If CI reports a recipe error (JDK, output path, tag pattern), fix that yml only. Do not claim a reproducible build. `Binaries` stays unset until a rebuild matches the GitHub Release APK.

F-Droid builds from the git tag. It does not need the upload keystore. The wrapper is Gradle 9.9.2 (in the transparency log).
