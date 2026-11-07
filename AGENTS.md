# AGENTS.md — Kiln

Kiln ist eine Android-App (reines Kotlin + Jetpack Compose), die Package-Manager (F-Droid, GitHub Releases), APK-Tools, App-Backup, Storage-Management und einen Debloat-Modus in einer App vereint. Privilegierte Funktionen laufen optional über Shizuku/ShizukuPlus oder Root.

- **Package / applicationId:** `dev.involvex.kiln`
- **Quelle der Wahrheit für Scope, Architektur und Roadmap:** [`PLAN.md`](./PLAN.md). Vor jeder größeren Aufgabe das relevante Kapitel lesen. Bei Widersprüchen gewinnt `PLAN.md`; Abweichungen vorher mit dem Nutzer klären.
- **Prompts pro Meilenstein:** `docs/prompts/`

## Arbeitsweise

1. **Erst erkunden, dann planen, dann bauen.** Repo und `PLAN.md` lesen, offene Fragen und Annahmen auflisten, kurzen Umsetzungsplan vorschlagen, erst danach Code schreiben.
2. **Ein Meilenstein pro Branch/PR** (`m1-installed-apps`, …), kleine Commits, Conventional Commits (`feat:`, `fix:`, `chore:`, `refactor:`, `test:`, `docs:`).
3. **Nicht über den Meilenstein hinaus bauen.** Backlog-Ideen aus `PLAN.md` §12 nicht „nebenbei" umsetzen.
4. **Bei Unsicherheit fragen statt raten**, vor allem bei: Android-API-Verhalten pro Version, Herstellerabweichungen (HyperOS), Lizenzen, Sicherheitsentscheidungen.
5. Nach jedem Meilenstein: `README` und Capability-Tabelle in `PLAN.md` aktualisieren, Akzeptanzkriterien einzeln abhaken.

## Stack (Kurzfassung)

- Kotlin (aktuell stabil), Gradle Kotlin DSL, **Version Catalog** (`gradle/libs.versions.toml`), Convention Plugins in `build-logic/`
- minSdk 26, targetSdk = aktuelles Stable
- Jetpack Compose, Material 3, Navigation Compose (type-safe Routes)
- DI: **Hilt** (KSP)
- Persistenz: Room, DataStore
- Netzwerk: OkHttp + kotlinx.serialization
- Hintergrund: WorkManager, Foreground Services
- Bilder: Coil
- Tests: JUnit5, MockK, Turbine
- Versionen immer die aktuellen stabilen aus offizieller Dokumentation prüfen, **nie aus dem Gedächtnis raten**

## Modulstruktur

```
:app
:core:common  :core:model  :core:database  :core:network  :core:designsystem  :core:privilege
:feature:apps  :feature:sources  :feature:installer  :feature:storage
:feature:backup  :feature:onboarding  :feature:debloat  :feature:termux  :feature:settings
```

Module werden erst angelegt, wenn der Meilenstein sie braucht (siehe `PLAN.md` §3 und §8). Feature-Module hängen nie voneinander ab, nur von `:core:*`. Navigation läuft über `:app`.

## Befehle

Werden in M0 eingerichtet; bis dahin gilt nur Standard-Gradle. Ziel-Befehle:

```bash
./gradlew assembleDebug            # Debug-Build
./gradlew testDebugUnitTest        # Unit-Tests
./gradlew lint                     # Android Lint
./gradlew spotlessCheck detekt     # Format + statische Analyse (nach M0)
./gradlew spotlessApply            # Auto-Format
```

Vor jedem Commit müssen Build, Unit-Tests, Lint und Format-Check grün sein.

## Code-Konventionen

- Architektur: UI (Compose) → ViewModel (`StateFlow`, ein `UiState` pro Screen, `Event`/Intent für Aktionen) → UseCase (nur wenn Logik wiederverwendet wird) → Repository → DataSource. Unidirektionaler Datenfluss.
- Compose: zustandslose Composables mit `state` + Lambdas, `@Preview` für jede Komponente im Design System, `collectAsStateWithLifecycle()`.
- Coroutines: Dispatcher **injizieren** (keine hartcodierten `Dispatchers.IO`), kein `GlobalScope`, keine blockierenden Calls auf Main, Strukturierte Nebenläufigkeit, Cancellation respektieren.
- Fehler: `Result`/sealed Typen statt Exceptions über Modulgrenzen; Nutzer-Fehlertexte aus `strings.xml`.
- Strings: ausschließlich in `strings.xml` (DE + EN). Keine hartcodierten UI-Texte.
- Keine `!!`, keine Plattformtypen in `:core:model`, kein Android-Framework in reiner Domänenlogik, wo vermeidbar.
- API-Level-Verzweigungen (`Build.VERSION.SDK_INT`) mit Kommentar, **warum** und ab welcher Version.
- Logging: Timber, keine sensiblen Daten (Token, Pfade mit Nutzerdaten) im Log.

