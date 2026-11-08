# Kiln

All-in-one package, APK, and storage manager for Android.

- **Package:** `dev.involvex.kiln`
- **License:** [Apache-2.0](./LICENSE)
- **Status:** M2 Done — sources, update check, auto-match (see [docs/ROADMAP.md](./docs/ROADMAP.md)); installer is M3

Kiln unifies F-Droid/GitHub sources, APK tools, app backup, storage cleanup, and an optional debloat mode. Privileged actions run through Shizuku/ShizukuPlus or Root when available; Tier NONE stays useful without them.

## Requirements

- JDK 21
- Android SDK (compile/target API 37)
- Android device or emulator (minSdk 26)

## Build

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lint
./gradlew spotlessCheck detekt
./gradlew spotlessApply
```

On Windows use `gradlew.bat`.

Install the debug APK:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Modules (M2)

```
:app                      Navigation 3 shell, Hilt root, theme, FileProvider
:core:common              Result type, qualified dispatchers, fingerprints
:core:model               Installed + source/update domain types
:core:network             OkHttp HTTPS client
:core:database            Room (repos, index cache, ManagedApp)
:core:designsystem        Material 3 theme + base components
:feature:apps             List, detail, export, update badges
:feature:sources          F-Droid/Izzy sync, GitHub, Obtainium import
:feature:onboarding       Auto-match installed apps to sources
:feature:storage          Placeholder (M5)
:feature:backup           Placeholder (M7)
:feature:settings         Placeholder
```

Feature modules depend only on `:core:*`. NavKeys live in `:app`.

## Docs

| Doc | Purpose |
| --- | --- |
| [docs/ROADMAP.md](./docs/ROADMAP.md) | Milestone status & capability matrix |
| [docs/PLAN.md](./docs/PLAN.md) | Vision, architecture, feature specs |
| [AGENTS.md](./AGENTS.md) | Agent / contributor rules |
| [docs/prompts/](./docs/prompts/) | Per-milestone agent prompts |
| [docs/reference.md](./docs/reference.md) | Reference apps (read-only; no GPL copy) |

## Toolchain (M2)

| Component | Version |
| --- | --- |
| AGP | 9.5.1 (built-in Kotlin; no `org.jetbrains.kotlin.android`) |
| Gradle | 9.6.0 |
| Kotlin | 2.4.21 |
| KSP | 2.3.13 |
| Compose BOM | 2026.09.00 |
| Navigation 3 | 1.2.1 |
| Hilt | 2.60.1 |
| Coil | 3.6.3 |
| WorkManager | 2.12.0 |
| Room | 2.8.5 |
| OkHttp | 5.5.1 |

## License

Copyright 2026 involvex. Licensed under the Apache License, Version 2.0.
