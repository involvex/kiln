# Prompt M10 — Polish & Release

> Voraussetzung: M9 gemergt (oder parallel auf `main` + rebase). Branch `m10-polish`. Regeln: `AGENTS.md`. Specs: `docs/PLAN.md` §8, `docs/ROADMAP.md`, `docs/RELEASE.md`.

---

Du arbeitest am Android-Projekt **Kiln**. Bearbeite **ausschließlich Meilenstein M10** (kein §12-Backlog).

## Scope

- Version bump (`KilnAndroid.kt`), CHANGELOG, privacy policy
- Settings → About (version, license, privacy, source links) + basic a11y labels
- F-Droid fastlane metadata (en-US + de-DE); screenshot slots documented
- GitHub Actions tag release workflow + optional upload-keystore secrets
- README / ROADMAP capability matrix

## Nicht tun

- Keine neuen Feature-Module, kein Debloat-Katalog aus dem Gedächtnis, keine Telemetrie
- Keine Keystores committen; Screenshots erst nach Geräte-Capture

## Akzeptanz

- [ ] About shows versionName/versionCode
- [ ] Links open source / privacy / license
- [ ] `fastlane/metadata/android/` present
- [ ] Tag `v*` workflow assembles release APK
- [ ] Gates green; docs updated
