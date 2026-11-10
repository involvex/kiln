# PLAN.md — Kiln

> All-in-One Package-, APK- und Storage-Manager für Android.
> Dieses Dokument ist die Arbeitsgrundlage für Cursor / AI-Agents. Bitte zuerst komplett lesen, dann Meilenstein für Meilenstein umsetzen.

---

## 1. Vision

Eine App, die diese Dinge ersetzt:

| Heute (separate Apps)                       | In Kiln                                                               |
| ------------------------------------------- | --------------------------------------------------------------------- |
| F-Droid, IzzyOnDroid, Droid-ify/Neo Store   | Source „F-Droid-Repos" (beliebige Repos hinzufügbar)                  |
| Discoverium / Obtainium / Repo Store        | Source „GitHub Releases" (+ Codeberg/GitLab später)                   |
| APK Extractor, XAPK Extractor, APK Explorer | APK-/Split-Extraktion, Export als `.apk` / `.apks` / `.xapk`          |
| Package Manager, DroidKit-Teile             | Installierte Apps: Details, Install, Deinstall, Berechtigungen        |
| Cleaner / Storage-Analyse                   | Cache-Cleaner, Storage-Analyzer, große Dateien, Reste verwaister Apps |
| Backup-Tools                                | App-Backup (APK + Daten, je nach Berechtigungsstufe)                  |

**Kernprinzip: Capability-Tiers.** Die App muss ohne Spezialrechte nützlich sein und mit Shizuku/Root mehr können. Jedes Feature deklariert, welche Stufe es braucht, und die UI zeigt das ehrlich an (kein stilles Scheitern).

## 2. Tech-Entscheidung: reines Kotlin + Jetpack Compose

**Begründung (nicht verhandeln, außer es gibt neue Fakten):**

- Fast alle Kernfunktionen sind native Android-APIs: `PackageInstaller`, `PackageManager`, `StorageStatsManager`, `UsageStatsManager`, SAF, Foreground Services, WorkManager, Shizuku-Binder/AIDL (`UserService`).
- Mit Expo/React Native müsste man all das als Kotlin-Module schreiben und dann über die Bridge streamen (Download-Fortschritt, riesige App-/Dateilisten, Binder-Callbacks). Der Aufwand liegt trotzdem in Kotlin, nur mit Extra-Schicht.
- Shizuku `UserService` läuft in einem eigenen Prozess und braucht AIDL — das ist nativ am saubersten.
- Expo-Go ist ausgeschlossen, Dev-Builds nötig → kein Vorteil beim Iterieren.

**Lizenz:** Apache-2.0. Referenz-Apps nur als Verhaltensvorlage lesen; kein Code, keine Strings, kein Layout aus GPL-Quellen übernehmen (siehe §9 und [`reference.md`](./reference.md)). Status der Meilensteine: [`ROADMAP.md`](./ROADMAP.md).

**Stack**

- Kotlin (aktuell stabil), Gradle Kotlin DSL, Version Catalog (`libs.versions.toml`), immer die jeweils aktuell stabilen Versionen verwenden (Baseline und Re-Check-Regel: §2.1)
- Name: **Kiln** · `applicationId` / Package: `dev.involvex.kiln` (Root-Package im Code ebenfalls `dev.involvex.kiln`, Module darunter z. B. `dev.involvex.kiln.feature.apps`)
- minSdk 26, compileSdk/targetSdk = aktuelles Stable (Android 17 / API 37 Stand 2026-11)
- UI: Jetpack Compose + Material 3 (stabil; **kein** Material 3 Expressive, solange es nicht im stabilen Compose BOM liegt), **Navigation 3** (`androidx.navigation3`, type-safe `@Serializable`-NavKeys), Dynamic Color + AMOLED-Dark-Option
- DI: **Hilt** (KSP; kein Koin, keine kapt)
- Persistenz: Room (Repo-Index-Cache, Backups, Update-State; ab M2), DataStore (Settings; wenn Settings-Persistenz nötig)
- Netzwerk: OkHttp + kotlinx.serialization (Ktor optional; ab M2)
- Hintergrund: WorkManager (Update-Checks, Batch-Export ab M1) + Foreground Service (Downloads/Backups/Scans)
- Bilder: Coil (ab M1)
- Privileged: `dev.rikka.shizuku:api` + `provider`, optional `libsu` (Root; ab M4)
- APK-Handling: Export in M1 über `java.util.zip` (STORED); `com.android.tools.build:apksig` erst wenn Signatur-Verify für fremde Dateien nötig wird; **kein** zip4j / commons-compress in M0/M1
- Tests: JUnit5, MockK, Turbine; Compose UI Tests für Kernscreens

### 2.1 Toolchain-Baseline (Stand 2026-11-08)

Vor dem Start von M0 Patch-Versionen an offiziellen Quellen neu prüfen. Bei Abweichung: stoppen und nachfragen, nicht raten.

