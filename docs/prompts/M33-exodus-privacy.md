# M33 — Exodus privacy chips

> Branch `m33-exodus-privacy`. After `v0.2.3`. PLAN §12 P2 tracker info.

## License / API constraint

- Official Exodus API: **no keys for end-user Android apps**; self-host encouraged.
- API/DB: ODbL / DbCL — attribute; do not ship GPL/AGPL code.
- Default path: **HTTPS deep-link** to public reports site (no API key).
- Optional: user-configured **self-hosted** base URL + Token (Keystore) to fetch tracker names.

## In scope

- Catalog detail + App detail: “Open Exodus report” (browser)
- Settings → Privacy: optional Exodus instance URL + API token (encrypted); clear token
- When configured: fetch `/api/search/{package}`, resolve tracker names via `/api/trackers` (cached), show chips + attribution
- DE + EN strings; unit tests for URL builder + JSON parse
- Docs: ROADMAP M33 Done on merge

## Out of scope

- Bundled offline tracker DB, on-device APK scan, freeze/hibernate, F-Droid submit

## Done when

- Deep-link works without any Exodus config
- With self-hosted URL+token, catalog/app detail can show tracker name chips
- Without config, UI does not call reports.exodus-privacy.eu.org API
- Gates green
