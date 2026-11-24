# Releasing Kiln

## Version

`versionCode` / `versionName` live in
`build-logic/convention/.../KilnAndroid.kt`. Bump both for every store or
GitHub release. Keep [CHANGELOG.md](../CHANGELOG.md) in sync.

Settings → About reads the installed APK version via `PackageManager` (no
hardcoded string).

## Automated cut (recommended)

From a clean `main` that matches the latest `v*` tag:

```powershell
# Preview (writes nothing)
.\scripts\release.ps1

# Patch bump (default), commit, annotated tag, push → Release workflow
.\scripts\release.ps1 -Yes

# Optional: -Bump minor | -Bump major
.\scripts\release.ps1 -Bump minor -Yes
```

The script:

1. Refuses a dirty tree, a non-`main` branch, or a `versionName` that does not
   match the latest `v*` tag.
2. Builds a Keep a Changelog section from `## [Unreleased]` plus conventional
   commits since that tag (`feat` → Added, `fix` → Fixed, `refactor`/`perf` →
   Changed; docs/tests/chores skipped).
3. Updates `KilnAndroid.kt`, `CHANGELOG.md`, `README.md` version lines, and the
   current-status paragraph in `docs/ROADMAP.md`.
4. With `-Yes`: commits `chore: release X.Y.Z`, creates `vX.Y.Z`, pushes branch
   and tag. CI opens the GitHub Release (the script does not call
   `gh release create`).

## GitHub Release (CI)

1. Merge feature work to `main`.
2. Run `.\scripts\release.ps1 -Yes` (or tag manually:
   `git tag -a v0.2.6 -m "Kiln 0.2.6" && git push origin v0.2.6`).
3. Workflow [`.github/workflows/release.yml`](../.github/workflows/release.yml)
   builds a **upload-keystore signed** release APK (`kiln-<version>.apk`),
   verifies it with `apksigner`, and attaches it to the GitHub Release.
4. Docs site (optional): after `docs.yml` runs, enable **Settings → Pages → GitHub Actions**.
   Site URL: `https://involvex.github.io/kiln/`

### Signing secrets (required for tagged releases)

Tagged releases **fail** if any of these repository secrets are missing.
Local `assembleRelease` without the env vars may still fall back to the
debug keystore (sideload / smoke only).

| Secret | Purpose |
| --- | --- |
| `KILN_STORE_FILE_BASE64` | Base64 of the upload/release `.jks` |
| `KILN_STORE_PASSWORD` | Keystore password |
| `KILN_KEY_ALIAS` | Key alias |
| `KILN_KEY_PASSWORD` | Key password |

Never commit keystores or `local.properties` with passwords.
Never paste secret values into chat or shell history if you can avoid it.

From the repo, with `gh` authenticated to `involvex/kiln`:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("C:\path\to\kiln-upload.jks")) |
  gh secret set KILN_STORE_FILE_BASE64 -R involvex/kiln

# Each command prompts for the value. Do not pass passwords with --body in a shared terminal log.
gh secret set KILN_STORE_PASSWORD -R involvex/kiln
gh secret set KILN_KEY_ALIAS -R involvex/kiln
gh secret set KILN_KEY_PASSWORD -R involvex/kiln
gh secret list -R involvex/kiln
```

## F-Droid and IzzyOnDroid

Filing steps, the permission note, and the draft fdroiddata recipe are in [STORE.md](./STORE.md).

- Metadata: [`fastlane/metadata/android/`](../fastlane/metadata/android/) (en-US and de-DE, including changelog `10.txt` and `images/icon.png`)
- F-Droid builds from source and does not need the upload keystore
- IzzyOnDroid ships the upload-signed GitHub Release APK
- Phone screenshots live under `fastlane/metadata/android/en-US/images/phoneScreenshots/`
- Application id: `dev.involvex.kiln` · License: Apache-2.0

## Manual local release build

```bash
./gradlew.bat assembleRelease
# APK: app/build/outputs/apk/release/app-release.apk (or -unsigned)
# Without KILN_* env vars this may be debug-signed — CI tags always require upload secrets.
```

## Checklist

- [ ] CHANGELOG section for the version
- [ ] versionName / versionCode bumped
- [ ] `spotlessCheck detekt lint testDebugUnitTest assembleRelease` green
- [ ] Tag pushed; Release workflow verifies with `apksigner` and uploads `kiln-<version>.apk`
- [ ] Privacy policy URL still valid (`docs/PRIVACY.md`)
