# M45 — Hilt crash fix + App Detail overhaul + stability guards

> Branch `m45-stability-app-detail`. After M44 (#76) / docs (#77).

## In scope

- Fix runtime `Cannot create an instance of …ViewModel` from **incomplete/stale KSP Hilt maps** (Storage, Background Controls, Freeze, Permission Audit, Catalog Detail missing from debug graph)
- `ksp.incremental=false`; `verifyHiltViewModelMapDebug` + CI step; allowlist [`app/hilt-viewmodels.txt`](../../app/hilt-viewmodels.txt)
- Harden `kilnHiltViewModel` (clear Activity error); forbid raw `hiltViewModel()` in feature/app; drop PermissionAudit `!!`
- App Detail UI overhaul: hero → actions → update status → Updates group → Installation → Security → Technical
- Docs: ROADMAP / PLAN §12 / CHANGELOG

## Out of scope

- Dual-user / tablet / cloud backup
- Redesigning Background Controls or Manage tabs beyond crash fix
- HyperOS M1–M8 release-gate checklist completion

## Done when

- [ ] Clean `assembleDebug` includes all allowlisted ViewModels in Hilt debug component
- [ ] `verifyHiltViewModelMapDebug` green in CI
- [ ] App Detail section order matches Catalog-style density (actions under hero; truncated signature)
- [ ] `spotlessCheck detekt lint testDebugUnitTest assembleDebug` green
- [ ] Manual HyperOS checklist in PR

## Manual HyperOS checklist (PR)

- [ ] Manage → Storage opens without ViewModel crash
- [ ] Settings → Tools → Background controls / Freeze / Permission audit open without crash
- [ ] Catalog detail opens without crash
- [ ] App Detail: actions near top; Updates / Installation / Security / Technical cards readable
