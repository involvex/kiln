# M31 — APK tools 2b (resign + split export)

> Branch `m31-apk-tools-merge-resign`. PLAN §12 P3 (APK-Werkzeuge Phase 2 — remaining slice after M29). After M30 (#48).

## In scope

- Manage → APK tools: **Resign** a SAF-picked `.apk` with a user PKCS12/BKS keystore (also via SAF)
  - Use `com.android.tools.build:apksig` (v2/v3 schemes as supported by the library)
  - Keystore + key passwords stay in memory for the operation only — never DataStore, never logs, never Kiln-Config
  - Strong UI warning: resign breaks update path from the original signer
  - Save signed APK via SAF create document
- **Split export**: from `.apks` / `.xapk`, extract member `.apk` files to a SAF tree (honest multi-APK export; not a universal single APK)
- Unit tests for signer/export helpers where pure-JVM possible; DE + EN strings
- Docs: ROADMAP M30 Done, M31 In progress → Done on merge; backlog APK tools marked Done (true universal merge still deferred)

## Out of scope

- True merge of splits into one installable universal APK (needs apktool-class resource surgery → later / Termux)
- Embedding private keys; cloud keystores; biometric-wrapped key storage
- Auto-install of resigned APKs; changing installer signature gate defaults
- Coil proxy follow-up from M30; Exodus; freeze/hibernate; F-Droid submit

## Security assumptions (confirm if wrong)

1. User supplies their own keystore file; Kiln does not generate or retain keys by default
2. Passwords cleared from UI state after success/failure
3. Destructive/confusing action gated by explicit confirmation copy

## Done when

- Resign flow: pick APK + keystore + alias/passwords → signed APK saved; mismatch/wrong password shows clear error
- Split export: `.apks`/`.xapk` members written to chosen folder
- `spotlessCheck detekt lint testDebugUnitTest assembleDebug` green
