# M41 — Discover panel (+ Hilt Nav3 crash fix)

> Shipped in [#71](https://github.com/involvex/kiln/pull/71). Branch `m41-discover-panel`. After M40 (#69) / docs closeout (#70).

## In scope

- Fix Apps-shell Discover crash: `hiltViewModel` via `LocalActivity` (Nav3 entry owner missed Hilt map)
- F-Droid-style Discover home: shelves (recent / new in catalog / topics) + Categories + Search; GitHub tab retained
- Apps segments: Library | Discover only (Trending absorbed)
- Honest labels — no download charts; DE + EN; unit tests; quality gates

## Out of scope

- Dual-user / tablet / F-Droid submit / cloud backup / ROOT backup
- Copying F-Droid GPL UI

## Done when

- [x] Discover opens under Apps shell without ViewModel crash
- [x] Home shelves + categories; search/filter path retained
- [x] Trending segment removed
- [x] `spotlessCheck detekt lint testDebugUnitTest assembleDebug` green locally
- [ ] Manual HyperOS: Library, Discover shelves, search, GitHub tab, catalog detail

## Manual HyperOS checklist (PR)

- [ ] Apps → Discover opens (no crash)
- [ ] Shelves show after catalog refresh; honesty copy visible
- [ ] Category chip filters to list; clear returns to shelves
- [ ] GitHub tab lookup still works
- [ ] Catalog detail from shelf card
