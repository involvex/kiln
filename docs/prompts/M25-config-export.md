# M25 — Config Export / Import

> Branch `m25-config-export`. PLAN §12 P2 (Kiln-Config JSON). Also mark M23 Done (#37).

## In scope

- Settings → **Kiln config**: SAF export / import of `kiln-config.json` (`schemaVersion: 1`)
- Contents: repos, managed apps (incl. watchlist), non-secret prefs (background updates, automation, maintenance, appearance, installer, termux)
- Hint only: `githubPatWasConfigured` boolean — never the PAT
- Import: merge/upsert by repo `id` / managed `packageName`; prefs sections overwrite after confirm dialog
- Mark M23 Done (#37) in ROADMAP / README / capability matrix

## Out of scope

- Cloudflare sync
- Index cache, install jobs, rollback APKs, debloat snapshots, maintenance run log, Dev prefs, cooldown first-seen map
- Replace-all wipe import
- Changing Obtainium import (stays under Sources)

## Done when

- Export writes valid JSON via SAF; import merges with confirmation summary
- Serialized file never contains PAT / token values
- After import, UI reminds user to sync Sources and re-enter PAT if needed
- `spotlessCheck detekt lint testDebugUnitTest assembleDebug` green
