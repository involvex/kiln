# M36 — Freeze / hibernate

> Branch `m36-freeze-hibernate`. After M35 (#56). PLAN §12 P2 (Apps einfrieren/hibernieren).

## In scope

- Freeze / unfreeze user apps via `pm suspend` / `pm unsuspend` through `PrivilegedExecutor` + ShellAllowlist only
- Gate on `PrivilegeCapability.DEBLOAT` (same shell tier as Debloat; UI copy distinguishes suspend vs disable-user)
- Settings entry next to Background controls: searchable list, Frozen / Active / All filter, confirmation before freeze
- Read suspended state via `ApplicationInfo.FLAG_SUSPENDED` (Tier NONE display)
- Activity log; DE + EN; unit tests for allowlist + filters
- Docs: ROADMAP M36; PLAN §12 P2 In progress → Done on merge

## Out of scope

- Dual-user / `--user`, auto-hibernate heuristics, Aurogon/Millet/FCM hacks
- GPL App Manager code; tablet layouts; publishing

## Done when

- [ ] Freeze suspends app; Unfreeze restores; honest gate without privilege
- [ ] Confirm dialog before freeze; empty filter state
- [ ] `spotlessCheck detekt lint testDebugUnitTest assembleDebug` green
