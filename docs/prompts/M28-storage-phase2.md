# M28 — Storage phase 2

> Branch `m28-storage-phase2`. PLAN §4.5 deferred (leftovers, size bars, orphaned Android/data). Also mark M27 Done (#41).

## In scope

- Simple size bars from existing `AppStorageStats` (not Treemap/Sunburst)
- APK leftover scanner (Downloads via SAF/MediaStore): preview + confirm delete (Tier NONE)
- Orphaned `Android/data` + `Android/obb` list + delete with confirm (`ACCESS_ANDROID_DATA`)
- Mark M27 Done (#41) in ROADMAP / README / capability matrix

## Out of scope

- Full Treemap/Sunburst, duplicate-by-hash, empty folders, Booster UX
- Root `/data/data` cleanup, backup Android/data wiring
- Auto-delete without preview

## Done when

- Storage shows proportional size bars for top apps
- Leftover APK list deletes only after confirm
- Orphans require privilege; delete confirms; missing capability → Privilege setup
- `spotlessCheck detekt lint testDebugUnitTest assembleDebug` green
