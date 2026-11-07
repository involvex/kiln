# Prompt M1 — Installierte Apps + APK-Export (Tier NONE)

> Voraussetzung: M0 ist gemergt (Branch `m1-installed-apps` von `main`). `AGENTS.md` und `PLAN.md` liegen im Repo-Root.

---

Du arbeitest am Android-Projekt **Kiln** (`dev.involvex.kiln`). Lies zuerst `AGENTS.md` und `PLAN.md` (insbesondere §4.1 Installierte Apps, §4.4 APK-Extraktion, §6 Permissions, §7 Sicherheit, §8 M1). Bearbeite **ausschließlich Meilenstein M1**. Es gibt in M1 **keine Privilegien** (kein Shizuku, kein Root): alles läuft mit normalen App-Rechten.

## Schritt 1: Erkunden (noch kein Code)

- Lies den Ist-Zustand aus M0: Module, Convention Plugins, Navigation, Design System, Hilt-Muster, Beispiel-ViewModel/Tests. **Übernimm die bestehenden Muster**, erfinde keine neuen.
- Prüfe in der offiziellen Android-Dokumentation das Verhalten dieser APIs für API 26–aktuell und notiere Versionsunterschiede, die du im Code behandeln musst:
  - Package Visibility (`QUERY_ALL_PACKAGES`), `PackageManager.getInstalledPackages` / `PackageInfoFlags` (ab API 33)
  - Signatur-Auslesen: `signingInfo` (ab API 28) vs. `signatures` (API 26–27), `getSigningCertificateHistory`
  - Installer-Herkunft: `getInstallSourceInfo` (ab API 30) vs. `getInstallerPackageName`
  - Deinstallation per `Intent.ACTION_DELETE` und `REQUEST_DELETE_PACKAGES`
  - SAF: `ACTION_CREATE_DOCUMENT`, `ACTION_OPEN_DOCUMENT_TREE`
  - WorkManager Long-Running Worker mit Foreground (Service-Typ `dataSync`), `POST_NOTIFICATIONS` ab API 33
- Liste Annahmen, offene Fragen und einen Umsetzungsplan (max. 12 Schritte). Bei wesentlichen Unklarheiten kurz fragen, sonst fortfahren.

## Schritt 2: Umsetzen

### A. Datenschicht (`:feature:apps`, Daten-Interfaces austauschbar für Tests)

- `InstalledApp`-Modell in `:core:model`: `packageName`, `label`, `versionName`, `versionCode` (Long), `isSystem`, `isUpdatedSystemApp`, `installerPackage` (nullable), `firstInstallTime`, `lastUpdateTime`, `minSdk`, `targetSdk`, `apkPaths` (Base + Splits), `apkSizeBytes` (Summe aller APK-Dateien), `hasSplits`, `signerSha256` (Liste von Fingerprints, aktueller zuerst), `requestedPermissions` (mit Granted-Status).
- Android-Zugriff hinter einem Interface (`PackageInfoSource`) kapseln, Implementierung nutzt `PackageManager`. Repository liefert einen `Flow<List<InstalledApp>>`, lädt auf dem injizierten IO-Dispatcher und **aktualisiert sich** bei `PACKAGE_ADDED`/`REMOVED`/`REPLACED` (dynamisch registrierter Receiver mit `package`-Scheme) und beim Zurückkehren in die App.
- Fingerprint-Formatierung (SHA-256, Hex, Großbuchstaben, mit `:`) als reine Funktion mit Unit-Tests.
- Performance: ~300+ Apps müssen flüssig laden. Teure Dinge (Icons, Berechtigungsdetails) **lazy** und nur für sichtbare/geöffnete Einträge laden.

### B. App-Liste (Tab „Apps")

