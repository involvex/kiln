# M24 — Activity log

> Branch `m24-activity-log`. PLAN §12 P2 (searchable local log). Also mark M25 Done (#38).

## In scope

- Settings → **Activity log**: searchable/filterable list of local events
- Room table `activity_log` (cap 500); never leave the device
- Emitters: install terminals, backup/restore, cache clear, maintenance summary, allowlisted shell (`commandId` only), debloat, Termux RUN_COMMAND, automation
- Clear all with confirmation
- Mark M25 Done (#38) in ROADMAP / README / capability matrix

## Out of scope

- SAF/export of the log, remote sync
- Replacing M23 maintenance protocol
- System `ACTION_DELETE` uninstall results
- Per-step install progress logging
- Widgets (M26)

## Done when

- Events appear after install / backup / cache clear / maintenance / privilege shell / Termux / automation
- Search + type filter work; stored rows have no URLs, paths, tokens, or raw shell output
- `spotlessCheck detekt lint testDebugUnitTest assembleDebug` green
