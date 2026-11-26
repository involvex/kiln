# M56 — Per-user component toggles and freeze

> Branch `m56-per-user-state`. After M55. When the census lists ≥2 users, let component enable/disable and freeze/unfreeze target a chosen user id.

## Intent

M55 already wires `--user N` into install/uninstall. This milestone extends the same census + picker pattern to **one-component** `pm enable`/`disable` and `pm suspend`/`unsuspend`. Debloat batch (`disable-user` / package enable) stays on user 0.

## In scope

- Reuse M53 census and `DualUserInstallGate`; do not invent users
- Allowlist: optional trailing numeric `--user` for component enable/disable and suspend/unsuspend
- Confirm dialogs show a user picker only when DEBLOAT + ≥2 census users
- Default selection = current user; never silently switch users
- DE + EN strings; activity log notes target user when ≠ current
- Unit tests: allowlist rejects bad ids; default missing user → `"0"`
- Docs: ROADMAP M56 Done; capability matrix; dual-user Partial → Done for component/freeze

## Out of scope

- Debloat batch per user
- Backup / restore per user, ROOT `/data/data`, cloud sync
- Tablet layouts, F-Droid refile, Dual Apps provisioning
- Version tag `0.2.10` (separate release cut)

## Done when

- Single-user: component and freeze UX unchanged (no picker)
- Multi-user + Shizuku: confirm can target another listed id; bad id refused by allowlist
- `spotlessCheck detekt lint testDebugUnitTest assembleDebug` green
