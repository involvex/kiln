# M50 — App component lists (services / receivers / providers)

> Branch `m50-app-components`. After M49. Extends M43 activities viewer.

## In scope

- App Detail: exported **services**, **receivers**, **providers** (read-only cards)
- Same PackageManager load as activities (`GET_SERVICES` / `GET_RECEIVERS` / `GET_PROVIDERS`)
- Filter via existing activities search query; DE + EN; unit tests for `AppComponentFilter`
- Docs: ROADMAP / capability matrix

## Out of scope

- Start / enable / disable non-exported components (needs Shizuku + confirm)
- GPL App Manager UI copy

## Done when

- Exported-only lists under App Detail Technical section
- `spotlessCheck detekt lint testDebugUnitTest assembleDebug` green
