# Roadmap

Hobby ship loop for Cursor `/ship-roadmap`. Architecture and milestone tracker stay in [`docs/PLAN.md`](./docs/PLAN.md) and [`docs/ROADMAP.md`](./docs/ROADMAP.md).

## Now

- [ ] F-Droid refile (see [docs/STORE.md](./docs/STORE.md); age window / uniqueness)

## Later

- [ ] Import first cleared debloat package rows into `debloat-db.json` (needs approved Apache-2.0/CC0/dual-licensed or Kiln-first-party source + NOTICE; **not** UAD-NG GPL-3.0)
- [ ] Share 0.2.14 device diagnostics (draft in SHARE.md)
- [ ] Share M61 GitLab + Codeberg release sources (draft in SHARE.md)
- [ ] Share M62 storage treemap (draft in SHARE.md)
- [ ] Share M63 curated debloat catalog pipeline (draft in SHARE.md)
- [ ] Share M64 diagnostics extras (draft in SHARE.md)
- [ ] Share M65 tablet / foldable layouts (draft in SHARE.md)
- [ ] Share M66 WebDAV backup destination (draft in SHARE.md)
- [ ] Share M67 ROOT `/data/data` backup (draft in SHARE.md)
- [ ] Share M68 debloat license gate (draft in SHARE.md)

## Done

- [x] **M68** Debloat data license gate (`m68-debloat-license-gate`; UAD-NG GPL-3.0 not cleared; `packages: []`)
- [x] **M67** ROOT `/data/data` backup (`m67-root-data-backup`; Magisk opt-in + libsu; `user_de` deferred)
- [x] **M66** WebDAV backup destination (`m66-webdav-backup`; not Cloudflare/R2)
- [x] **M65** Tablet/foldable: NavigationRail + list-detail (`m65-tablet-layouts`)
- [x] **M64** Diagnostics extras: cpuinfo + thermals cards (`m64-diagnostics-extras`)
- [x] **M63** Curated debloat ratings pipeline (`m63-curated-debloat-ratings`; packages still empty)
- [x] **M62** Storage treemap of largest consumers (`m62-storage-treemap`)
- [x] **M61** GitLab + Codeberg release sources (`m61-gitlab-codeberg`)
- [x] **M60** Device diagnostics docs closeout (mark Done after #111 / 0.2.14; seed this ROADMAP)
- [x] **M60** Device diagnostics implementation ([#111](https://github.com/involvex/kiln/pull/111), `0.2.14`)
