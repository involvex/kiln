# Prompt M47 — Polish, stability, signed 0.2.5

> Branch `m47-polish-signed-release`. After M46 (#81). Rules: `AGENTS.md`. Specs: `docs/RELEASE.md`, `docs/ROADMAP.md`.

---

You work on the Android project **Kiln**. Handle **only milestone M47** (no §12 backlog, no HyperOS checklist).

## Scope

- Version **0.2.5** / versionCode **8**; CHANGELOG rollup of post-`v0.2.4` work
- GitHub Release workflow: **require** all four `KILN_*` upload-keystore secrets; attach `kiln-<version>.apk`; `apksigner verify --print-certs`
- Package icon Coil path: decode to request size, `DataSource.MEMORY`, `ImageLoader.crossfade(false)`
- Docs: `RELEASE.md`, `README`, `ROADMAP` capability/status

## Do not

- Remove `KilnApplication` startup `runBlocking` locale apply (HyperOS relaunch race)
- F-Droid submit / screenshots / dual-user / tablet
- Tag or `adb install` without explicit user OK
- New feature modules

## Acceptance

- [ ] `versionName` / `versionCode` are 0.2.5 / 8
- [ ] Tagged release workflow fails without upload secrets (no debug-signed GitHub APK)
- [ ] Release asset named `kiln-0.2.5.apk`; body states upload-keystore signed
- [ ] Package icon unit test for downscale/cap green
- [ ] `spotlessCheck detekt lint testDebugUnitTest assembleRelease` green
- [ ] README / ROADMAP / CHANGELOG updated
