# M42 — Import GitHub starred repos → Sources

> Branch `m42-github-stars`. After M41 (#71) / docs (#72).

## In scope

- Fix remaining Nav3/Hilt ViewModel crashes via shared `kilnHiltViewModel` (Activity scope) — includes Storage crash under Manage
- Sources: load authenticated user’s starred repos (`GET /user/starred`, paginated) when a GitHub PAT is set
- Review UI: multi-select → add as managed GitHub sources (reuse `addGithubSource` path); mark already configured
- Light APK heuristic (recent release asset name matches `*.apk`) with honest copy — stars ≠ apps; no silent install
- DE + EN strings; unit tests; quality gates

## Out of scope

- Dual-user / tablet / cloud backup / ROOT backup
- Auto-install from stars; inventing package names beyond provisional `github.owner.repo`
- Deep Actions/Nightly import from stars (already covered by existing GitHub channel settings)

## Done when

- [ ] Manage → Storage (and sibling shell routes) open without ViewModel crash
- [ ] Sources → Load starred (PAT required) shows review list; multi-select add works; duplicates reported
- [ ] APK heuristic labels are honest; rate-limit / no-token messaging clear
- [ ] `spotlessCheck detekt lint testDebugUnitTest assembleDebug` green locally
- [ ] Manual HyperOS checklist in PR

## Manual HyperOS checklist (PR)

- [ ] Manage → Storage / Backup / APK tools open (no crash)
- [ ] Sources → no PAT: clear CTA to Updates settings
- [ ] With PAT: Load starred → select → Add; already-managed rows marked
- [ ] Added sources appear for update check; no install without user action
