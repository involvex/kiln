# M21 — Changelog & Watchlist

> Branch `m21-changelog-watchlist`. PLAN §12 P2 (Release Notes + Beobachten ohne Installieren).

## In scope

- Parse GitHub release `body` and F-Droid index-v2 `whatsNew` into `AppUpdateInfo.releaseNotes`
- App detail Update section: expandable plain-text release notes
- Catalog detail **Watch / Unwatch** for uninstalled F-Droid packages (`watchOnly` managed row, pinned)
- GitHub Discover lookup creates `watchOnly` managed rows until install
- Sources **Watchlist** section with new-version badge + notes preview + Unwatch
- Background notify includes watch-only; auto-install skips them
- Mark M20 Done (#34) in ROADMAP

## Out of scope

- Markdown renderer, full release history
- Auto-Match NONE CTA, widgets, Exodus trackers
- Auto-install for watch-only apps

## Done when

- Notes show for GitHub/F-Droid updates on App detail when present
- Watch persists across sync; Unwatch removes row; install clears `watchOnly`
- `spotlessCheck detekt lint testDebugUnitTest assembleDebug` green
