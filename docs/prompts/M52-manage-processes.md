# M52 — Processes inside Manage / Settings

> Branch `m52-manage-processes`. After M51. Surfaces widget process list in-app.

## In scope

- Settings → Tools (or Manage) entry: process list reusing meminfo snapshot + `ForceStopConfirmGate`
- Confirm before `am force-stop`; refuse Kiln itself
- Tier NONE: show privilege setup reason (same as Processes widget)
- No new shell commands; no `dumpsys` on main / BroadcastReceiver
- DE + EN; unit tests for gate reuse if new wrappers
- Docs: ROADMAP / capability matrix

## Out of scope

- New dumpsys formats; auto-kill heuristics; dual-user

## Done when

- In-app list shows mem/CPU when capable; force-stop confirms and refreshes
- `spotlessCheck detekt lint testDebugUnitTest assembleDebug` green
