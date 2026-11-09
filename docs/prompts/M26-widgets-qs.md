# M26 — Widgets / QS tile

> Branch `m26-widgets-qs`. PLAN §12 P2 (Glance + QS). Also mark M24 Done (#39).

## In scope

- Glance home widget: update count, Check now, Trim caches (two-step confirm)
- Thin QS tile: update count label + Check now
- Durable `WidgetPreferences` count (AppUpdateRepository is in-memory)
- Check now → `BackgroundUpdateScheduler.enqueueImmediate()`
- Trim → `PrivilegedExecutor.clearCache(null)` when `CLEAR_CACHE`, else open Manage/Storage
- Mark M24 Done (#39) in ROADMAP / README / capability matrix

## Out of scope

- App Shortcuts, multi-size layouts, auto-install from widget, per-app cache from widget
- M27 Permission audit, M28 Storage phase 2

## Done when

- Widget shows a plausible update count after check / app refresh
- Check now queues WorkManager (force) from widget and QS tile
- Trim confirms on widget; without privilege opens Manage honestly
- `spotlessCheck detekt lint testDebugUnitTest assembleDebug` green
