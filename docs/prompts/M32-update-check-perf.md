# M32 — Update-check responsiveness

> Branch `m32-update-check-perf`. After M31 (#49). Fixes Apps/Sources update spinner hang.

## In scope

- `refreshUpdates()` = local F-Droid index only (no live GitHub HTTP)
- Live GitHub probes only during explicit Check updates / Sync all / background worker
- Per-repo progress + cancel for Apps Check updates and Sources Sync all
- Shorter timeout budget for mirror failover after the first failed base
- Bounded concurrency (~2) when syncing enabled F-Droid repos
- Unit tests; DE + EN strings
- Docs: ROADMAP M32 In progress → Done on merge

## Out of scope

- Exodus, freeze/hibernate, universal APK merge, Coil proxy, F-Droid submit
- Redesigning Discovery catalog sync

## Done when

- Opening Apps does not trigger GitHub network for badges
- Check updates / Sync all show progress and can be cancelled
- Dead proxy/mirror fails faster than full 10‑minute call timeout × mirrors
- `spotlessCheck detekt lint testDebugUnitTest assembleDebug` green
