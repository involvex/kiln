# M51 — Component enable / disable

> Branch `m51-component-toggles`. After M50. Extends App Detail component lists.

## In scope

- App Detail: show enabled state for services / receivers / providers (exported + non-exported; non-exported stay read-only)
- With Shizuku (`DEBLOAT` capability): confirm then `pm enable` / `pm disable` one component via `PrivilegedExecutor` + allowlisted argv (`pkg/class` only)
- Hard denylist: reuse Debloat hard + dynamic launcher / IME / dialer / SMS; refuse Kiln self
- Tier NONE: lists stay read-only with privilege setup CTA
- Activity log on each change; DE + EN; unit tests for allowlist + denylist gate
- Docs: ROADMAP / capability matrix

## Out of scope

- Shizuku `am start` for non-exported activities
- Dual-user / `--user N` installs
- GPL App Manager UI copy

## Done when

- Enable/disable works for a harmless exported component with Shizuku; denylist blocks protected packages
- Without privilege: read-only + setup path
- `spotlessCheck detekt lint testDebugUnitTest assembleDebug` green
