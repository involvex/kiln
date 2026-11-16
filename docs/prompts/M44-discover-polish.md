# M44 — Discover UI polish + remove Exodus

> Branch `m44-discover-polish`. After M43 (#75). Includes M43 docs closeout.

## In scope

- Discover shelf cards: denser compact layout (no package/categories on shelf face; 1-line summary; compact Install; narrower width)
- Catalog detail: Install / Share / Watch under hero; tighter spacing; flatten Sources + Links (no nested card stack)
- **Remove Exodus** end-to-end: catalog + app detail sections, Settings Privacy hub, prefs/API/repo, DI, tests, strings
- Docs: M43 Done (#75); M33 noted removed in M44; ROADMAP / PLAN / CHANGELOG
- DE + EN string cleanup; quality gates

## Out of scope

- New discovery sources / ranking / fake download counts
- Library list or bottom-nav redesign
- Sticky bottom Install bar
- Replacing Exodus with another tracker service
- Dual-user / tablet / release tagging

## Rationale (Exodus)

Public `reports.exodus-privacy.eu.org/.../reports/<pkg>/` often 404s. Tracker chips needed a self-hosted API; official Exodus does not issue end-user API keys. Dead UI → remove. Keep F-Droid anti-feature chips (M19).

## Done when

- [ ] Discover home shows clearly denser shelves (≥4 shelf rows more reachable without scroll)
- [ ] Catalog detail shows Install in the first screenful; no Exodus UI
- [ ] App detail and Settings have no Exodus / Privacy-Exodus hub
- [ ] `spotlessCheck detekt lint testDebugUnitTest assembleDebug` green
- [ ] Manual HyperOS checklist in PR

## Manual HyperOS checklist (PR)

- [ ] Apps → Discover: shelves denser; Install on card still works; tap opens detail
- [ ] Catalog detail: Install / Share / Watch near top; Sources/Links readable; no Exodus
- [ ] App detail: no Exodus section
- [ ] Settings hub: no Privacy (Exodus) entry
