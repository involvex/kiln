# M19 — GitHub channels + Anti-features

> Branch `m19-channels-antifeatures`. Specs: PLAN §12 (GitHub Actions / Nightly / Beta; Anti-Features).

## In scope

- App detail channel picker: Stable | Pre-release | Nightly for GitHub-linked apps
- Nightly via GitHub Actions artifacts (PAT required); install unzip + APK regex
- Catalog detail: F-Droid `metadata.antiFeatures` chips (DE/EN labels)

## Out of scope

- Exodus / tracker scanning
- Workflow picker UI
- Codeberg / GitLab
- List-row anti-feature badges
- F-Droid store submit

## Done when

- Channel switch refreshes updates; Nightly without PAT shows honest error
- Nightly with PAT can enqueue install from artifact zip
- Catalog detail shows anti-feature chips after sync
- `spotlessCheck detekt lint testDebugUnitTest assembleDebug` green
