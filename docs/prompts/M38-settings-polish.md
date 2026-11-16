# M38 — Settings polish (rollback N, per-app cooldown, ignored overview, repo priority)

> Branch `m38-settings-polish`. After M37 (#61). Includes M37 docs closeout.

## In scope

- Docs: mark M37 Done; ROADMAP / PLAN §12 / Capability matrix
- Global rollback keep-N slider (1–5, default 3) under Settings → Updates; retrim on change; Kiln-Config round-trip
- Per-app update cooldown override on App detail (`null` = use global); Room + AutoInstallPolicy
- Settings → Updates → Ignored updates overview (list + re-enable; tap → App detail)
- Sources repo priority ↑↓; `ensureKnownRepos` must preserve user priority
- DE + EN; unit tests; quality gates

## Out of scope

- Publishing / F-Droid submit, dual-user, tablet layouts, backup phase 2
- Drag-and-drop reorder library (↑↓ only)
- Nearby swap, panic mode, local notes
- GPL reference code

## Done when

- [x] M37 Done in ROADMAP/PLAN; this prompt linked as M38
- [x] Rollback N 1–5 applies and retrims; config export/import includes the pref
- [x] Per-app cooldown overrides global for auto-install; null uses global
- [x] Ignored-updates list shows ignored apps; re-enable clears ignore
- [x] Sources ↑↓ renumbers priorities; repair does not overwrite user priority
- [x] `spotlessCheck detekt lint testDebugUnitTest assembleDebug` green

Shipped on main via [#63](https://github.com/involvex/kiln/pull/63).
