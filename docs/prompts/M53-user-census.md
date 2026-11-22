# M53 — Read-only Android user census

> Branch `m53-user-census`. After M52. Probe for Dual Apps / work profiles before changing `--user`.

## In scope

- With Shizuku: allowlisted `pm list users` (no user-supplied args) + show current user id
- Honest copy when only user 0 exists (do not invent Dual Apps)
- Settings → Tools entry; DE + EN; unit test for list-users parser
- Docs: ROADMAP / capability matrix; note that `--user N` installs wait for a later milestone

## Out of scope

- `--user N` install / uninstall / debloat / backup
- Writing Dual Apps or Second space paths
- Cloud / ROOT backup / F-Droid / tablet

## Done when

- Screen lists users from `pm list users` when capable; Tier NONE shows setup CTA
- `spotlessCheck detekt lint testDebugUnitTest assembleDebug` green
