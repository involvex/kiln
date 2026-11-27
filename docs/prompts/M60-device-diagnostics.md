# M60 — Device diagnostics (Shizuku, pretty summaries)

> Branch `m60-device-diagnostics`. After M59 / `v0.2.13`. Shizuku-only compact battery / netstats / top cards. No Root, no terminal, no AGPL copy from adbcaptain.

## In scope

- Settings → Tools → **Device diagnostics**
- Allowlisted: `dumpsys battery`, `dumpsys netstats --checkin` (truncated), `top -n 1 -m 20` (truncated)
- Parsers in `:core:model` → pretty Material 3 cards (battery / network / CPU)
- Tier NONE: explain + Privilege setup CTA
- DE + EN; unit tests for parsers + allowlist argv
- Docs: ROADMAP / PLAN §12 / capability matrix / reference.md (adbcaptain observe-only)

## Out of scope

- Root / libsu
- Raw dump toggle, widgets, force-stop on this screen
- `dumpsys cpuinfo` / thermalservice / batterystats
- `cmd battery set-level`, wifi toggles, logcat, arbitrary shell

## Done when

- [ ] Refresh with Shizuku shows three cards (or honest per-section error)
- [ ] No SSID / subscriberId in UI
- [ ] `spotlessCheck detekt lint testDebugUnitTest assembleDebug` + `verifyHiltViewModelMapDebug` green

## HyperOS checklist (manual)

- [ ] Shizuku ready → Tools → Device diagnostics → Refresh
- [ ] Battery card: percent + status; missing `current now` does not crash
- [ ] Network card loads or shows section error (no ANR / freeze)
- [ ] CPU card loads or shows section error if `top` stalls
- [ ] Tier NONE: setup CTA, no silent empty
