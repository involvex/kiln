# M55 — Dual-user installs (best-effort `--user N`)

> Branch `m55-dual-user-installs`. After M53. Optional: when the census lists more than one Android user, let install/uninstall target a chosen user id.

## Intent

M53 already shows `pm list users`. This milestone **wires that list into install/uninstall** when it is useful — and **does nothing special** when the device only has user 0 (typical single-user phone). That is success, not a bug.

Not a HyperOS-only feature. Dual Apps / work profiles / secondary users are just cases where multiple ids appear. No manufacturer checklist gate for this milestone.

## In scope

- Reuse M53 census (`listAndroidUsers` / `AndroidUserInfo`); do not invent users
- When **≥2 users** and Shizuku can install/uninstall: optional user picker on **confirm** dialogs for PackageInstaller / silent `pm install` / `pm uninstall` (and user-uninstall restore paths that already use `--user`)
- Default selection = current user (`Process.myUid() / 100_000`); never silently switch users
- Shell: fixed allowlist argv only — numeric `--user <id>` from the census set; **never** concatenate free text
- Tier NONE / single-user / missing capability: keep today’s behaviour (current user only); no dead controls
- DE + EN strings; activity log notes target user id when ≠ current
- Unit tests: allowlist rejects bad ids; picker hidden when `users.size < 2`
- Docs: ROADMAP + capability matrix; mark dual-user Partial → Done (best-effort)

## Out of scope

- HyperOS Dual Apps “setup” or Second space provisioning
- Debloat / backup / freeze / component toggles per user (stay user 0 unless a later milestone)
- Cloud / ROOT `/data/data` / tablet layouts / F-Droid / self-hosted repo
- Manufacturer E2E checklists as acceptance for this PR

## Done when

- Device with only user 0: install/uninstall UX unchanged; tests green
- Device/emulator with ≥2 listed users + Shizuku: confirm dialog can target another listed id; wrong/unknown id refused
- `spotlessCheck detekt lint testDebugUnitTest assembleDebug` green

## Manual check (optional, any device)

- [ ] Single-user: no user picker; install still works
- [ ] If census shows ≥2 users: pick non-current user → install or uninstall affects that user only (confirm copy shows id/name)
