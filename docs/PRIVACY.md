# Privacy policy — Kiln

**Effective date:** 2026-11-08  
**Package:** `dev.involvex.kiln`  
**Contact:** via [GitHub issues](https://github.com/involvex/kiln/issues)

## Summary

Kiln is a local package and storage tool. It does **not** include analytics, advertising, crash-reporting SDKs, or telemetry. Installed-app lists and matching stay on the device.

## Data Kiln processes on the device

- Installed package metadata (names, versions, signatures, sizes) for listing, updates, export, backup, storage, and debloat
- User-configured sources (F-Droid-style repos, GitHub mappings), preferences (theme, language, installer, background updates, optional Termux package id)
- Optional encrypted secrets such as a GitHub personal access token (Android Keystore / encrypted storage — never logged, never committed)
- Local caches (repo indexes, APK rollback copies, backups you choose to write via SAF)

## Data that leaves the device

Only when **you** trigger a network feature:

- HTTPS requests to configured F-Droid-compatible repos and GitHub (or other sources you add) for indexes and APK downloads
- No bulk upload of your installed-app list

## Permissions

Permissions are requested for features you use (e.g. notifications for background update checks, install packages, usage access for storage sizes, optional Termux `RUN_COMMAND`). See the in-app setup screens for why each is needed.

## Third parties

- **F-Droid / IzzyOnDroid / custom repos** and **GitHub** receive normal HTTPS client traffic for the sources you configure
- **Shizuku / Termux** (optional): IPC only when you enable those integrations

## Changes

Material updates to this policy will be noted in the repository changelog and this file’s effective date.
