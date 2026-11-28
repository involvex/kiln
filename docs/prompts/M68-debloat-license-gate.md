# M68 — Debloat data license gate

> Branch `m68-debloat-license-gate`. SPDX review before any `debloat-db.json` rows. No invented ratings.

## In scope

- Record UAD-NG / `uad_lists.json` SPDX (GPL-3.0) in `docs/reference.md` — **not cleared** for Apache-2.0 Kiln
- Keep shipped `packages: []`; honest notes + NOTICE that no third-party catalog is bundled
- ROADMAP: gate Done; real import waits for an approved Apache-2.0 / CC0 / dual-licensed / Kiln-first-party source

## Out of scope

- Importing UAD lists; inventing OEM ratings; GPL UI/strings; remote catalog fetch

## Done when

- [ ] SPDX decision documented; `packages` still empty
- [ ] Parser tests green; no fake ratings
- [ ] `spotlessCheck detekt lint testDebugUnitTest assembleDebug` green
