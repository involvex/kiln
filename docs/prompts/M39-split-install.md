# M39 — Split APK install (bundles)

> Branch `m39-split-install`. After M38 (#63) + CI lint harden (#65).

## In scope

- Install one archive: `.apks` / `.xapk` / `.apkm` (and single `.apk`)
- Manage → APK tools **Install** mode (SAF `OpenDocument`)
- Share / `VIEW` into Kiln for those MIME/types
- `PackageInstaller` multi-session (signatures preserved); device-aware split filter
- Silent `pm install-multiple` when Shizuku + preferred; fallback to session
- DE + EN; unit tests; quality gates; ROADMAP/PLAN/capability matrix

## Out of scope

- Loose multi-select APKs / `SEND_MULTIPLE`
- OBB; bundletool `toc.pb`
- Changing F-Droid/GitHub download pipeline to prefer splits
- Publishing / dual-user / HyperOS checklist sweep

## Done when

- [x] SAF Install mode unpacks `.apks`/`.xapk`/`.apkm`, filters splits, enqueues multi-APK session
- [x] Share / VIEW of a bundle opens Manage → APK tools Install
- [x] Single `.apk` sideload path retained
- [x] Silent `pm install-multiple` when capable; PackageInstaller fallback
- [x] `spotlessCheck detekt lint testDebugUnitTest assembleDebug` green locally
- [ ] Manual HyperOS: SAF `.apks`, Share-to, single `.apk` regression (user device)