- `LazyColumn` mit stabilen Keys, Item: Icon, Name, Paketname, Version, APK-Größe, Badges (System, Split-APK).
- Icons: eigener Coil-`Fetcher`/`Keyer` für Paketnamen (oder gleichwertige Lösung) mit Memory-Cache; Adaptive Icons korrekt rendern.
- Suche (Name + Paketname, case-insensitive), Filter-Chips **Nutzer / System / Alle** (Standard: Nutzer), Sortierung nach Name, Größe, Installationsdatum, Update-Datum (auf-/absteigend).
- Leere und Fehlerzustände mit `EmptyState`, Pull-to-Refresh.
- Mehrfachauswahl per Long-Press mit Aktionsleiste („APK exportieren"). Filter-/Sortierlogik als **reine Funktionen** mit Unit-Tests.

### C. App-Detail

- Anzeige: Name, Paketname, Version (Name + Code), Min/Target-SDK, Installationsdatum, letztes Update, Installer-Herkunft (lesbar, z. B. „F-Droid", „Play Store", „Unbekannt/Sideload"), Signatur-SHA-256 (mit Kopieren-Button), APK-Pfade und -Größen, Berechtigungsliste (gewährt/nicht gewährt/normal).
- Aktionen: **Starten** (falls Launch-Intent existiert), **App-Info** (System-Einstellungen), **Deinstallieren** (System-Dialog via `Intent.ACTION_DELETE`, Ergebnis über erneuten Check beim Zurückkehren; für System-Apps deaktiviert mit kurzer Erklärung, dass dafür später der Debloat-Modus vorgesehen ist), **APK exportieren**.

### D. APK-Export

- **Formate** (Auswahl-Dialog, nur sinnvolle Optionen anzeigen):
  - `.apk` — bei Apps ohne Splits die einzige APK; bei Split-Apps nur Base-APK mit **deutlichem Hinweis**, dass die App so allein evtl. nicht lauffähig ist
  - `.apks` — ZIP mit allen APKs (Base + Splits), Originaldateinamen, keine/geringe Kompression. Hinweis in der UI und im Code: das ist ein einfaches ZIP, **kein** bundletool-`.apks` mit `toc.pb`
  - `.xapk` — ZIP mit allen APKs plus `manifest.json` (`xapk_version`, `package_name`, `name`, `version_code`, `version_name`, `min_sdk_version`, `target_sdk_version`, `permissions`, `split_apks` mit `file` und `id`). Prüfe das Format gegen eine verlässliche Quelle bzw. ein echtes XAPK und dokumentiere Abweichungen
- **Dateiname:** `<Label>_<versionName>_<versionCode>.<ext>` (Sonderzeichen sanitizen)
- **Einzelexport:** Zieldatei per `ACTION_CREATE_DOCUMENT`, Kopieren mit Streams, Fortschritt im UI, sauberes Abbrechen/Aufräumen bei Fehler.
- **Teilen:** in den App-Cache schreiben, über `FileProvider` + `ACTION_SEND` teilen, Cache danach aufräumen.
- **Batch-Export** (Mehrfachauswahl): Zielordner per `ACTION_OPEN_DOCUMENT_TREE` (Berechtigung persistieren), Verarbeitung in einem **WorkManager-Long-Running-Worker** mit Foreground-Benachrichtigung (Fortschritt „3/12", Abbrechen-Aktion), Ergebnis-Zusammenfassung (erfolgreich/fehlgeschlagen mit Grund). Falls Foreground-Worker/Permissions für M1 unverhältnismäßig kompliziert werden, **frage mich**, bevor du auf eine einfachere Variante (ViewModel-Scope mit Fortschritts-UI) ausweichst.
- Kein Root-/Shizuku-Zugriff nötig: Quelle sind die lesbaren Pfade aus `ApplicationInfo.sourceDir` und `splitSourceDirs`.
- Zip-/Manifest-Logik als eigene, testbare Klassen (`ApkBundleWriter`, `XapkManifestBuilder`) mit JVM-Unit-Tests (Temp-Dateien).

### E. Manifest & Berechtigungen (nur das Nötige, jede im PR begründen)

- `QUERY_ALL_PACKAGES`, `REQUEST_DELETE_PACKAGES`
- Für den Batch-Worker: `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC`, `POST_NOTIFICATIONS` (Laufzeitabfrage erst beim ersten Batch-Export mit Erklärtext; Export muss auch ohne Benachrichtigungsrecht funktionieren)
- `FileProvider` mit eng gefasster Pfaddefinition (nur Export-Cache)
- **Keine** Storage-Permissions (`MANAGE_EXTERNAL_STORAGE` u. ä.), SAF genügt

### F. Qualität

- UI-Texte in DE + EN, keine hartcodierten Strings
- ViewModel-Tests (Turbine) für Liste/Suche/Filter/Auswahl und Export-Zustände
- Unit-Tests: Fingerprint-Format, Filter/Sortierung, Dateiname-Sanitizing, `ApkBundleWriter`, `XapkManifestBuilder`
- `./gradlew spotlessCheck detekt lint testDebugUnitTest assembleDebug` grün, CI grün

## Randbedingungen

- Nichts aus M2 oder später (keine Quellen, kein Netzwerk, keine Updates, kein Installer, kein Room, kein Shizuku)
- Keine Telemetrie; die App-Liste wird nirgends gespeichert oder gesendet (nur im Speicher)
- Branch `m1-installed-apps`, kleine Commits (Conventional Commits)

## Akzeptanzkriterien (einzeln im Abschluss-Report abhaken)

- [ ] Anzahl der Nutzer-Apps in Kiln entspricht `adb shell pm list packages -3` (Abweichungen erklären); System-Apps über Filter sichtbar
- [ ] Liste scrollt flüssig, Icons erscheinen ohne Ruckeln, Suche/Filter/Sortierung funktionieren
- [ ] Detailansicht zeigt korrekte Version, Installer, Signatur-SHA-256 (gegen `apksigner verify --print-certs` oder `keytool` stichprobenartig geprüft)
- [ ] Export einer Nicht-Split-App als `.apk` und installierbar auf einem zweiten Gerät
- [ ] Export einer Split-App als `.apks`/`.xapk`; Test: alle enthaltenen APKs lassen sich per `adb install-multiple` auf einem zweiten Gerät installieren und die App startet
- [ ] Batch-Export von mind. 5 Apps mit Fortschritt, Abbruch und Zusammenfassung
- [ ] Deinstallation per System-Dialog klappt, Liste aktualisiert sich automatisch; System-Apps sind nicht deinstallierbar markiert
- [ ] Verhalten auf API 26–27 (Signatur-Fallback) bzw. ohne Notification-Berechtigung ist behandelt und getestet bzw. begründet dokumentiert
- [ ] Alle Strings DE + EN, Build/Lint/Tests/CI grün

## Abschluss-Report (am Ende ausgeben)

1. Was wurde gebaut (Stichpunkte, betroffene Module)
2. Manuelle Testergebnisse auf dem Gerät (HyperOS-Besonderheiten beachten, z. B. Installationsdialoge, Benachrichtigungen, Dateinamen im SAF-Dialog)
3. Abweichungen vom Plan und warum, inkl. XAPK-Format-Entscheidungen
4. Bekannte Einschränkungen / offene Punkte
5. Empfehlungen für M2 (Sources, Update-Check, Auto-Match), insbesondere welche Daten aus `InstalledApp` M2 braucht
