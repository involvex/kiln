# M58 — Backup / restore user targeting

> Branch `m58-backup-per-user`. After M57. When the census lists ≥2 users, let restore (and multi-user backup confirm) target a chosen user id.

## Intent

M55 already wires `--user N` into install. Restore currently enqueues a local install without a user picker. This milestone adds the same census + `DualUserInstallGate` pattern to **restore confirm** (and a light **backup confirm** when multi-user) so the install queue gets `targetUserId`. APK bytes stay package-wide; Shizuku `Android/data`+obb stay **current-user paths** (no `/data/user/N` trees).

## In scope

- Reuse M53 census and `DualUserInstallGate`; do not invent users
- Restore confirm: user picker only when `SILENT_INSTALL` + ≥2 census users; pass `targetUserId` into `LocalInstallRequest` / install queue
- Multi-user backup confirm: record `targetUserId` in archive metadata + activity log (honesty); default = current user
- Single-user / Tier NONE without silent install: UX unchanged (no picker; current user only)
- DE + EN strings; activity log notes target user when ≠ current
- Unit tests: metadata encode/decode with optional `targetUserId`; missing field → null / current-user default
- Docs: ROADMAP M58 Done; capability matrix; PLAN §12 backup-per-user Done (best-effort); cut **0.2.12**

## Out of scope

- ROOT `/data/data`, cloud/WebDAV
- True multi-user Android/data copy (`/data/user/N/...`)
- Tablet layouts, F-Droid refile, Dual Apps provisioning

## Done when

- Single-user: backup/restore UX unchanged (no picker)
- Multi-user + Shizuku silent install: restore confirm can target another listed id; install queue receives that id
- Backup metadata may note intended user; data tier paths unchanged
- `spotlessCheck detekt lint testDebugUnitTest assembleDebug` green
