# M23 — Scheduled Maintenance

> Branch `m23-scheduled-maintenance`. PLAN §12 P1 (Geplante Wartung). Also refresh ROADMAP after M22 (#36).

## In scope

- Opt-in Settings → **Maintenance** (off by default)
- Weekly WorkManager job with **UNMETERED + REQUIRES_CHARGING**; **Run now** without charging
- Tasks (each toggleable):
  - System cache trim via `PrivilegedExecutor.clearCache(null)` — skip honestly without `CLEAR_CACHE`
  - Prune `cacheDir/apk_downloads` older than 3 days
  - Prune rollback cache by age (30d) and/or total size (1 GiB); keep ≤3/pkg
  - Prune `cacheDir/backup_restore` temps
- Maintenance run protocol (last ~50 runs) visible in Settings
- New `:feature:maintenance` module; contracts in `:core:model`
- ROADMAP/PLAN/README: M22 Done (#36); M23–M28 sequence; backlog status table

## Out of scope

- SAF user backup rotation (no retained tree URI)
- Full activity log (M24), widgets, config sync, Exodus, F-Droid submit
- HyperOS M1–M8 checklist sweep (release gate, not this milestone)
- User-configurable rollback N slider

## Done when

- Toggle off → no periodic work; Run now still records skip/disabled when master off
- Toggle on + Wi‑Fi/charging → weekly run; protocol shows per-task lines
- Without Shizuku: trim skipped, other prune tasks still run
- `spotlessCheck detekt lint testDebugUnitTest assembleDebug` green
