# AGENTS.md — Kiln

Kiln ist eine Android-App (reines Kotlin + Jetpack Compose), die Package-Manager (F-Droid, GitHub Releases), APK-Tools, App-Backup, Storage-Management und einen Debloat-Modus in einer App vereint. Privilegierte Funktionen laufen optional über Shizuku/ShizukuPlus oder Root.

- **Package / applicationId:** `dev.involvex.kiln`
- **Lizenz:** Apache-2.0
- **Quelle der Wahrheit für Scope und Architektur:** [`docs/PLAN.md`](./docs/PLAN.md). Bei Widersprüchen gewinnt `PLAN.md`; Abweichungen vorher mit dem Nutzer klären.
- **Roadmap / Status:** [`docs/ROADMAP.md`](./docs/ROADMAP.md) (Capability-Matrix, Meilenstein-Status)
- **Prompts pro Meilenstein:** [`docs/prompts/`](./docs/prompts/) (`M0-setup.md`, `M1-installed-apps.md`, …)
- **Referenzen:** [`docs/reference.md`](./docs/reference.md) — Ideen lesen, GPL-Code nicht kopieren

## Arbeitsweise

1. **Erst erkunden, dann planen, dann bauen.** Repo, `docs/PLAN.md` und `docs/ROADMAP.md` lesen, offene Fragen und Annahmen auflisten, kurzen Umsetzungsplan vorschlagen, erst danach Code schreiben.
2. **Ein Meilenstein pro Branch/PR** (`m1-installed-apps`, …), kleine Commits, Conventional Commits (`feat:`, `fix:`, `chore:`, `refactor:`, `test:`, `docs:`).
3. **Nicht über den Meilenstein hinaus bauen.** Backlog-Ideen aus `PLAN.md` §12 nicht „nebenbei" umsetzen.
4. **Bei Unsicherheit fragen statt raten**, vor allem bei: Android-API-Verhalten pro Version, Herstellerabweichungen (HyperOS), Lizenzen, Sicherheitsentscheidungen.
5. Nach jedem Meilenstein: `README` und Capability-Matrix in `docs/ROADMAP.md` aktualisieren, Akzeptanzkriterien einzeln abhaken.

## Stack (Kurzfassung)

- Kotlin (aktuell stabil), Gradle Kotlin DSL, **Version Catalog** (`gradle/libs.versions.toml`), Convention Plugins in `build-logic/`
- AGP 9+: **Built-in Kotlin** — `org.jetbrains.kotlin.android` nicht anwenden; Compose- und Serialization-Plugins explizit
- minSdk 26, compileSdk/targetSdk = aktuelles Stable (Baseline: API 37)
- Jetpack Compose, Material 3 (stabiles BOM; kein Expressive solange nicht stabil), **Navigation 3** (type-safe `@Serializable`-NavKeys in `:app`)
- DI: **Hilt** (KSP, kein kapt)
- Persistenz: Room, DataStore (ab den Meilensteinen, die sie brauchen)
- Netzwerk: OkHttp + kotlinx.serialization
- Hintergrund: WorkManager, Foreground Services
- Bilder: Coil
- Tests: JUnit5, MockK, Turbine
- Versionen immer die aktuellen stabilen aus offizieller Dokumentation prüfen, **nie aus dem Gedächtnis raten** (Baseline: `docs/PLAN.md` §2.1)

## Modulstruktur

```
:app
:core:common  :core:model  :core:database  :core:network  :core:designsystem  :core:privilege
:feature:apps  :feature:sources  :feature:installer  :feature:storage
:feature:backup  :feature:onboarding  :feature:debloat  :feature:termux  :feature:settings
```

Module werden erst angelegt, wenn der Meilenstein sie braucht (siehe `docs/PLAN.md` §3 und `docs/ROADMAP.md`). Feature-Module hängen nie voneinander ab, nur von `:core:*`. Navigation (Navigation 3) läuft ausschließlich über `:app`; Features exportieren nur Screen-Composables.

## Befehle

Ziel-Befehle:

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
- Debloat: keine Paket-Ratings „aus dem Gedächtnis" erzeugen. Daten nur aus geprüften Quellen mit geklärter Lizenz (siehe `docs/PLAN.md` §4.10).

## Dependencies

- Neue Abhängigkeit nur mit kurzer Begründung (was, warum, Alternativen, Lizenz, Wartungsstand). Lizenz muss mit einer quelloffenen Veröffentlichung vereinbar sein.
- Im Version Catalog pflegen, keine Versionsnummern in `build.gradle.kts`-Dateien.

## Tests & Qualität

- Reine Logik (Parser, Matching, Sortierung/Filter, Manifest-Builder) mit Unit-Tests abdecken.
- Android-abhängiger Code hinter Interfaces kapseln, damit er in Unit-Tests ersetzbar ist.
- Akzeptanzkriterien aus `docs/ROADMAP.md` + Meilenstein-Prompt sind die Definition of Done; manuelle Geräte-Checks als Checkliste im PR dokumentieren.
- Testgerät des Nutzers: Xiaomi mit HyperOS (Android-Herstellerabweichungen beachten, z. B. bei Cache-Clear, Autostart, Berechtigungs-Dialogen).
- **Gerät/Emulator:** APK bauen und Install-/Launch-Befehle (`adb install`, `am start`) nur dokumentieren. **Nicht** selbst auf dem physischen Gerät installieren oder die App starten, außer der Nutzer fordert das ausdrücklich.

## Nicht tun

- Keine Expo-/React-Native-/Flutter-Teile; die Entscheidung für reines Kotlin ist getroffen (`docs/PLAN.md` §2).
- Keine Root-/Shizuku-Pflicht für Basisfunktionen (Tier NONE muss nützlich bleiben).
- Keine stille Datenlöschung, keine automatischen destruktiven Aktionen ohne Bestätigung.
- Keine Secrets, Keystores oder `local.properties` committen.
- Keine großen Refactorings oder Umbenennungen außerhalb des aktuellen Meilensteins.
- Kein Code, keine Strings, keine Layouts aus GPL-Referenz-Apps (App Manager, Termux) übernehmen — nur Verhalten beobachten (`docs/reference.md`).
- Kein Navigation Compose 2 (`androidx.navigation:navigation-compose`); Kiln nutzt Navigation 3.
- Kein `adb install` / App-Launch auf dem Nutzergerät ohne ausdrückliche Aufforderung.

## Definition of Done (pro PR)

- [ ] Build, Unit-Tests, Lint, Format-Check grün (lokal + CI)
- [ ] Akzeptanzkriterien des Meilensteins erfüllt und im PR einzeln benannt
- [ ] Neue UI-Texte in DE + EN
- [ ] Keine neuen Permissions ohne Begründung im PR
- [ ] `README` / `docs/ROADMAP.md` (Capability-Matrix, Status) aktualisiert
- [ ] Bekannte Einschränkungen und offene Punkte im PR-Text aufgelistet
