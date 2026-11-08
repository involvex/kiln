# Prompt M1 — Installierte Apps + APK-Export (Tier NONE)

> Voraussetzung: M0 ist gemergt (Branch `m1-installed-apps` von `main`). Regeln: `AGENTS.md`. Specs: `docs/PLAN.md`, Status: `docs/ROADMAP.md`.

---

Du arbeitest am Android-Projekt **Kiln** (`dev.involvex.kiln`, Apache-2.0). Lies zuerst `AGENTS.md`, `docs/PLAN.md` (insbesondere §4.1, §4.4, §6, §7, §8, §9) und `docs/ROADMAP.md`. Bearbeite **ausschließlich Meilenstein M1**. Es gibt in M1 **keine Privilegien** (kein Shizuku, kein Root): alles läuft mit normalen App-Rechten.

## Festgelegte Entscheidungen (nicht erneut fragen)

- Muster aus M0 übernehmen (Hilt, Dispatcher, Design System, Navigation 3, Strings DE/EN). Keine neuen Architekturstile.
- Navigation: Liste → Detail **pushen** (kein adaptive List-Detail in M1). NavKeys in `:app`.
- Datenmodell: **Summary** für Liste, **Detail** lazy — siehe unten.
- Zip nur über `java.util.zip`, Kompression **STORED**. Kein zip4j / commons-compress / `apksig` in M1.
- GPL-Referenz-Apps (App Manager, Termux) nur lesen, nie Code kopieren.
- Wenn Foreground-Worker / Permissions für Batch unverhältnismäßig kompliziert werden: **fragen**, bevor auf ViewModel-Scope ausgewichen wird.

## Schritt 1: Erkunden (noch kein Code)

- Lies den Ist-Zustand aus M0: Module, Convention Plugins, Navigation 3, Design System, Hilt-Muster, Beispiel-ViewModel/Tests.
- Prüfe in der offiziellen Android-Dokumentation das Verhalten dieser APIs für API 26–37 und notiere Versionsunterschiede:
  - Package Visibility (`QUERY_ALL_PACKAGES`), `PackageManager.getInstalledPackages` / `PackageInfoFlags` (ab API 33)
  - Signatur: `signingInfo` (ab API 28) vs. `signatures` (API 26–27), `getSigningCertificateHistory` wo vorhanden
  - Installer: `getInstallSourceInfo` (ab API 30) vs. `getInstallerPackageName`
  - Deinstallation: `Intent.ACTION_DELETE`, `REQUEST_DELETE_PACKAGES`
  - SAF: `ACTION_CREATE_DOCUMENT`, `ACTION_OPEN_DOCUMENT_TREE` (+ persistierte Tree-URI)
  - WorkManager Long-Running Worker mit Foreground (`dataSync`), `POST_NOTIFICATIONS` ab API 33
  - Coil 3 Custom Fetcher für Paket-Icons
- Während der Umsetzung: ein reales XAPK (z. B. aus SAI o. Ä.) gegen das geplante `manifest.json` diffen und Abweichungen dokumentieren.
- Liste Annahmen, offene Fragen und einen Umsetzungsplan (max. 12 Schritte). Bei wesentlichen Unklarheiten kurz fragen, sonst fortfahren.

## Schritt 2: Umsetzen

### A. Datenschicht (`:feature:apps` + Modelle in `:core:model`)

**`InstalledAppSummary`** (Liste, schnell ladbar):

- `packageName`, `label`, `versionName`, `versionCode` (Long), `isSystem`, `isUpdatedSystemApp`, `apkSizeBytes` (Long?, `null` = unbekannt), `hasSplits`, `lastUpdateTime`

**`InstalledAppDetail`** (beim Öffnen / vor Export):

- alles aus Summary plus: `installerPackage` (nullable), `firstInstallTime`, `minSdk`, `targetSdk`, `apkPaths` (Base + Splits), `signerSha256` (Liste, aktueller zuerst, danach History wenn API vorhanden), `requestedPermissions` (mit Granted-Status), `hasLaunchIntent`

Kapselung:

