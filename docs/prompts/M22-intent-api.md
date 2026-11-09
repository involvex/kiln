# M22 — Intent / Broadcast API

> Branch `m22-intent-api`. PLAN §12 P2 (Automatisierung).

## In scope

- Opt-in Settings → **Automation** master switch (off by default)
- Signature permission `dev.involvex.kiln.permission.AUTOMATION`
- Exported `AutomationReceiver` with:
  - `CHECK_UPDATES` — force oneshot WorkManager check (`KEY_FORCE`)
  - `INSTALL_UPDATE` — enqueue managed update when HTTPS + signature matches
  - `INSTALL_FROM_URL` — HTTPS only; first-time installs allowed (`signatureMatches=false`); installed packages still require matching cert via InstallQueue
  - `CLEAN_CACHE` — via `PrivilegedExecutor`; hard-fail without CLEAR_CACHE capability
- Result broadcast `AUTOMATION_RESULT` (same permission) + optional failure notification
- Mark M21 Done (#35) in ROADMAP

## Out of scope

- Debloat / backup / shell / widgets
- Opt-in token auth (signature only in v1)
- Force-installing signature mismatches
- Scheduled maintenance (separate backlog item)

## Done when

- Toggle off → every action returns `automation_disabled` (+ result broadcast)
- Toggle on + same-signer/ADB caller can enqueue check / install / clean
- Non-HTTPS `INSTALL_FROM_URL` rejected; CLEAN_CACHE without privilege fails honestly
- `spotlessCheck detekt lint testDebugUnitTest assembleDebug` green

## Caller sketch (ADB)

```bash
# Enable Automation in Settings first.
adb shell am broadcast \
  -a dev.involvex.kiln.action.CHECK_UPDATES \
  -n dev.involvex.kiln/.automation.AutomationReceiver
```