| Komponente | Baseline |
| --- | --- |
| JDK | 21 (AGP-Minimum 17) |
| AGP | 9.5.1 |
| Gradle | 9.6.0 |
| Kotlin | 2.4.21 — AGP 9 hat **Built-in Kotlin**; `org.jetbrains.kotlin.android` **nicht** anwenden. Compose- und Serialization-Plugins weiter explizit. |
| Compose BOM | 2026.09.00 · activity-compose 1.13.0 |
| Navigation 3 | 1.2.1 |
| Hilt | 2.60.1 · androidx.hilt 1.4.0 · KSP (neueste stabile, die Kotlin 2.4.21 unterstützt; zuletzt geprüft: 2.3.13) |
| Coil / WorkManager | 3.6.3 / 2.12.0 — **erst M1** |

API 37: keine feste `screenOrientation` setzen (Large-Screen-Resizing ist erzwungen).

## 3. Architektur

Multi-Module, Clean-ish, unidirektionaler Datenfluss (ViewModel + `StateFlow`).

Navigation: **Navigation 3** in `:app` (`NavDisplay`, Bottom Bar mit einem Back-Stack pro Tab laut offiziellem Recipe). Feature-Module exportieren nur Screen-Composables; NavKeys liegen in `:app`. Adaptive List-Detail-Scenes erst später; Phone-Flow in M0/M1 ist Liste → Detail pushen. **Kein** `androidx.navigation:navigation-compose` (Navigation 2).

```
:app                      // Navigation 3, DI-Root, Theme
:core:common              // Result-Typen, Dispatcher, Utils
:core:model               // reine Datenklassen
:core:database            // Room (ab M2)
:core:network             // OkHttp, Serializer (ab M2)
:core:designsystem        // Compose-Komponenten
:core:privilege           // PrivilegedExecutor + Implementierungen (siehe §5; ab M4)
:feature:apps             // installierte Apps, Details, Extract, Uninstall
:feature:sources          // F-Droid-/GitHub-Repos, Browse, Update-Check
:feature:installer        // Download, Verify, Install-Sessions, Queue
:feature:storage          // Analyzer, Cleaner, Large Files
:feature:backup           // Backup/Restore
:feature:onboarding        // Auto-Match installierter Apps + Shizuku-Setup-Assistent
:feature:debloat           // Debloat-Modus (braucht Privilege-Tier >= SHIZUKU)
:feature:permissions      // Berechtigungs-Audit (Übersicht Tier NONE; AppOps/revoke ab SHIZUKU)
:feature:termux           // optional, Feature-Flag
:feature:settings
```

Wichtige Abstraktionen:

```kotlin
interface AppSource {            // F-Droid-Repo, GitHub, später Codeberg/GitLab/direkte URL
    val id: String
    suspend fun fetchCatalog(): List<RemoteApp>
    suspend fun fetchReleases(app: RemoteApp): List<Release>
}

interface PrivilegedExecutor {   // Tier-Abstraktion
    val tier: Tier               // NONE, SHIZUKU, SHIZUKU_PLUS, ROOT
    suspend fun installApk(session: InstallRequest): InstallResult
    suspend fun uninstall(pkg: String, keepData: Boolean): Result<Unit>
    suspend fun clearCache(pkg: String?): Result<Long>      // null = alle
    suspend fun runShell(cmd: String): ShellResult
}
```

Die UI fragt nie „ist Shizuku da?", sondern `capabilities: Set<Capability>` aus dem aktiven Executor.

## 4. Features im Detail

### 4.1 Installierte Apps (`feature:apps`)

**M1 liefert (Tier NONE):**

- Liste mit Icon, Name, Version, **APK-Dateigröße** (Summe Base + Splits), System/User-Filter, Suche, Sortierung, Mehrfachauswahl für Batch-Export
- Detail: Paketname, Version/Code, Signatur-SHA-256, Min-/Target-SDK, Installer-of-Record, Installations-/Update-Datum, APK-Pfade, Berechtigungen (gewährt/nicht)
- Aktionen: App-Info (System-Intent), Starten (wenn Launch-Intent), Deinstallieren (System-Dialog; System-Apps deaktiviert mit Hinweis auf späteren Debloat), APK exportieren (siehe §4.4)
- Datenmodell gesplittet: `InstalledAppSummary` für die Liste, `InstalledAppDetail` lazy beim Öffnen/Export — siehe [`prompts/M1-installed-apps.md`](./prompts/M1-installed-apps.md)
- Braucht `QUERY_ALL_PACKAGES` (Sideload-Distribution, kein Play Store)

**Später (nicht M1):**

- Daten-/Cache-Größe → M5 (`PACKAGE_USAGE_STATS` / `StorageStatsManager`)
- Update-Badge → M2
- Komponentenliste (Activities/Services/…) → Backlog / spätere Meilensteine
- Cache leeren → M5; Backup → M7

### 4.2 Sources & Updates (`feature:sources`)

