# Prompt M17 — HyperOS E2E + release prep

> Branch `m17-hyperos-release-prep`. Depends on M13–M16 merged ([#27](https://github.com/involvex/kiln/pull/27)).

## Context for the next agent

- UX plan (Check-now / locales / Termux request / 4-tab Manage / store Discover / Trending) is **done on main**.
- Do **not** reopen M13–M16 scope or the old UX plan file.
- Source of truth: [`docs/ROADMAP.md`](../ROADMAP.md), [`docs/PLAN.md`](../PLAN.md), [`AGENTS.md`](../../AGENTS.md).
- Test device: Xiaomi HyperOS. Prefer documenting manual checks over inventing features.
- **Do not** `adb install` / launch on the user’s phone unless they ask.
- Publishing / F-Droid submit / nightlies stay out of scope unless the user explicitly asks.

## Scope

1. **Device checklist (HyperOS)** — tick or note failures in the PR for:
   - Language EN/DE chips (first frame + bottom nav)
   - Settings → Check now (no crash; notification path)
   - Termux: Request RUN_COMMAND → refresh checklist
   - Exactly 4 bottom tabs; Manage → Storage | Backup
   - Apps → Library | Discover | Trending
   - Catalog refresh → icons/detail/screenshots when index has media
   - Trending ≥3 shelves when catalog is populated (honest copy visible)
2. **Docs / release prep**
   - Keep [`CHANGELOG.md`](../../CHANGELOG.md) Unreleased accurate for M11–M16 (move into `0.2.0` only when tagging)
   - Optionally bump `versionName` / `versionCode` toward `0.2.0` when checklists look good
   - Update README status line after bump
3. **Small fixes only** if a checklist item fails due to an obvious bug from #27 (keep commits focused)

## Out of scope

- New store UI / ranking / telemetry
- GitHub Actions nightlies (PLAN §12 P1 — later milestone)
- F-Droid / Play submit
- Expanding Termux beyond permission UX
- Feature↔feature module dependencies

## Definition of Done

- [ ] HyperOS checklist filled in PR (pass / fail / blocked with reason)
- [ ] Build, unit tests, lint, spotless, detekt green
- [ ] ROADMAP M17 status updated; capability matrix still accurate
- [ ] No new permissions without PR justification
