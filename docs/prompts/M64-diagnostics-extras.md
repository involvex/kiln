# M64 — Diagnostics extras (cpuinfo / thermals)

> Branch `m64-diagnostics-extras`. Extends M60 Device diagnostics. No raw dumps, no `cmd battery`, no Root.

## In scope

- Settings → Tools → Device diagnostics: add **CPU packages** (`dumpsys cpuinfo`) + **Thermals** (`dumpsys thermalservice`)
- Bounded ShellRunner for both; wire existing `DUMPSYS_CPUINFO` off unbounded `run`
- Pure parsers + unit tests; DE + EN; honest per-card errors

## Out of scope

- Raw dump toggle; `cmd battery` / `batterystats`; Root; Processes widget cpuinfo (ANR risk); AGPL copy

## Done when

- [ ] Refresh shows five cards (or section errors): battery / net / top / cpuinfo / thermals
- [ ] No raw stdout; no `cmd battery`
- [ ] HyperOS: missing HAL / empty cpuinfo → section error or empty, no crash
- [ ] `spotlessCheck detekt lint testDebugUnitTest assembleDebug` green
