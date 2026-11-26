# M57 — Debloat batch per user

> Branch `m57-debloat-per-user`. After M56. When the census lists ≥2 users, let the debloat batch target a chosen user id.

## Intent

M55/M56 already wire `--user N` into install/uninstall, component toggles, and freeze. This milestone extends the same census + picker pattern to the **debloat batch**: `pm disable-user`, package `pm enable`, uninstall-for-user, and `pm install-existing`. Snapshot restore stays on the current user (no extra picker).

## In scope

- Reuse M53 census and `DualUserInstallGate`; do not invent users
- Allowlist: optional trailing numeric `--user` for `PM_DISABLE_USER` and `PM_ENABLE` (package); reuse existing user args for uninstall / install-existing
- Confirm dialog shows a user picker only when DEBLOAT + ≥2 census users
- Default selection = current user; never silently switch users
- DE + EN strings; activity log notes target user when ≠ current
- Unit tests: allowlist rejects bad ids; missing user → `"0"`
- Docs: ROADMAP M57 Done; capability matrix; PLAN §12 dual-user debloat Done; cut **0.2.11**

## Out of scope

- Backup / restore per user, ROOT `/data/data`, cloud sync
- Tablet layouts, F-Droid refile, Dual Apps provisioning
- Snapshot restore user picker (uses current user only)

## Done when

- Single-user: debloat UX unchanged (no picker)
- Multi-user + Shizuku: confirm can target another listed id; bad id refused by allowlist
- `spotlessCheck detekt lint testDebugUnitTest assembleDebug` green
