# M62 — Storage treemap

> Branch `m62-storage-treemap`. PLAN §4.5 treemap of largest consumers (replaces M28 size bars).

## In scope

- Squarified treemap (pure Compose Canvas) of top apps by measured size
- Optional aggregate leaves for scanned APK leftovers, orphans, and large files (only when bytes &gt; 0)
- Tap app tile → App info; DE + EN; unit tests for layout + leaf selection
- No new chart libraries

## Out of scope

- Sunburst; inventing Downloads/Archive volumes without measured data
- Delete-from-treemap; new permissions; booster UX

## Done when

- [ ] Storage shows proportional treemap after usage access
- [ ] Areas roughly match relative sizes; empty/permission states unchanged
- [ ] No silent destructive actions from the treemap
- [ ] DE + EN; `StorageTreemapTest` green
- [ ] `spotlessCheck detekt lint testDebugUnitTest assembleDebug` green
