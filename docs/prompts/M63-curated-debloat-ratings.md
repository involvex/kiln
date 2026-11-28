# M63 — Curated debloat ratings

> Branch `m63-curated-debloat-ratings`. PLAN §4.10 curated `debloat-db.json` (no invented ratings).

## In scope

- Pure `DebloatDbParser` + unit tests (empty asset shape + fixture with synthetic pkgs)
- Keep shipped `packages: []` until a **license-cleared** source is approved
- Show catalog source / version / rated-package count in Debloat UI
- Docs: reference.md license gate; ROADMAP/PLAN/SHARE/capability matrix

## Out of scope

- Inventing or importing real OEM package ratings without clearance
- Remote catalog fetch; rating/vendor filter UI; copying GPL/UAD lists

## Done when

- [ ] Asset still has `packages: []`
- [ ] Parser tests green (empty + invalid rating → UNKNOWN + fixture)
- [ ] Debloat shows honest catalog meta line
- [ ] `spotlessCheck detekt lint testDebugUnitTest assembleDebug` green
