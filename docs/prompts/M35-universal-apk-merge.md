# M35 — Universal APK merge

> Branch `m35-universal-apk-merge`. After M34 (#53). PLAN §12 P3 remaining slice (true universal merge after M29/M31).

## In scope

- Manage → APK tools: **Merge** mode for `.apks` / `.xapk` / `.apkm` → one installable APK
- Engine: Maven `io.github.reandroid:ARSCLib` + Kiln `UniversalApkMerger` (APKEditor-style merge, Apache-2.0; no GPL AntiSplit-M)
- Always clear `META-INF` and resign via existing M31 `ApkResign` / user keystore before SAF save
- DE + EN; unit tests for kind/helpers; docs ROADMAP/PLAN/README

## Out of scope

- Auto-install after merge; Termux/apktool path; decode/rebuild UI; generating keystores
- Freeze/hibernate; Coil; Exodus follow-ups; store version bump unless asked

## Done when

- [ ] Merge + resign flow: pick bundle + keystore → signed universal APK saved
- [ ] Plain `.apk` rejected; errors for unsupported/protected/OOM are clear
- [ ] Confirmation that resign breaks original update path
- [ ] `spotlessCheck detekt lint testDebugUnitTest assembleDebug` green
