# Prompt M11 — App Discovery

> Branch `m11-discovery`. Specs: PLAN §4.2 (browse). Publishing / store polish deferred.

## Scope

- Full F-Droid catalog sync (stream index-v2; keep newest release only)
- Persist `categories`; Discovery UI: search, category chips, install enqueue
- Entry from Sources → Discover
- Wanted-package update sync must **upsert** (not wipe catalog)

## Out of scope

- GitHub browse, icons CDN, anti-features UI, F-Droid submit
