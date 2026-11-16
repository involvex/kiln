# M40 — Backup phase 2 (SAF + Shizuku data)

> Branch `m40-backup-phase2`. After `v0.2.4` (#68) / M39 (#66).

## In scope

- Persistable SAF tree destination + keep-N rotation per package
- Real SHIZUKU tier: APK(s) + `Android/data/<pkg>` + `Android/obb/<pkg>` when `ACCESS_ANDROID_DATA`
- Optional AES-GCM password encryption (PBKDF2); metadata/format v2
- NONE path remains APK-only; honest UI; DE + EN; unit tests; quality gates

## Out of scope

- ROOT `/data/data` / `user_de` / `restorecon`
- Cloud / WebDAV / R2
- F-Droid submit; HyperOS checklist sweep; dual-user; tablet layouts

## Done when

- [x] SAF tree pick/clear; backups write under `pkg/` with rotation
- [x] Shizuku backup includes android_data/obb when capable; restore installs APK then copies data when possible
- [x] Optional encrypt on backup; decrypt on restore; wrong password fails closed
- [x] `spotlessCheck detekt lint testDebugUnitTest assembleDebug` green locally
- [ ] Manual HyperOS checklist in PR (user device)

## Manual HyperOS checklist (PR)

- [ ] Pick persistable SAF tree; label shows; clear works
- [ ] APK-only backup (NONE / no Shizuku data) writes under `pkg/` with suggested name
- [ ] With Shizuku `ACCESS_ANDROID_DATA`: backup includes Android/data and/or OBB when present; metadata `includesAndroidData=true`
- [ ] Restore: APK install queued; shared dirs restored after package present when capable
- [ ] Encrypted archive: backup with password; restore requires password; wrong password fails closed
- [ ] Keep-N prune: after N+1 backups for one package, oldest deleted
