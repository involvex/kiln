# Kiln

All-in-one package, APK, and storage manager for Android.

- **Package:** `dev.involvex.kiln`
- **License:** [Apache-2.0](./LICENSE)
- **Status:** M11 Discovery on main; M12 GitHub browse in progress (see [docs/ROADMAP.md](./docs/ROADMAP.md))
- **Version:** `0.1.0` (versionCode 2)

Kiln unifies F-Droid/GitHub sources, APK tools, app backup, storage cleanup, optional debloat, optional Termux hooks, and F-Droid catalog discovery. Privileged actions run through Shizuku/ShizukuPlus or Root when available; Tier NONE stays useful without them.

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

## Modules

```
:app                      Navigation 3 shell, Hilt root, theme, FileProvider, AppBindingsModule
:core:common              Result type, qualified dispatchers, fingerprints, byte sizes
:core:model               Domain types + background-update / Termux prefs contracts
:core:network             OkHttp HTTPS client (+ resumable download)
:core:database            Room (repos, index cache, ManagedApp, install jobs)
:core:designsystem        Material 3 theme + base components
:core:privilege           PrivilegedExecutor (Shizuku UserService allowlist)
:feature:apps             List, detail, export, update badges + install CTA
:feature:sources          F-Droid/Izzy sync, GitHub, Obtainium import, Discover catalog; UpdateCheckWorker
:feature:installer        Download queue; PackageInstaller or silent pm; local APK enqueue
:feature:onboarding       Auto-match + Shizuku setup assistant (A→D)
:feature:storage          StorageStats analyzer + cache clear (usage access)
:feature:debloat          Debloat (denylist, snapshot, disable/restore)
:feature:backup           APK .kilnbackup.zip via SAF + restore via InstallQueue
:feature:termux           Optional RUN_COMMAND (feature flag, configurable packageId)
:feature:settings         Privilege, Debloat, Termux, About, background-update DataStore toggles
```

Feature modules depend only on `:core:*`. Cross-feature binds live in `:app`. NavKeys live in `:app`.

## Docs

| Doc | Purpose |
| --- | --- |
| [docs/ROADMAP.md](./docs/ROADMAP.md) | Milestone status & capability matrix |
| [docs/PLAN.md](./docs/PLAN.md) | Vision, architecture, feature specs |
| [AGENTS.md](./AGENTS.md) | Agent / contributor rules |
| [docs/prompts/](./docs/prompts/) | Per-milestone agent prompts |
| [docs/reference.md](./docs/reference.md) | Reference apps (read-only; no GPL copy) |
| [CHANGELOG.md](./CHANGELOG.md) | User-facing changes |
| [docs/PRIVACY.md](./docs/PRIVACY.md) | Privacy policy |
| [docs/RELEASE.md](./docs/RELEASE.md) | Tagging, signing, F-Droid |
| [fastlane/metadata/android/](./fastlane/metadata/android/) | Store listing copy (en/de) |

## Toolchain (M4)

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

Confirm current pins in [`gradle/libs.versions.toml`](./gradle/libs.versions.toml) before bumping.
