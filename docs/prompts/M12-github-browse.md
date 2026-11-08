# Prompt M12 — GitHub browse in Discover

> Branch `m12-github-browse`. Extends M11 Discover with GitHub Releases lookup.

## Scope

- Discover source tabs: F-Droid | GitHub
- Look up `owner/repo` or github.com URL → latest APK asset via existing `GitHubReleasesClient`
- List saved ManagedApp GitHub sources; Install enqueues via `InstallQueue`
- Looking up a new repo also persists a ManagedApp (same provisional package as Sources share-add)
- Reuse PAT from Sources (rate limit honesty in UI copy)

## Out of scope

- GitHub Actions / nightly channels (PLAN §12 P1)
- Repo search API ranking, icons CDN, Codeberg/GitLab