## Berechtigungen & Privilegien (harte Regeln)

- **Alles, was Shizuku/Root/Shell braucht, geht ausschließlich über `PrivilegedExecutor`** (`:core:privilege`). Kein direkter Shizuku- oder `su`-Aufruf in Feature-Modulen.
- Features lesen `capabilities` des aktiven Executors, nie „ist Shizuku installiert?". Fehlt eine Capability, zeigt die UI ehrlich den Grund und den Weg dorthin (Setup-Assistent), statt still zu scheitern.
- Shell-Befehle: feste Allowlist, Argumente escapen, **nie** Nutzereingaben in Strings konkatenieren.
- Permissions erst zur Laufzeit anfragen, wenn das Feature genutzt wird, mit Erklär-Screen. Manifest nur mit Permissions, die der aktuelle Meilenstein wirklich braucht.

## Sicherheit & Datenschutz

- Nur HTTPS. Keine Telemetrie, kein Analytics, keine Tracking-Libraries.
- Updates nur installieren, wenn das Signatur-Zertifikat der neuen APK dem der installierten App entspricht (Ausnahme nur manuell, mit Warnung).
- Tokens (z. B. GitHub-PAT) nur im Android Keystore / verschlüsselt, nie im Klartext, nie im Log, nie im Repo.
- Die Liste installierter Apps verlässt das Gerät nicht.
- **Destruktive Aktionen** (Deinstallieren, Löschen, Restore, Debloat) immer mit Bestätigung; Batch-Aktionen mit Zusammenfassung; wo möglich Vorschau/Papierkorb/Rückgängig. Pro destruktivem Feature mindestens ein Unit-Test.
- Debloat: keine Paket-Ratings „aus dem Gedächtnis" erzeugen. Daten nur aus geprüften Quellen mit geklärter Lizenz (siehe `PLAN.md` §4.10).

## Dependencies

- Neue Abhängigkeit nur mit kurzer Begründung (was, warum, Alternativen, Lizenz, Wartungsstand). Lizenz muss mit einer quelloffenen Veröffentlichung vereinbar sein.
- Im Version Catalog pflegen, keine Versionsnummern in `build.gradle.kts`-Dateien.

## Tests & Qualität

- Reine Logik (Parser, Matching, Sortierung/Filter, Manifest-Builder) mit Unit-Tests abdecken.
- Android-abhängiger Code hinter Interfaces kapseln, damit er in Unit-Tests ersetzbar ist.
- Akzeptanzkriterien aus `PLAN.md` §8 sind die Definition of Done des Meilensteins; manuelle Geräte-Checks als Checkliste im PR dokumentieren.
- Testgerät des Nutzers: Xiaomi mit HyperOS (Android-Herstellerabweichungen beachten, z. B. bei Cache-Clear, Autostart, Berechtigungs-Dialogen).

## Nicht tun

- Keine Expo-/React-Native-/Flutter-Teile; die Entscheidung für reines Kotlin ist getroffen (`PLAN.md` §2).
- Keine Root-/Shizuku-Pflicht für Basisfunktionen (Tier NONE muss nützlich bleiben).
- Keine stille Datenlöschung, keine automatischen destruktiven Aktionen ohne Bestätigung.
- Keine Secrets, Keystores oder `local.properties` committen.
- Keine großen Refactorings oder Umbenennungen außerhalb des aktuellen Meilensteins.

## Definition of Done (pro PR)

- [ ] Build, Unit-Tests, Lint, Format-Check grün (lokal + CI)
- [ ] Akzeptanzkriterien des Meilensteins erfüllt und im PR einzeln benannt
- [ ] Neue UI-Texte in DE + EN
- [ ] Keine neuen Permissions ohne Begründung im PR
- [ ] `README`/`PLAN.md` (Capability-Tabelle, Status) aktualisiert
- [ ] Bekannte Einschränkungen und offene Punkte im PR-Text aufgelistet
