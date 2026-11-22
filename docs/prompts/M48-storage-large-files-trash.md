# M48 — Storage phase 3: large files + trash

> Branch `m48-storage-large-files-trash`. PLAN §4.5 large files; soft-delete via `.kiln-trash`.

## In scope

- SAF-tree scan (same folder pick as leftovers): large files ≥ 50 MB (non-APK), same-size then SHA-256 duplicates
- Soft-delete leftovers / large / duplicates into `.kiln-trash/` with TSV manifest; Undo + permanent purge
- Persist last tree URI; weekly maintenance age-prunes trash (14 days) with `pruneBackupTemp`
- DE + EN; unit tests for `StorageCleanerLogic` + manifest codec
- Docs: ROADMAP M48 Done on merge

## Out of scope

- Treemap/Sunburst, whole-disk hash, empty folders, `MANAGE_EXTERNAL_STORAGE`
- Dual-user / tablet / cloud / ROOT backup / F-Droid submit

## Done when

- Storage shows large files + duplicates + trash after folder pick
- Move to trash / restore / purge confirm; orphans still permanent
- `spotlessCheck detekt lint testDebugUnitTest assembleDebug` green
