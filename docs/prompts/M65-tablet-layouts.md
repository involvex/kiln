# M65 — Tablet / foldable layouts

> Branch `m65-tablet-layouts`. PLAN §12 P3. Phone-first remains the default; medium+ width gets NavigationRail + Apps list-detail.

## In scope

- Compact (`< 600dp`): keep 4-tab `NavigationBar`; list still pushes detail
- Medium+: `NavigationRail` (same 4 destinations); no bottom bar
- Apps (`AppsKey`) + `AppDetailKey` / `CatalogDetailKey` via Material Adaptive `ListDetailSceneStrategy` (side-by-side; empty placeholder; hide detail back in two-pane)
- DE + EN strings; docs / ROADMAP / capability matrix

## Out of scope

- Two-pane Settings / Sources hubs, Debloat, Storage, Permission audit
- NavigationDrawer, extra top-level tabs, resurrecting `StorageKey` / `BackupKey`
- M66 WebDAV, M67 ROOT `/data/data`, F-Droid refile, invented debloat ratings
- `adb install` / launch on device

## Done when

- [ ] Compact: bar + push detail unchanged
- [ ] Medium+: rail + Apps two-pane with placeholder
- [ ] Resize compact ↔ medium swaps chrome without crash
- [ ] `spotlessCheck detekt lint testDebugUnitTest assembleDebug` green
