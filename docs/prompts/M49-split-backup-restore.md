# M49 — Split backup restore

> Branch `m49-split-backup-restore`. After M48. Uses M39 `LocalInstallRequest` multi-path install.

## In scope

- Restore enqueues **all** extracted APKs (base first) via `LocalInstallRequest.apkAbsolutePaths`
- Abort with honest copy when `hasSplits` but archive has only one APK
- Update Backup honesty / confirm / list strings (DE + EN); M7 checklist note
- Unit tests: `BackupRestoreApks` + multi-APK archive extract paths

## Out of scope

- ROOT `/data/data`; cloud backup; dual-user; F-Droid submit

## Done when

- Split restore queues every APK path; incomplete-split archives fail closed
- `spotlessCheck detekt lint testDebugUnitTest assembleDebug` green
