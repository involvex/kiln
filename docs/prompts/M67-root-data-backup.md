# M67 — ROOT `/data/data` backup

> Branch `m67-root-data-backup`. RootExecutor + libsu (opt-in). CE `/data/data` only.

## In scope

- JitPack + `libsu` core/service; `RootExecutor` behind `PrivilegedExecutor` only
- Magisk root opt-in (default off) in Privilege setup
- Allowlist: `TAR_APP_DATA_OUT` / `TAR_APP_DATA_IN` / `RESTORECON_APP_DATA` / `LS_APP_DATA`
- Backup packs `app_data.tar` when `FULL_BACKUP`; restore: install → force-stop → extract → restorecon
- Honesty strings DE + EN; unit tests for allowlist + zip round-trip

## Out of scope

- `/data/user_de`, R2/S3, invented debloat, GPL copy, adb install without ask

## Done when

- [ ] ROOT tier packages `/data/data` when capable; SHIZUKU/NONE honesty unchanged otherwise
- [ ] Root opt-in default off; no direct `su` outside `:core:privilege`
- [ ] `spotlessCheck detekt lint testDebugUnitTest assembleDebug` green
