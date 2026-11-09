# Releasing Kiln

## Version

`versionCode` / `versionName` live in
`build-logic/convention/.../KilnAndroid.kt`. Bump both for every store or
GitHub release. Keep [CHANGELOG.md](../CHANGELOG.md) in sync.

## GitHub Release (CI)

1. Merge the release PR to `main`.
2. Tag: `git tag -a v0.1.0 -m "Kiln 0.1.0" && git push origin v0.1.0`
3. Workflow [`.github/workflows/release.yml`](../.github/workflows/release.yml)
   builds a release APK and attaches it to the GitHub Release.
4. Docs site (optional): after `docs.yml` runs, enable **Settings → Pages → GitHub Actions**.
   Site URL: `https://involvex.github.io/kiln/`

### Signing secrets (optional but recommended)

Without secrets, CI signs the “release” APK with the **debug** keystore so
sideloaders still get an artifact (label it clearly). For production uploads
add repository secrets:

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

Without all four, the release APK stays debug-signed. The workflow already reads `KILN_STORE_FILE_BASE64` in [`.github/workflows/release.yml`](../.github/workflows/release.yml).

## F-Droid

- Metadata scaffold: [`fastlane/metadata/android/`](../fastlane/metadata/android/)
- F-Droid builds from source; they do not need your upload keystore
- Provide screenshots under `fastlane/metadata/android/<locale>/images/phoneScreenshots/` before submission (not committed until captured on device)
- Application id: `dev.involvex.kiln` · License: Apache-2.0

## Manual local release build

```bash
./gradlew.bat assembleRelease
# APK: app/build/outputs/apk/release/app-release.apk (or -unsigned)
```

## Checklist

- [ ] CHANGELOG section for the version
- [ ] versionName / versionCode bumped
- [ ] `spotlessCheck detekt lint testDebugUnitTest assembleRelease` green
- [ ] Tag pushed; Release notes pasted from CHANGELOG
- [ ] Privacy policy URL still valid (`docs/PRIVACY.md`)
