# M61 — GitLab + Codeberg release sources

> Branch `m61-gitlab-codeberg`. After M60 / `v0.2.14`. Extend release sources beyond GitHub. No privilege changes.

**Status:** Done on `m61-gitlab-codeberg`.

## In scope

- Add **GitLab** and **Codeberg** as release source hosts (owner/repo or forge URL)
- Reuse the same Stable / Pre-release channel + APK asset heuristic as GitHub (M12 / M19)
- Discover lookup + Sources add + ManagedApp persistence for the new hosts
- Optional PAT / token for private or rate-limited APIs (Keystore; never log)
- DE + EN strings; unit tests for URL parse / host detection
- Docs: ROADMAP / PLAN / capability matrix

## Out of scope

- GitHub Actions / Nightly for non-GitHub forges
- Full forge search ranking / stars import for GitLab/Codeberg
- Self-hosted arbitrary GitLab instances (unless a small allowlist of API base URLs fits cleanly)
- Cloud backup, ROOT `/data/data`, tablet layouts

## Done when

- [x] User can add a GitLab and a Codeberg repo and see a release APK candidate (or honest empty/error)
- [x] Existing GitHub flows unchanged
- [x] `spotlessCheck detekt lint testDebugUnitTest assembleDebug` + `verifyHiltViewModelMapDebug` green
