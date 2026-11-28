# Reference Apps

Public projects used only to observe behaviour, UX, and API hints.

**Rule:** Kiln is **Apache-2.0**. Ideas and observations are allowed. **Do not** copy code, comments, string tables, or layouts from GPL projects into Kiln. Apache-2.0 samples (e.g. Shizuku) only when a milestone needs them, with attribution / checked file licence.

Keep reference checkouts **outside** this repository. Do not commit machine-local paths.

## Shizuku / Shizuku Plus

- Upstream: [RikkaApps/Shizuku](https://github.com/RikkaApps/Shizuku) (and forks branded “Shizuku Plus”)
- SPDX: **Apache-2.0** — confirm `LICENSE` in the checkout you use
- Before privileged features: compare API differences vs stock Shizuku

## Termux

- Upstream: [termux/termux-app](https://github.com/termux/termux-app)
- SPDX: **GPL-3.0-only** (sub-libraries may differ — check before any reuse)
- For Kiln: **read behaviour only** (e.g. `RUN_COMMAND`); never copy Termux source

## App Manager

- Upstream: [MuntashirAkon/AppManager](https://github.com/MuntashirAkon/AppManager)
- SPDX: **GPL-3.0-or-later**
- For Kiln: **read behaviour only**

## REAndroid (APK merge)

- Upstream: [REAndroid/ARSCLib](https://github.com/REAndroid/ARSCLib) (Maven `io.github.reandroid:ARSCLib`) and [REAndroid/APKEditor](https://github.com/REAndroid/APKEditor) merge behaviour
- SPDX: **Apache-2.0**
- Kiln uses ARSCLib for in-app universal split merge (M35) and attributes APKEditor-style META-INF clearing / merge flow in `NOTICE`
- Do **not** copy GPL AntiSplit-M or other GPL merge UIs

## ADB Captain (diagnostics ideas)

- Upstream: [eatenlamp/adbcaptain](https://github.com/eatenlamp/adbcaptain)
- SPDX: **AGPL-3.0-or-later**
- For Kiln: **read behaviour / command list only** (e.g. `dumpsys battery`, `dumpsys netstats`, `top -n 1`, `dumpsys cpuinfo`, `dumpsys thermalservice`). Never copy parsers, UI, strings, or layouts into Kiln (Apache-2.0). Never use `cmd battery`.

## Universal Android Debloater NG (debloat list — not cleared)

- Upstream: [Universal-Debloater-Alliance/universal-android-debloater-next-generation](https://github.com/Universal-Debloater-Alliance/universal-android-debloater-next-generation) (lists: `resources/assets/uad_lists.json`)
- SPDX (repo LICENSE, verified 2026-11-28): **GPL-3.0** (GNU GPL v3). The package catalogue is treated as the same copyleft data; other apps that ship it (e.g. OpenADB) also label it GPL-3.0.
- **Kiln decision (M68):** **not cleared** for bundling into Apache-2.0 Kiln. Do **not** import `uad_lists.json` into `debloat-db.json`. Do not invent substitute OEM ratings. Behaviour observation only.
- Cleared import later requires an approved **Apache-2.0 / CC0 / explicitly dual-licensed / Kiln-first-party curated** source, SPDX in this file, attribution in root `NOTICE`, then a PR that maps ratings 1:1 (no AI-invented rows).

## Other public references

- [APK Explorer & Editor](https://github.com/apk-editor/APK-Explorer-Editor)
- [SmartPack Package Manager](https://github.com/SmartPack/PackageManager/)
- Check each project’s licence before using any code or assets
