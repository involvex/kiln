# M27 — Permission audit

> Branch `m27-permission-audit`. PLAN §12 P2 (dangerous overview + AppOps / bulk revoke). Also mark M26 Done (#40).

## In scope

- Settings entry → full-screen Permission audit (like Debloat)
- Tier NONE overview: apps with dangerous+granted permissions (search/filter)
- Privilege-gated `pm revoke` (single + bulk) with confirm
- Basic `appops set` with fixed op/mode allowlists
- `PrivilegeCapability.MANAGE_APP_OPS`; missing capability → PrivilegeSetup CTA
- Mark M26 Done (#40) in ROADMAP / README / capability matrix

## Out of scope

- Full AppOps editor, `appops reset`, re-grant, special permissions
- Freeze/hibernate, Manage tab, automation/widget revoke
- M28 Storage phase 2

## Done when

- Overview lists dangerous granted perms without privilege
- Revoke / AppOps require confirm + `MANAGE_APP_OPS`
- Without privilege, UI opens PrivilegeSetup instead of silent failure
- `spotlessCheck detekt lint testDebugUnitTest assembleDebug` green
