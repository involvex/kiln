# M20 — Play Store deep-link (unmatched apps)

> Branch `m20-play-deeplink`. Honest link-out only — no Play download API.

## In scope

- App detail: **Open in Play Store** when there is no managed F-Droid/GitHub link **and** installer is Play (`com.android.vending`) or Aurora (`com.aurora.store` / `.nightly`)
- Intent: `market://details?id=<pkg>` first, https Play URL fallback
- EN + DE strings; unit tests for offer rules + URL builders

## Out of scope

- Auto-Match NONE row CTA
- Play search/download/update API, AAS tokens, GPL `gplayapi`
- Claiming a listing exists (no network verify)
- Sideload / unknown installer

## Done when

- CTA visible only for unmanaged Play/Aurora-installed apps
- Tap opens Play app or https fallback
- `spotlessCheck detekt lint testDebugUnitTest assembleDebug` green
