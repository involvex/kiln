# Roadmap

Hobby ship loop for Cursor `/ship-roadmap`. Architecture and milestone tracker stay in [`docs/PLAN.md`](./docs/PLAN.md) and [`docs/ROADMAP.md`](./docs/ROADMAP.md).

## Now

- [ ] **M61** GitLab + Codeberg release sources (owner/repo; same channel/APK heuristic as GitHub) — [`docs/prompts/M61-gitlab-codeberg.md`](./docs/prompts/M61-gitlab-codeberg.md)

## Later

- [ ] **M62** Storage treemap of largest consumers (PLAN §4.5 leftover)
- [ ] **M63** Curated debloat ratings (license-cleared source only; no invented ratings; `debloat-db.json` still empty)
- [ ] **M64** Diagnostics extras: `dumpsys cpuinfo` / thermals cards (no raw dump; no `cmd battery`)
- [ ] **M65** Tablet/foldable: NavigationRail + list-detail (PLAN §12 P3)
- [ ] **M66** WebDAV backup destination (not Cloudflare/R2)
- [ ] **M67** ROOT `/data/data` backup (needs RootExecutor + libsu first)
- [ ] F-Droid refile ≈ 2026-02-08 ([docs/STORE.md](./docs/STORE.md))
- [ ] Share 0.2.14 device diagnostics (draft in SHARE.md)

## Done

- [x] **M60** Device diagnostics docs closeout (mark Done after #111 / 0.2.14; seed this ROADMAP)
- [x] **M60** Device diagnostics implementation ([#111](https://github.com/involvex/kiln/pull/111), `0.2.14`)