- **F-Droid-Format** (gilt auch für IzzyOnDroid und eigene Repos): `index-v2.json` laden, **Signatur via `entry.jar` prüfen**, Diffs nutzen wenn vorhanden, lokal in Room cachen. Repos mit Name/URL/Fingerprint hinzufügbar, vorkonfiguriert: F-Droid, IzzyOnDroid
- **GitHub Releases**: `GET /repos/{owner}/{repo}/releases`; pro App konfigurierbar: Asset-Regex, ABI-Präferenz (arm64-v8a), Pre-Releases ja/nein, Versions-Parsing-Regel. Optionaler Personal Access Token (Rate-Limit 60/h ohne, 5000/h mit Token) → Keystore-verschlüsselt speichern
- Import: Obtainium-Export (JSON) und „Repo-URL einfügen" mit Auto-Erkennung
- Update-Vergleich primär über `versionCode`, Fallback Versionsstring; **Signatur muss mit installierter App übereinstimmen**, sonst Warnung und kein Auto-Update
- Pro App: „Updates ignorieren", „Source pinnen" (welche Quelle ist maßgeblich, wenn mehrere die App anbieten)

### 4.3 Installer (`feature:installer`)

- Download-Queue (Foreground Service, resumable, Hash-Check gegen Index-SHA256 wenn vorhanden)
- Installation über `PackageInstaller` Sessions (Single APK + Split-Bundles `.apks`/`.xapk`/`.apkm` entpacken, passende Splits für Gerät wählen)
- Ohne Privilegien: System-Bestätigungsdialog. Ab Android 12: `USER_ACTION_NOT_REQUIRED` für Updates nutzen, wenn Kiln Installer-of-Record ist. Ab Android 14: `setRequestUpdateOwnership(true)` setzen
- Mit Shizuku/Root: stille Installation/Updates, Deinstallation ohne Dialog
- Fehler sauber mappen (`STATUS_FAILURE_*` → verständliche Texte, z. B. „Signatur passt nicht zur installierten Version")

### 4.4 APK-Extraktion

- Quelle: lesbare Pfade aus `ApplicationInfo.sourceDir` + `splitSourceDirs` (kein Root/Shizuku)
- Formate (Details und XAPK-Felder: [`prompts/M1-installed-apps.md`](./prompts/M1-installed-apps.md)):
  - `.apk` — Einzel-APK; bei Splits nur Base + Warnung
  - `.apks` — ZIP aller APKs, Originalnamen, Kompression **STORED** via `java.util.zip`; **kein** bundletool-`toc.pb`, kein zip4j
  - `.xapk` — gleiches ZIP plus `manifest.json` (`xapk_version` 2, …); kein OBB in M1
- Ziel: SAF (`ACTION_CREATE_DOCUMENT` / `ACTION_OPEN_DOCUMENT_TREE`); Teilen per `FileProvider` + `ACTION_SEND`
- Batch-Export: WorkManager Long-Running Worker + Foreground (`dataSync`), Fortschritt/Abbrechen/Zusammenfassung
- Optional später: APK-Inspector für Dateien außerhalb installierter Apps

### 4.5 Storage (`feature:storage`)

- **Analyzer**: Übersicht wie im Systemdialog, aber detaillierter — pro App App/Daten/Cache über `StorageStatsManager` (braucht `PACKAGE_USAGE_STATS`-Zugriff, Onboarding-Screen mit Deep-Link in die Settings)
- **Treemap/Sunburst** der größten Verbraucher (Apps, Android/data, Downloads, Archive, APK-Reste)
- **Cleaner**:
  - Cache pro App/alle leeren: ohne Privilegien nur Deep-Link in die System-App-Info; mit Shizuku `pm trim-caches` bzw. `pm clear --cache-only <pkg>` (**Verfügbarkeit je Android-Version/ROM testen**, HyperOS/MIUI weicht teils ab)
  - Reste verwaister Apps in `Android/data` / `Android/obb` (nur mit Shizuku/`MANAGE_EXTERNAL_STORAGE` lesbar)
  - Alte APKs/XAPKs in Downloads, Duplikate (Größe + Hash), leere Ordner, große Dateien (Top-N)
  - **Immer Vorschau + Bestätigung + (wo möglich) Papierkorb** — niemals still löschen
- Kein „Booster"-Marketing-Schnickschnack

### 4.6 Backup (`feature:backup`) — ehrliche Stufen

| Tier                | Was geht                                                                                                                         |
| ------------------- | -------------------------------------------------------------------------------------------------------------------------------- |
| NONE                | APK(s) sichern, Einstellungen der App selbst; **keine** fremden App-Daten (Android sperrt `/data/data`)                          |
| SHIZUKU (shell)     | APK + `Android/data` + `Android/obb` (Zugriff als shell-UID); App-Daten in `/data/data` nur bei Apps, die selbst Backup erlauben |
| ROOT / Shizuku-Root | Volles Backup `/data/data` + `/data/user_de` + Berechtigungen; Restore mit korrektem Owner/SELinux-Kontext (`restorecon`)        |

- Format: `tar.zst` (oder zip) + `metadata.json` (Paket, Version, Signatur, Zeit, Tier), optional Verschlüsselung (AES-GCM, Schlüssel per Passwort/Keystore)
- Ziel: SAF-Ordner; später WebDAV/Cloudflare R2 (S3-kompatibel)
- Restore-Reihenfolge: APK installieren → `force-stop` → Daten entpacken → Owner/Kontext setzen
- Realistisch: Vollständige Datenbackups ohne Root sind **nicht** zuverlässig machbar. Die UI muss das klar kommunizieren. Referenz: Neo Backup, Swift Backup, AppManager

### 4.7 Termux-Integration (optional, `feature:termux`, Feature-Flag)

- Über `RUN_COMMAND`-Intent (`com.termux.RUN_COMMAND`, Service `com.termux.app.RunCommandService`), Permission `com.termux.permission.RUN_COMMAND`; im Termux muss `allow-external-apps = true` in `~/.termux/termux.properties` stehen
- **Paketname konfigurierbar** (eigener Fork `involvex/termux-app` hat evtl. andere `applicationId`)
- Ergebnis über `PendingIntent` zurückholen
- Einsatz: Post-Install-Hooks, eigene Skripte (z. B. `apktool`, `apksigner`, `rsync`/`rclone`-Upload der Backups), „Run in Termux"-Button
- Wichtig: Termux läuft als eigene App-UID, hat **keine** `pm`-Rechte → ersetzt Shizuku nicht. Kein Kernpfad der App

### 4.8 Auto-Match installierter Apps (`feature:onboarding`)

Ziel: Beim ersten Start (und danach auf Knopfdruck) alle installierten Apps automatisch passenden Quellen zuordnen, damit niemand jede App manuell hinzufügen muss.

**Ablauf**

1. Installierte Apps einlesen: Paketname, `versionCode`, **Signatur-Zertifikat (SHA-256)** über `PackageInfo.signingInfo` (inkl. `getSigningCertificateHistory`, damit Key-Rotation nicht zu Fehlmatches führt), Installer-of-Record. System-Apps standardmäßig ausblenden (Toggle).
2. Matching gegen die lokal gecachten Repo-Indizes (F-Droid, IzzyOnDroid, eigene Repos): Paketname + Vergleich des Signer-Fingerprints mit dem Index-Eintrag.
3. GitHub-Zuordnung (kein blindes Raten):
   - kuratierte Mapping-Datei im Repo (`github-map.json`: Paketname → `owner/repo` + Asset-Regex), erweiterbar per Pull Request
   - Hinweis aus dem F-Droid-Index (`sourceCode`-URL zeigt auf GitHub) als **Vorschlag**, nicht als automatische Zuordnung
   - Import aus Obtainium-Export
   - manuelles Hinzufügen
4. Ergebnis als Liste mit Konfidenz:

| Konfidenz   | Bedingung                                                         | Standardverhalten                                                                      |
| ----------- | ----------------------------------------------------------------- | -------------------------------------------------------------------------------------- |
| **HOCH**    | Paketname und Signer-Fingerprint stimmen überein                  | vorausgewählt                                                                          |
| **MITTEL**  | Paketname stimmt, Signer im Index unbekannt oder GitHub-Vorschlag | nicht vorausgewählt, Hinweis „wird beim ersten Download geprüft"                       |
| **WARNUNG** | Paketname stimmt, **Signer weicht ab**                            | nicht auswählbar ohne Bestätigung, Hinweis „Update würde mit Signaturfehler scheitern" |
| **KEINE**   | nichts gefunden (Play-Store-App, unbekannt)                       | Gruppe „Nicht gefunden"                                                                |

**Wichtig:** F-Droid signiert nicht reproduzierbar gebaute Apps mit eigenem Schlüssel. Eine von GitHub oder Play installierte App lässt sich dann **nicht** per F-Droid-Quelle updaten (Signatur passt nicht). Genau diese Fälle muss die WARNUNG-Stufe sichtbar machen.

**Regeln**

- Hat eine App mehrere passende Quellen, entscheidet eine einstellbare Priorität (Standard: IzzyOnDroid > F-Droid > GitHub), aber **nur unter Quellen mit passender Signatur**. Pro App überschreibbar („Source pinnen").
- Persistenz: `ManagedApp` in Room (`packageName`, `sourceId`, `confidence`, `pinned`, `ignoreUpdates`).
- Neu-Matching läuft automatisch, wenn ein Repo-Index aktualisiert wurde oder eine neue Quelle dazukommt.
- **Datenschutz:** Der Abgleich passiert komplett lokal gegen den heruntergeladenen Index. Die Liste installierter Apps wird nirgendwohin hochgeladen. GitHub-Anfragen gehen nur für Apps, die der Nutzer bestätigt hat.
- UI: Onboarding-Schritt „Apps übernehmen" mit Gruppen (Sicher zuordenbar / Prüfen / Warnung / Nicht gefunden) und Bulk-Aktion „Alle sicheren übernehmen".

### 4.9 Shizuku-Setup-Assistent (`feature:onboarding`)

Ziel: Die größte Einstiegshürde für alle privilegierten Features (stille Installation, Cache-Clean, Debloat, Backup-Stufen) mit einem geführten Ablauf beseitigen. Der Assistent erkennt den Zustand und zeigt nur den jeweils nächsten Schritt.

**Zustandsmaschine** (Re-Check bei `onResume` und bei Shizuku-Binder-Events):

| Zustand                      | Erkennung                                                                                 | Aktion im Assistenten                                                                        |
| ---------------------------- | ----------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------- |
| A. Nicht installiert         | `PackageManager`: weder `moe.shizuku.privileged.api` noch `af.shizuku.plus.api` vorhanden | Installation anbieten (über Kiln-Quelle, GitHub-Release von Shizuku bzw. ShizukuPlus)        |
| B. Installiert, läuft nicht  | `Shizuku.pingBinder() == false`                                                           | Startmethode wählen (siehe unten)                                                            |
| C. Läuft, keine Berechtigung | `Shizuku.checkSelfPermission() != GRANTED`                                                | `Shizuku.requestPermission()`, bei „Nie wieder fragen" Hinweis auf Shizuku-App-Einstellungen |
| D. Bereit                    | Binder ok, Permission ok                                                                  | **Selbsttest** (siehe unten), danach Tier-Anzeige                                            |

**Startmethoden (B)**

1. _Wireless Debugging_ (Android 11+, ohne PC): Schritte mit Deep-Links: Entwickleroptionen (`Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS`) → „Kabelloses Debugging" aktivieren → Kopplung per Code → in der Shizuku-App starten. Shizuku-App direkt öffnen per `getLaunchIntentForPackage`.
2. _Root_: Button „Mit Root starten", falls `su` verfügbar.
3. _ADB am PC_: Befehl zum Kopieren anzeigen (Start-Skript von Shizuku, Pfad aus der installierten Shizuku-App ableiten statt hart codieren).
4. Hinweis, falls Shizuku/ShizukuPlus einen Autostart nach Neustart anbietet (Funktion der jeweiligen App, Kiln verspricht das nicht selbst).

**Hersteller-Hinweise** (ROM-abhängig, als Hilfetexte, nicht als Logik): z. B. HyperOS/MIUI: „USB-Debugging (Sicherheitseinstellungen)" ggf. aktivieren; Autostart und Akku-Optimierung für Shizuku erlauben, damit der Dienst nicht beendet wird.

**Selbsttest (D)**: über den `UserService` `id` ausführen und anzeigen: UID (2000 = shell, 0 = root), SELinux-Kontext, Android-API-Level, Shizuku-/ShizukuPlus-Version. Daraus die **Capability-Matrix** ableiten (✅ verfügbar / ⚠️ eingeschränkt / ❌ nicht verfügbar): stille Installation, Deinstallation, Cache-Clean, `Android/data`-Zugriff, Debloat, Backup-Stufe. Button „Diagnose kopieren" für Bug-Reports.

**Nach Neustart/Absturz**: Statusbanner „Shizuku ist nicht aktiv, privilegierte Funktionen pausiert" mit Sprung zurück in den Assistenten. Kein Absturz, keine stille Funktionseinbuße: Features fallen auf Tier NONE zurück (System-Dialoge).

### 4.10 Debloat-Modus (`feature:debloat`)

**Voraussetzung:** Privilege-Tier ≥ SHIZUKU (shell). Ohne Privilegien nur Lesemodus (Bewertungen ansehen) plus Button „Shizuku einrichten".

**Aktionen** (über `PrivilegedExecutor`, nie direkt):

| Aktion                         | Befehl                           | Umkehrbar                                                                                                         |
| ------------------------------ | -------------------------------- | ----------------------------------------------------------------------------------------------------------------- |
| Deaktivieren (**Standard**)    | `pm disable-user --user 0 <pkg>` | ja: `pm enable <pkg>`                                                                                             |
| Für Nutzer entfernen           | `pm uninstall --user 0 <pkg>`    | ja: `pm install-existing --user 0 <pkg>` (System-Image bleibt unverändert; nach Werksreset ist die App wieder da) |
| Wieder aktivieren/installieren | siehe oben                       | –                                                                                                                 |

Je nach Android-Version/ROM lehnt das System manche Pakete ab (`SecurityException`, „not allowed"). Fehler sauber abfangen und pro Paket anzeigen, nicht den ganzen Batch abbrechen.

**Paketdatenbank** (`debloat-db.json`, versioniert, im Repo, per PR erweiterbar, optional Remote-Update):

```json
{
  "pkg": "…",
  "label": "…",
  "vendor": "Xiaomi|Google|Samsung|Carrier|AOSP",
  "category": "…",
  "rating": "RECOMMENDED|ADVANCED|EXPERT|UNSAFE",
  "description": "…",
  "breaks": ["…"],
  "minSdk": 0,
  "maxSdk": 99
}
```

- Bewertungen **nicht erfinden**: Cursor/AI darf Ratings nicht aus dem Gedächtnis generieren. Startdatenbank klein halten und aus einer geprüften Quelle ableiten, z. B. Universal Android Debloater Next Generation (**Lizenz vorher prüfen**, Attribution einhalten). Unbekannte Pakete = Rating „UNBEKANNT", keine Aktion ohne Expertenmodus.
- Filter: Vendor, Kategorie, Rating, „aktuell aktiv/deaktiviert/entfernt", Suche.

**Sicherheitsnetze (Pflicht, ohne diese nicht ausliefern)**

1. **Snapshot vor der ersten Änderung**: Zustand aller Pakete (`pm list packages -u`, `-d`, `-e`) als JSON-Profil lokal speichern, exportierbar.
2. **Harte Sperrliste**, die nie anfassbar ist: Kiln selbst, Shizuku/ShizukuPlus, SystemUI, Einstellungen, Telefon/Telephony, Package Installer, Permission Controller, WebView-Provider; **dynamisch ermittelt**: aktueller Standard-Launcher, aktive Tastatur (IME), Standard-Dialer, Standard-SMS-App (`RoleManager`/`PackageManager`).
3. **UNSAFE-Rating** = nicht auswählbar; **EXPERT** = nur im Expertenmodus mit getippter Bestätigung („ICH VERSTEHE").
4. **Dry-Run + Zusammenfassung**: Vor jedem Batch Liste der Änderungen mit Anzahl, Rating-Verteilung und bekannten Abhängigkeiten (`breaks`).
5. **Ein-Klick-Wiederherstellung**: pro Aktion („Rückgängig") und global („Zustand vom Snapshot wiederherstellen").
6. **Protokoll** jeder Aktion (siehe Aktivitätsprotokoll); Hinweis, nach größeren Batches das Gerät neu zu starten und zu testen, bevor weitergemacht wird.
7. **Standard = Deaktivieren**, nicht Entfernen; Entfernen nur bewusst wählbar.

**Akzeptanzkriterien:** Deaktivieren, Wiederaktivieren und Entfernen/Wiederherstellen funktionieren für mindestens 3 harmlose Test-Apps; Sperrliste verhindert nachweislich Aktionen auf Launcher, IME und Dialer; Snapshot-Restore stellt den Ausgangszustand her.

## 5. Privilege-Layer (`:core:privilege`)

Reihenfolge der Erkennung beim Start (Re-Check bei `onResume` und Shizuku-Binder-Events):

1. `ShizukuPlusExecutor` — falls `af.shizuku.plus.api` vorhanden **und** deren API von Standard-Shizuku abweicht (vor Implementierung Repo <https://github.com/thejaustin/ShizukuPlus> prüfen: Binder-Kompatibilität, zusätzliche Features, Permission-Flow)
2. `ShizukuExecutor` — Standard-Shizuku (`Shizuku.pingBinder()`, `Shizuku.checkSelfPermission()`, `requestPermission()`)
3. `RootExecutor` — `libsu`, nur wenn Nutzer es aktiviert
4. `NoopExecutor` — Fallback; Features zeigen „Benötigt Shizuku" mit Setup-Anleitung

Umsetzung:

- Schwere Operationen in einem **Shizuku `UserService`** (eigener Prozess, AIDL-Interface), nicht per `Shizuku.newProcess` (deprecated/langsam)
- Shell-Befehle strikt über Allowlist + Argument-Escaping (keine String-Konkatenation mit Nutzerinput)
- Capability-Matrix pro Android-Version in Tests dokumentieren

## 6. Permissions-Übersicht

| Permission                                                    | Wofür                       | Hinweis                            |
| ------------------------------------------------------------- | --------------------------- | ---------------------------------- |
| `QUERY_ALL_PACKAGES`                                          | Liste installierter Apps    | Sideload ok                        |
| `REQUEST_INSTALL_PACKAGES`                                    | Installation ohne Shizuku   |                                    |
| `REQUEST_DELETE_PACKAGES`                                     | Deinstallation ohne Shizuku |                                    |
| `PACKAGE_USAGE_STATS`                                         | Storage-Stats pro App       | per Settings-Intent                |
| `MANAGE_EXTERNAL_STORAGE`                                     | Dateiscans/Cleaner          | optional, nur wenn Feature genutzt |
| `POST_NOTIFICATIONS`                                          | Update-Hinweise             |                                    |
| `FOREGROUND_SERVICE_DATA_SYNC`                                | Downloads/Backups           |                                    |
| `INTERNET`, `ACCESS_NETWORK_STATE`                            | Quellen                     |                                    |
| Shizuku-Permission (`moe.shizuku.manager.permission.API_V23`) | Privilegierte Aktionen      | zur Laufzeit                       |

Prinzip: **Permissions erst anfragen, wenn das Feature benutzt wird**, mit Erklär-Screen.

## 7. Sicherheit (nicht optional)

- Nur HTTPS; F-Droid-Index-Signatur prüfen; Hash der APK gegen Index prüfen
- Vor jedem Update: Signatur-Zertifikat der neuen APK == installierte App (sonst blocken, nur manuell überschreibbar)
- GitHub-Token im Android Keystore, nie im Klartext/Log
- Keine Telemetrie, keine Analytics
- Destruktive Aktionen (Deinstall, Löschen, Restore) immer mit Bestätigung; Batch-Aktionen mit Zusammenfassung
- Root/Shell-Aktionen werden im internen Log festgehalten (lokal, einsehbar)

## 8. Roadmap (Meilensteine mit Akzeptanzkriterien)

**Lebender Status, Capability-Matrix und Reihenfolge:** [`ROADMAP.md`](./ROADMAP.md). Agent-Prompts: [`prompts/`](./prompts/).

Kurzfassung (Details und Akzeptanzkriterien in ROADMAP + Prompts / Feature-Kapiteln):

| ID | Inhalt |
| --- | --- |
| **M0** | Multi-Module, Catalog, Convention Plugins, AGP-9-Built-in-Kotlin, CI, Design System, Navigation-3-Shell, `LICENSE`, Android-`.gitignore` |
| **M1** | Installierte Apps + APK-Export (Tier NONE); Summary/Detail; `.apk` / `.apks` STORED / `.xapk` |
| **M2** | Sources, Update-Check, Auto-Match (§4.2 / §4.8) |
| **M3** | Installer ohne Privilegien (§4.3) |
| **M4** | Privilege-Layer + Shizuku-Setup (§4.9 / §5) |
| **M5** | Storage Analyzer / Cleaner (§4.5) |
| **M6** | Debloat (§4.10) |
| **M7** | Backup/Restore (§4.6) |
| **M8** | Auto-Update im Hintergrund |
| **M9** | Termux-Integration (§4.7) |
| **M10** | Polish & Release |

> Kernpfad für eine nutzbare erste Version: **M0–M5**. M6 kann parallel zu M5 laufen, sobald M4 steht.

## 9. Referenzprojekte (lesen, Ideen holen, Lizenzen beachten)

Lokale Checkouts und SPDX-IDs: [`reference.md`](./reference.md) (früher Repo-Root).

**Lizenzgrenze (Apache-2.0):** Referenz-Repos nur für Verhalten, UX-Ideen und API-Hinweise lesen. **Kein** Code, keine Kommentare, keine String-Tabellen, keine Dateistrukturen aus GPL-Projekten (App Manager, Termux) in Kiln übernehmen. Shizuku-/ShizukuPlus-Samples (Apache-2.0) dürfen ab M4 mit Attribution genutzt werden, wenn die konkrete Dateilizenz passt.

- **Obtainium** — GitHub/Multi-Source-Updates (Konfigurationsmodell)
- **Droid-ify / Neo Store** — F-Droid-Client-Architektur
- **App Manager (MuntashirAkon)** — Scope-Referenz (APK-Tools, Backup, Shizuku/Root); **GPL-3.0-or-later → nur lesen**
- **SD Maid SE** — Storage-Cleaner-Konzepte
- **Neo Backup** — Backup-Formate und Restore-Logik
- **Shizuku-Samples** (`RikkaApps/Shizuku-API`) — UserService-Beispiel (Apache-2.0; ab M4)

> Ehrlicher Hinweis: Obtainium + App Manager + SD Maid decken viel davon ab. Der Mehrwert von Kiln ist **die vereinheitlichte UX und ein gemeinsames Capability-Modell**, nicht die Einzelfunktionen. Bei Zeitdruck lieber M1–M5 sehr gut machen als alles halb.

## 10. Offene Punkte / Risiken

- ShizukuPlus-API: tatsächliche Unterschiede zu Standard-Shizuku verifizieren, bevor Code geschrieben wird
- Cache-Clear-Befehle auf HyperOS/MIUI testen (Herstellerabweichungen)
- Android-14+-Einschränkungen für Hintergrund-Installationen ohne Privilegien
- Backup ohne Root: Erwartungsmanagement in der UI
- Zugriff auf `Android/data` ab Android 11 nur mit Shizuku/Root
- Auto-Match: F-Droid signiert nicht reproduzierbare Builds mit eigenem Schlüssel → Fehlmatches/Signaturfehler müssen als WARNUNG sichtbar sein
- Debloat: falsche Entfernung kann Funktionen oder den Bootvorgang beeinträchtigen → Sperrliste, Snapshot und Standard „Deaktivieren" sind Pflicht; Lizenz der Paketdatenbank-Quelle klären
- Shizuku-Assistent: Verhalten von Wireless Debugging und Autostart unterscheidet sich je Android-Version und ROM (HyperOS gesondert testen)
- Namenskonflikt „Kiln": bekanntes anderes Produkt [NKS1608/kiln-android-emulator](https://github.com/NKS1608/kiln-android-emulator) (macOS-Android-Emulator, proprietär) — siehe [#46](https://github.com/involvex/kiln/issues/46). Unser Produkt bleibt der Android-Paket-/Speicher-Manager mit Package-ID `dev.involvex.kiln`. Vor F-Droid/Play: Store-Beschreibung klar halten; Display-Rename nur bei Markenrisiko.
- KSP ↔ Kotlin-Kompatibilität bei AGP-9-Built-in-Kotlin / KSP-2.3.x vor dem ersten M0-Build verifizieren
- Navigation 3 vs. ältere Tutorials: keine Navigation-2-APIs einführen

## 11. Regeln für Cursor / AI-Agents

- Immer **ein Meilenstein pro Branch/PR**, kleine Commits
- Vor jeder neuen Abhängigkeit: kurz begründen, aktuelle stabile Version über Version Catalog
- Keine `GlobalScope`, keine blockierenden Calls auf Main, Coroutines mit injizierten Dispatchern
- Jede Funktion, die privilegierte Rechte braucht, geht **ausschließlich** über `PrivilegedExecutor`
- Jedes destruktive Feature: Unit-Test + Bestätigungsdialog
- UI-Strings nur in `strings.xml` (DE + EN)
- Bei Unsicherheit über eine Android-API: Verhalten pro API-Level im Code kommentieren und mit Gerät testen, nicht raten
- Nach jedem Meilenstein: `README`-Abschnitt + [`ROADMAP.md`](./ROADMAP.md) (Capability-Matrix / Status) aktualisieren
- Navigation: nur Navigation 3; Feature-Module ohne NavHost
- Lizenz: Apache-2.0; GPL-Referenzen (App Manager, Termux) nie kopieren

## 12. Backlog / Erweiterungen (Vorschläge, noch nicht fest eingeplant)

Priorität: **P1** = nach M6 sinnvoll, **P2** = nach M10, **P3** = nur bei Bedarf. Nichts davon darf den Kernpfad M1–M5 verzögern. _Auto-Match, Shizuku-Setup-Assistent und Debloat-Modus sind bereits in die Roadmap (M2, M4, M6) übernommen._

### Updates & Sicherheit

- **P1 — Rollback / Downgrade**: ~~Die letzten N APK-Versionen lokal cachen…~~ → **shipped M8** (≤3/pkg + Shizuku downgrade); M23 adds age/global size prune; Settings N slider still open.
- **P1 — Update-Cooldown**: ~~Pro App oder global…~~ → **shipped M8** (global days + major auto-install skip); per-app cooldown still open.
- **P2 — Anti-Features & Tracker-Info**: F-Droid chips → **shipped M19**; optional Exodus Privacy still open.
- **P2 — Berechtigungs-Audit**: Übersicht gefährlicher Berechtigungen pro App, per Shizuku AppOps ändern und Berechtigungen in Bulk entziehen (`appops`, `pm revoke`). → **M27** (Done #41)
- **P2 — Changelog & Watchlist**: → **shipped M21**
- **P3 — Mirrors/Proxy**: Alternative Mirror-URLs je Repo, HTTP-/SOCKS-Proxy, Tor-Option.

### Dev-Features (passt zu deinem Workflow)

- **P1 — GitHub Actions Artifacts / Nightly / Beta-Kanäle**: → **shipped M19**
- **P2 — Intent-/Broadcast-API für Automatisierung**: → **shipped M22**
- **P2 — Config-Export/-Import und Sync**: Quellenliste und Regeln als JSON (Kiln-Config) via SAF → **M25** (Done #38); optional Sync über Cloudflare Worker (Hono + D1/R2) später.
- **P3 — APK-Werkzeuge Phase 2**: Manifest-Viewer, Signaturvergleich zweier APKs, Splits zu einer APK zusammenführen, mit eigenem Schlüssel neu signieren (`apksig`). Bewusst nicht Kern, weil hoher Aufwand.

### Storage & Wartung

- **P1 — Geplante Wartung**: wöchentlich Cache leeren, alte APKs aufräumen, Kiln-Temps (WorkManager, Laden/WLAN, Protokoll). SAF-Nutzer-Backups bewusst später. → **M23**
- **P2 — Apps einfrieren/hibernieren**: Selten genutzte Apps pausieren statt löschen (spart Akku und Hintergrundaktivität).
- **P3 — Dual Apps / Zweiter Bereich / Work-Profile**: Installation und Updates pro User-ID (`--user`), relevant für HyperOS „Dual Apps".

### UX

- **P2 — Widgets & Quick-Settings-Tile (Glance)**: „X Updates verfügbar", Ein-Klick-Cache-Clean, App-Shortcuts. → **M26** (Done #40)
- **P2 — Aktivitätsprotokoll**: Lokales, durchsuchbares Log. → **M24** (Done #39; M23 nur Maintenance-Protokoll)
- **P3 — Tablet-/Foldable-Layouts** (Adaptive Layouts, Navigation Rail).

### Empfohlene Reihenfolge (aktuell)

1. **M23** Geplante Wartung
2. **M24** Aktivitätsprotokoll
3. **M25** Config-Export/-Import
4. **M26** Widgets / QS
5. **M27** Berechtigungs-Audit (Done #41)
6. **M28** Storage Phase 2 (Done #44)
7. **M29** APK tools phase 2 (inspect + compare; in progress)
8. **M30** Mirrors / proxy (planned)
