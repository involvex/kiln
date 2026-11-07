# Prompt M0 — Projekt-Setup

> In Cursor einfügen (am besten im Plan-/Agent-Modus). `AGENTS.md` und `PLAN.md` liegen im Repo-Root.

---

Du arbeitest am Android-Projekt **Kiln** (`dev.involvex.kiln`). Lies zuerst vollständig `AGENTS.md` und `PLAN.md` (insbesondere §2 Tech-Entscheidung, §3 Architektur, §6 Permissions, §8 Roadmap). Bearbeite **ausschließlich Meilenstein M0**.

## Schritt 1: Erkunden (noch kein Code)

- Prüfe den Repo-Zustand (leer? bestehende Dateien?) und die lokale Umgebung (JDK, Android SDK, Gradle-Wrapper).
- Recherchiere die **aktuell stabilen** Versionen für: Kotlin, AGP, Gradle, Compose BOM, Hilt, KSP, Navigation Compose, Room, DataStore, WorkManager, Coil, Timber, JUnit5, MockK, Turbine, Spotless/ktlint, detekt. Nutze offizielle Quellen, keine Versionen aus dem Gedächtnis.
- Liste Annahmen und offene Fragen auf und schlage einen Umsetzungsplan in max. 10 Schritten vor. Warte auf mein „Go", falls etwas Wesentliches unklar ist; sonst fahre fort.

## Schritt 2: Umsetzen

1. **Projektgerüst**
   - Gradle Kotlin DSL, `gradle/libs.versions.toml` (Version Catalog), `build-logic/` mit **Convention Plugins** (Android-Application, Android-Library, Compose, Hilt, Test), damit Modul-Build-Dateien klein bleiben.
   - `applicationId`/Namespace-Root: `dev.involvex.kiln`, minSdk 26, targetSdk aktuelles Stable, Java/Kotlin-Toolchain passend zu AGP.
   - Release-Build mit R8 aktiviert (Minify/Shrink), Debug mit `applicationIdSuffix ".debug"`.
2. **Module** (nur diese, weitere kommen in späteren Meilensteinen):
   `:app`, `:core:common`, `:core:model`, `:core:designsystem`, `:feature:apps`, `:feature:sources`, `:feature:storage`, `:feature:backup`, `:feature:settings`.
   Feature-Module enthalten vorerst nur einen Platzhalter-Screen mit Titel und kurzem Beschreibungstext (aus `strings.xml`), ein ViewModel mit `UiState` und eine Hilt-Verdrahtung nach dem Zielmuster aus `AGENTS.md`, damit spätere Meilensteine das Muster übernehmen.
3. **DI:** Hilt (mit KSP), `@HiltAndroidApp`-Application, injizierbare Dispatcher (`IoDispatcher`, `DefaultDispatcher`, `MainDispatcher`) in `:core:common`.
4. **Design System** (`:core:designsystem`): Material-3-Theme mit Dynamic Color (Android 12+) und Fallback-Farbschema, **AMOLED-Dark-Option** als Parameter (noch ohne Settings-UI), Typografie, Edge-to-Edge, ein paar Basis-Komponenten (z. B. `KilnScaffold`, `EmptyState`, `SectionHeader`) mit `@Preview`.
5. **Navigation:** Bottom-Navigation mit fünf Zielen **Apps / Sources / Storage / Backup / Settings**, type-safe Routes (Navigation Compose), State pro Tab erhalten (`saveState`/`restoreState`), Adaptive-Layout optional ignorieren.
6. **Lokalisierung:** `values/strings.xml` (EN) und `values-de/strings.xml` (DE) für alle sichtbaren Texte, App-Name „Kiln".
7. **Manifest:** nur `INTERNET` (und nichts darüber hinaus). Kein `QUERY_ALL_PACKAGES` in M0. App-Icon-Platzhalter (adaptive Icon, Monochrome-Layer für Themed Icons).
8. **Qualität:** Spotless (ktlint) + detekt konfiguriert, beide in `./gradlew check` eingebunden; `.editorconfig`; `.gitignore` (inkl. `local.properties`, Keystores, `build/`).
9. **Tests:** pro Konvention ein Beispiel-Unit-Test (JUnit5 + MockK + Turbine) in `:core:common` und ein ViewModel-Test für einen Platzhalter-Screen.
10. **CI:** GitHub Actions Workflow `ci.yml`: Checkout, JDK, Gradle-Caching, `./gradlew spotlessCheck detekt lint testDebugUnitTest assembleDebug`, Debug-APK als Artifact hochladen.
11. **Doku:** `README.md` (Was ist Kiln, Status, Build-Anleitung, Modulübersicht in 10 Zeilen, Link auf `PLAN.md`), Hinweis auf `AGENTS.md`.

## Randbedingungen

- Keine Features aus M1 oder später. Keine zusätzlichen Permissions. Kein Shizuku, kein Room (außer es wird für das Gerüst zwingend gebraucht, dann begründen).
- Neue Dependencies nur über den Version Catalog, jeweils mit kurzer Begründung im PR-Text.
- Kleine, sinnvolle Commits (Conventional Commits), Branch `m0-setup`.

## Akzeptanzkriterien (alle einzeln im Abschluss-Report abhaken)

- [ ] `./gradlew spotlessCheck detekt lint testDebugUnitTest assembleDebug` läuft lokal grün
- [ ] CI-Workflow läuft grün und lädt das Debug-APK hoch
- [ ] App startet auf Gerät/Emulator, zeigt Bottom-Navigation mit 5 Tabs, jeder Tab einen Platzhalter, Tab-Wechsel erhält den Zustand
- [ ] Dynamic Color funktioniert (Android 12+), Dark/Light folgt dem System, Edge-to-Edge ohne abgeschnittene Inhalte
- [ ] UI-Texte in DE und EN vorhanden, keine hartcodierten Strings
- [ ] Alle Module bauen unabhängig; Feature-Module hängen nur von `:core:*` ab
- [ ] `README.md` vorhanden und stimmt mit dem tatsächlichen Stand überein

## Abschluss-Report (am Ende ausgeben)

1. Was wurde gebaut (Stichpunkte, Modulübersicht)
2. Gewählte Versionen mit Quelle
3. Abweichungen vom Plan und warum
4. Offene Punkte / Risiken
5. Empfehlungen für M1
