# M59 — HyperOS phone polish

> Branch `m59-hyperos-polish`. After M58 / `v0.2.12`. Phone-first polish: dual-user honesty + docs sync. No tablet / cloud / ROOT.

## Intent

Dual-user (M53–M58) is shipped. This milestone cleans honesty copy and roadmap status for a HyperOS phone release cut, and leaves room for device-found regressions.

## In scope

- Fix stale census copy that still claims “installs only for user 0”
- Align dual-user picker bodies (install / component / freeze / debloat / backup) with best-effort wording EN+DE
- ROADMAP / PLAN / README / CHANGELOG: M59 Done; dual-user theme closed; later = tablet / cloud / ROOT
- Short manual HyperOS checklist in the prompt (not a CI gate)
- Version **0.2.13** (versionCode 16)

## Out of scope

- Full M1–M8 checklist sweep as a gate
- Tablet / NavigationRail
- Cloud backup, ROOT `/data/data`, F-Droid refile
- New features

## Manual HyperOS checklist (optional)

- [ ] Settings → Privilege setup A→D; silent install Available when Shizuku ready
- [ ] Settings → Check now: no crash
- [ ] Backup: APK archive + restore confirm; single-user = no user picker
- [ ] Debloat: disable one harmless package with confirm
- [ ] Storage: list loads after usage access

## Done when

- Stale “user 0 only” copy gone; dual-user strings mention best-effort
- Docs/version green; `spotlessCheck detekt lint testDebugUnitTest assembleDebug` green