- Android-Zugriff hinter `PackageInfoSource` (Implementierung nutzt `PackageManager`).
- Ein `getInstalledPackages`-Durchlauf für Summaries; Detail über gezieltes `getPackageInfo`.
- Repository: `Flow<List<InstalledAppSummary>>` + `suspend fun detail(packageName): …`, läuft auf injiziertem IO-Dispatcher.
- Nur im Speicher — keine Persistenz, kein Upload.
- Refresh bei dynamisch registriertem Receiver `PACKAGE_ADDED` / `REMOVED` / `REPLACED` (`package`-Scheme; ab API 33 `RECEIVER_NOT_EXPORTED`) und beim Zurückkehren in den Vordergrund.
- Größe = Summe der Dateigrößen von `sourceDir` + `splitSourceDirs`. Nicht lesbarer Pfad → Größe unbekannt, Zeile behalten.
- Fingerprint: SHA-256 des Zertifikats, Hex **Großbuchstaben** mit `:`, reine Funktion + Unit-Tests.
- Performance: ~300+ Apps flüssig; Icons und Detail-Felder lazy.

### B. App-Liste (Tab „Apps")

- `LazyColumn` mit stabilen Keys; Item: Icon, Name, Paketname, Version, APK-Größe, Badges (System, Split-APK).
- Icons: Coil-3-`Fetcher`/`Keyer`; Cache-Key = `packageName` + `lastUpdateTime`; Decode nicht auf Main; Adaptive Icons korrekt.
- Suche (Name + Paketname, case-insensitive), Filter-Chips **Nutzer / System / Alle** (Standard: Nutzer), Sortierung Name / Größe / Installationsdatum / Update-Datum (auf-/absteigend). Installationsdatum für Sortierung ggf. aus leichtgewichtigem Feld nachziehen oder Sort-Optionen auf verfügbare Summary-Felder beschränken — Entscheidung im Explore-Schritt kurz begründen.
- Leere und Fehlerzustände mit `EmptyState`, Pull-to-Refresh.
- Mehrfachauswahl per Long-Press, Aktionsleiste „APK exportieren".
- Filter-/Sortierlogik als **reine Funktionen** mit Unit-Tests.

### C. App-Detail

- Anzeige: Name, Paketname, Version (Name + Code), Min/Target-SDK, Install-/Update-Datum, Installer lesbar, Signatur-SHA-256 (Kopieren), APK-Pfade/-Größen, Berechtigungen (gewährt / nicht / normal).
- Installer-Mapping (eigene kleine Map, keine fremden String-Tabellen): Play Store, F-Droid, Droid-ify, Neo Store, Aurora Store, Obtainium, Platform Package Installer; sonst Roh-Paketname; `null` → Unbekannt/Sideload.
- Aktionen: **Starten** (wenn Launch-Intent), **App-Info** (System), **Deinstallieren** (`ACTION_DELETE`; Ergebnis durch erneuten Check beim Zurückkehren; System- und updated-System-Apps deaktiviert + Hinweis auf späteren Debloat), **APK exportieren**.

### D. APK-Export

**Dateiname:** `<sanitizedLabel>_<versionName>_<versionCode>.<ext>` — Sonderzeichen sanitizen, Länge begrenzen, Unit-Tests.

**Formate** (Dialog zeigt nur sinnvolle Optionen):

| Format | Regel |
| --- | --- |
| `.apk` | App ohne Splits: die eine APK. Mit Splits: nur Base + deutlicher Hinweis, dass allein ggf. nicht lauffähig |
| `.apks` | ZIP aller APKs, Originaldateinamen, Methode **STORED**. UI + Code: einfaches ZIP, **kein** bundletool-`.apks` mit `toc.pb` |
| `.xapk` | gleiches ZIP + `manifest.json`: `xapk_version` = 2, `package_name`, `name`, `version_code` (**String**), `version_name`, `min_sdk_version`, `target_sdk_version`, `permissions`, `total_size`, `split_apks` mit `file` + `id` (`base` oder Split-Name). **Kein OBB** in M1. Abweichungen zum geprüften Referenz-XAPK im PR dokumentieren |

