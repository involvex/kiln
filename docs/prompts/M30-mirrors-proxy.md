# M30 — Mirrors / proxy

> Branch `m30-mirrors-proxy`. PLAN §12 P3 (Mirrors/Proxy). After M29 (#47).

## In scope

- Per-repo **mirror HTTPS URLs** (ordered failover after primary `address`) for F-Droid index sync and APK download
- Global **HTTP / SOCKS** proxy in Settings → Network (host, port, optional user/password)
- **Orbot** shortcut: SOCKS `127.0.0.1:9050` (no embedded Tor)
- Wire proxy into `KilnHttpClient` / OkHttp; rebuild when prefs change
- Kiln-Config schema **v2**: export mirrors + non-secret proxy prefs (never proxy password); import accepts v1 and v2
- Unit tests; DE + EN strings
- Docs: ROADMAP M29 Done, M30 In progress → Done on merge

## Out of scope

- Embedded Tor binary / Arti
- Per-repo proxy overrides
- Coil ImageLoader proxy (catalog icons may bypass proxy until a follow-up)
- Changing built-in canonical `address` repair to wipe user mirrors

## Done when

- Repo mirrors editable in Sources; sync/download tries primary then mirrors on failure
- Settings proxy/Orbot affects F-Droid/GitHub HTTPS via `KilnHttpClient`
- Config export/import round-trips mirrors + proxy type/host/port (no password)
- `spotlessCheck detekt lint testDebugUnitTest assembleDebug` green
