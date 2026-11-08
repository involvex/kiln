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

## Other public references

- [APK Explorer & Editor](https://github.com/apk-editor/APK-Explorer-Editor)
- [SmartPack Package Manager](https://github.com/SmartPack/PackageManager/)
- Check each project’s licence before using any code or assets