**Einzel-Export:** `ACTION_CREATE_DOCUMENT`, Streams, Fortschritt im UI, Abbruch/Fehler löscht unvollständige Zieldatei.

**Teilen:** App-Cache → `FileProvider` Authority `${applicationId}.fileprovider` (Debug-Suffix beachten) → `ACTION_SEND` → Cache aufräumen.

**Batch-Export:** `ACTION_OPEN_DOCUMENT_TREE` (URI persistieren), WorkManager Long-Running Worker + Foreground `dataSync`, Notification „3/12" + Abbrechen, Ergebnis-Zusammenfassung (ok/fehlgeschlagen + Grund). `POST_NOTIFICATIONS` einmalig mit Erklärtext beim ersten Batch; Ablehnung blockiert den Export **nicht**.

Klassen `ApkBundleWriter` / `XapkManifestBuilder` testbar (JVM, Temp-Dateien).

### E. Manifest & Berechtigungen (nur das Nötige, jede im PR begründen)

- `QUERY_ALL_PACKAGES`, `REQUEST_DELETE_PACKAGES`
- Batch-Worker: `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC`, `POST_NOTIFICATIONS`
- `FileProvider` nur für Export-Cache
- **Keine** Storage-Permissions (`MANAGE_EXTERNAL_STORAGE` etc.) — SAF reicht
- Play-Lint `QueryAllPackagesPermission` mit Kommentar unterdrücken: Kiln wird sideloaded, nicht über Play verteilt

### F. Qualität

- UI-Texte DE + EN
- ViewModel-Tests (Turbine): Liste/Suche/Filter/Auswahl, Export-Zustände
- Unit-Tests: Fingerprint-Format, Filter/Sort, Dateiname-Sanitizing, `ApkBundleWriter`, `XapkManifestBuilder`
- `./gradlew spotlessCheck detekt lint testDebugUnitTest assembleDebug` grün, CI grün
- Capability-Matrix in `docs/ROADMAP.md` für M1 aktualisieren

## Randbedingungen

- Nichts aus M2 oder später (keine Quellen, kein Netzwerk-Feature, keine Updates, kein Installer, kein Room, kein Shizuku)
- Keine Telemetrie; App-Liste weder speichern noch senden
- Branch `m1-installed-apps`, kleine Conventional Commits
- Kein GPL-Code aus Referenz-Repos

## Akzeptanzkriterien (einzeln im Abschluss-Report abhaken)

- [ ] Anzahl der Nutzer-Apps in Kiln entspricht `adb shell pm list packages -3` (Abweichungen erklären); System-Apps über Filter sichtbar
- [ ] Liste scrollt flüssig, Icons ohne Ruckeln, Suche/Filter/Sortierung funktionieren
- [ ] Detail zeigt korrekte Version, Installer, Signatur-SHA-256 (Stichprobe gegen `apksigner verify --print-certs` oder `keytool`)
- [ ] Export einer Nicht-Split-App als `.apk` und installierbar auf zweitem Gerät **oder** per `adb install` nach Deinstall (Fallback dokumentieren)
- [ ] Export einer Split-App als `.apks`/`.xapk`; Test: `adb install-multiple` (oder zweites Gerät) startet die App
- [ ] Batch-Export von mind. 5 Apps mit Fortschritt, Abbruch und Zusammenfassung
- [ ] Deinstallation per System-Dialog; Liste aktualisiert sich; System-Apps nicht deinstallierbar markiert
- [ ] API 26–27 Signatur-Fallback und Betrieb ohne Notification-Permission behandelt/dokumentiert
- [ ] Alle Strings DE + EN; Build/Lint/Tests/CI grün; `docs/ROADMAP.md` aktualisiert

## Abschluss-Report (am Ende ausgeben)

1. Was wurde gebaut (Stichpunkte, betroffene Module)
2. Manuelle Testergebnisse (HyperOS: Dialoge, Notifications, SAF-Dateinamen)
3. Abweichungen vom Plan und warum, inkl. XAPK-Format-Entscheidungen
4. Bekannte Einschränkungen / offene Punkte
5. Empfehlungen für M2 (welche `InstalledApp*`-Felder Auto-Match/Update-Check braucht)
