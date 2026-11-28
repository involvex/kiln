# Roadmap

Hobby ship loop for Cursor `/ship-roadmap`. Architecture and milestone tracker stay in [`docs/PLAN.md`](./docs/PLAN.md) and [`docs/ROADMAP.md`](./docs/ROADMAP.md).

## Now

- [ ] **M64** Diagnostics extras: `dumpsys cpuinfo` / thermals cards (no raw dump; no `cmd battery`)

## Later

- [ ] **M65** Tablet/foldable: NavigationRail + list-detail (PLAN §12 P3)
- [ ] **M66** WebDAV backup destination (not Cloudflare/R2)
- [ ] **M67** ROOT `/data/data` backup (needs RootExecutor + libsu first)
- [ ] Import license-cleared debloat package rows into `debloat-db.json` (after SPDX + NOTICE; no invented ratings)
- [ ] F-Droid refile ≈ 2026-02-08 ([docs/STORE.md](./docs/STORE.md))
- [ ] Share 0.2.14 device diagnostics (draft in SHARE.md)
- [ ] Share M61 GitLab + Codeberg release sources (draft in SHARE.md)
- [ ] Share M62 storage treemap (draft in SHARE.md)
- [ ] Share M63 curated debloat catalog pipeline (draft in SHARE.md)

## Done

- [x] **M63** Curated debloat ratings pipeline (`m63-curated-debloat-ratings`; packages still empty)
- [x] **M62** Storage treemap of largest consumers (`m62-storage-treemap`)
- [x] **M61** GitLab + Codeberg release sources (`m61-gitlab-codeberg`)
- [x] **M60** Device diagnostics docs closeout (mark Done after #111 / 0.2.14; seed this ROADMAP)
- [x] **M60** Device diagnostics implementation ([#111](https://github.com/involvex/kiln/pull/111), `0.2.14`)
