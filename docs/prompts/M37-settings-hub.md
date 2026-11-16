# M37 — Settings hub, GitHub token, signature confirm

> Branch `m37-settings-hub`. After M36 (#58).

## In scope

- Settings category hub (Appearance, Updates, Network, Privacy, Installation, Automation, Maintenance, Tools, Data, About) with Navigation 3 subpages
- GitHub PAT editor under Settings → Updates (encrypted store unchanged); Sources keeps status + deep link
- Background update check interval: 12h / 1 day / 7 days; Kiln-Config export/import (null-safe for older files)
- Per-update “Install anyway” for known signature WARNING only; GitHub unknown-signer rows shown and verified after download
- Auto-install / Update-all / automation never use the override
- DE + EN; unit tests; ROADMAP / README

## Out of scope

- Repo priority reorder UI, ignored-updates overview, nearby swap, panic mode, local notes
- Global “skip all signature checks” switch; uninstall-then-install replace flow
- GPL F-Droid / IzzyOnDroid code

## Done when

- [ ] Settings hub opens each category; Tools still reaches Privilege / Debloat / … / Activity log
- [ ] PAT save/clear on Updates; Sources status reflects it; config never exports PAT
- [ ] Interval chips reschedule WorkManager; import without field leaves prior interval
- [ ] WARNING: Install disabled + Install anyway confirm; GitHub MEDIUM installable; auto-install still skips `!signatureMatches`
- [ ] `spotlessCheck detekt lint testDebugUnitTest assembleDebug` green
