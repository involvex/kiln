# M43 — App activities viewer + AppDetail crash fix

> Branch `m43-app-activities`. After M42 (#73) / docs (#74).

## In scope

- Fix App Detail crash: drop AssistedInject; `kilnHiltViewModel` + `load(packageName)` (CatalogDetail pattern)
- App Detail section: list **exported** activities (`GET_ACTIVITIES`); Start via normal Intent; pin shortcut (API 26+)
- Honest copy — no privilege, no non-exported launch
- DE + EN; unit tests for label helpers; quality gates
- Delete local `.phases/` logs (not committed)

## Out of scope

- Services / Receivers / Providers tabs
- Shizuku `am start` for non-exported components
- Copying App Manager / Termux GPL UI

## Done when

- [ ] App Detail opens without ViewModel crash
- [ ] Exported activities listed; Start / pin shortcut work or show clear messages
- [ ] `spotlessCheck detekt lint testDebugUnitTest assembleDebug` green
- [ ] Manual HyperOS checklist in PR

## Manual HyperOS checklist (PR)

- [ ] Open any app detail (no crash)
- [ ] Exported activities section visible; Start opens an activity
- [ ] Create shortcut prompts launcher or shows unsupported/rejected message
- [ ] Filter field appears when many activities
