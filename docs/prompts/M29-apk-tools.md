# M29 — APK tools phase 2 (inspect + compare)

> Branch `m29-apk-tools`. PLAN §12 P3 (APK-Werkzeuge Phase 2 — first slice). Also refresh ROADMAP after polish [#45](https://github.com/involvex/kiln/pull/45).

## In scope

- Shared `ApkSigningReader` (X.509 cert `encoded` SHA-256, history-aware); use from installer gate + apps detail
- Manage → **APK tools** tab: SAF inspect `.apk` / `.apks` / `.xapk` (package, version, SDK, permissions, signers, ZIP members)
- Compare signatures: APK A vs APK B, or APK vs installed package
- Unit tests; DE + EN strings
- Docs: ROADMAP M29 In progress → Done on merge; backlog APK tools 2 partial (merge/resign deferred)

## Out of scope

- Merge splits into one APK; resign with `apksig` / user keystore (→ M29b / APK tools 2b)
- Multi-split PackageInstaller sessions
- Freeze/hibernate, dual-user, tablet, mirrors/proxy (M30+)

## Done when

- Manage → APK tools inspects a picked archive and shows meta + signers
- Two-APK / APK-vs-installed compare shows match or mismatch
- Shared signing reader used by inspector and `ApkSignatureGate`
- `spotlessCheck detekt lint testDebugUnitTest assembleDebug` green
