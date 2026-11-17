# M46 — Widget rework (Updates / Cache / Processes)

> Shipped in [#81](https://github.com/involvex/kiln/pull/81). Branch `m46-widget-rework`. After M45 (#79) / docs (#80).

## In scope

- **Updates widget:** remove trim / Confirm / Cancel; show count + up to 5 update preview lines; Check now only
- **Cache widget (new):** global trim-caches with confirm; NONE → Manage + honest CTA
- **Processes widget (new):** Shizuku/Root list (mem + coarse CPU) + force-stop with confirm; NONE → privilege setup CTA
- Privilege: allowlisted `dumpsys meminfo` / `dumpsys cpuinfo`; parsers + unit tests; FORCE_STOP gate
- Docs: ROADMAP / PLAN / CHANGELOG / capability matrix

## Out of scope

- Per-app cache from widget, auto-install, App Shortcuts
- Real-time CPU graphs; kill without confirm

## Done when

- [x] Three Glance providers in picker; Updates has no trim UI
- [x] Cache confirm clears only after confirm; NONE opens Manage
- [x] Processes lists top apps with Shizuku; force-stop confirms; NEVER kills Kiln
- [x] `spotlessCheck detekt lint testDebugUnitTest assembleDebug verifyHiltViewModelMapDebug` green
- [ ] Manual HyperOS checklist in PR

## Manual HyperOS checklist (PR)

- [ ] Updates: preview lines + Check now; no Confirm trim / Cancel
- [ ] Cache: confirm flow; NONE CTA
- [ ] Processes: list + kill with Shizuku; NONE setup CTA
