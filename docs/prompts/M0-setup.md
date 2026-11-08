# Prompt M0 — Projekt-Setup

> In Cursor einfügen (am besten im Plan-/Agent-Modus). Regeln: `AGENTS.md`. Specs: `docs/PLAN.md`, Status: `docs/ROADMAP.md`.

---

Du arbeitest am Android-Projekt **Kiln** (`dev.involvex.kiln`, Lizenz **Apache-2.0**). Lies zuerst vollständig `AGENTS.md`, `docs/PLAN.md` (insbesondere §2 inkl. §2.1, §3, §6, §8, §9) und `docs/ROADMAP.md`. Bearbeite **ausschließlich Meilenstein M0**.

## Festgelegte Entscheidungen (nicht erneut fragen)

- **Navigation 3** (`androidx.navigation3`), nicht Navigation Compose 2. Bottom Bar mit einem Back-Stack pro Tab (offizielles Recipe). Adaptive List-Detail-Scenes **nicht** in M0.
- **Hilt** + KSP (kein Koin, kein kapt).
- **AGP 9 Built-in Kotlin**: `org.jetbrains.kotlin.android` **nicht** anwenden. Compose- und Serialization-Plugins explizit.
- **Material 3 stabil** aus dem Compose BOM; kein Material 3 Expressive.
- Seed-Farbe Fallback: `#C4552A` (Kiln-Tonerde; leicht änderbar).
- Keine Features aus M1+. Kein Coil, WorkManager, Room, DataStore, OkHttp, Shizuku in M0.
- Manifest: nur `INTERNET`.

## Schritt 1: Erkunden (noch kein Code)

- Prüfe den Repo-Zustand (derzeit Planungs-Docs + Node-orientiertes `.gitignore`; kein Gradle-Projekt) und die lokale Umgebung (JDK 21, Android SDK, später Gradle-Wrapper).
- Re-Check der **aktuell stabilen** Versionen gegen offizielle Quellen (Baseline in `docs/PLAN.md` §2.1): AGP, Gradle, Kotlin, Compose BOM, Navigation 3, Hilt, KSP, Spotless/ktlint, detekt, JUnit5, MockK, Turbine, Timber.
  - **KSP-Gate:** Neueste stabile KSP muss Kotlin 2.4.21 (oder die gewählte Kotlin-Version) unterstützen. Wenn nicht: neueste passende KSP wählen **oder** Kotlin senken — Versionen nur von der Release-Seite, nie raten. Bei Blocker kurz fragen.
- Liste Annahmen und offene Fragen auf und schlage einen Umsetzungsplan in max. 10 Schritten vor. Warte auf mein „Go", falls etwas Wesentliches unklar ist; sonst fahre fort.

## Schritt 2: Umsetzen

1. **Projektgerüst**
   - Gradle Kotlin DSL, `gradle/libs.versions.toml` (Version Catalog), `build-logic/` mit **Convention Plugins**: Android-Application, Android-Library, Compose, Hilt, Unit-Test. **Kein** Kotlin-Android-Plugin.
   - Explizit: `org.jetbrains.kotlin.plugin.compose`, `org.jetbrains.kotlin.plugin.serialization` (für `@Serializable`-NavKeys).
   - `applicationId`/Namespace-Root: `dev.involvex.kiln`, minSdk 26, compileSdk/targetSdk **37**, JDK-Toolchain 21.
   - Release-Build mit R8 aktiviert (Minify/Shrink), Debug mit `applicationIdSuffix ".debug"`.
   - Keine `screenOrientation`-Locks (API 37 / Large Screens).

2. **`.gitignore`**
   - Node-`.gitignore` durch eine **Android**-Ignore ersetzen: `local.properties`, `*.jks`, `*.keystore`, `.gradle/`, `**/build/`, IDE-Caches (`.idea/` sinnvoll filtern), Captures, `*.apk`/`*.aab` (außer bewusst versionierte Fixtures — in M0 keine). Secrets nie committen.

3. **Module** (nur diese; weitere kommen später):
   `:app`, `:core:common`, `:core:model`, `:core:designsystem`, `:feature:apps`, `:feature:sources`, `:feature:storage`, `:feature:backup`, `:feature:settings`.
   - Jedes Feature: Platzhalter-Screen (Titel + Kurzbeschreibung aus `strings.xml` des Moduls), ViewModel mit einem `UiState`, Hilt-Verdrahtung nach `AGENTS.md`.
   - Strings: `values/strings.xml` (EN) + `values-de/strings.xml` (DE) **pro Modul**, das UI-Texte hat.
   - Feature-Module hängen nur von `:core:*`, nie voneinander.

4. **DI:** Hilt (KSP), `@HiltAndroidApp`-Application, qualifizierte Dispatcher (`IoDispatcher`, `DefaultDispatcher`, `MainDispatcher`) in `:core:common`.

5. **Design System** (`:core:designsystem`): Material-3-Theme mit Dynamic Color (API 31+) und Fallback-Schema aus Seed `#C4552A`, **AMOLED-Dark** als Theme-Parameter (noch ohne Settings-UI), Typografie, Edge-to-Edge, Basis-Komponenten `KilnScaffold`, `EmptyState`, `SectionHeader` mit `@Preview`.

6. **Navigation (Navigation 3):**
   - Bottom Navigation mit fünf Zielen **Apps / Sources / Storage / Backup / Settings**.
   - Type-safe `@Serializable`-NavKeys **in `:app`**; Features exportieren nur Screen-Composables.
   - Ein Back-Stack pro Tab (`saveState`/`restoreState`-Äquivalent laut Navigation-3-Recipe).
   - Hilt-ViewModels über das in der offiziellen Navigation-3-Get-Started-Seite genannte ViewModel-Artefakt — **nicht** `androidx.hilt:hilt-navigation-compose` (Navigation 2).
   - Kein `androidx.navigation:navigation-compose`.

7. **Lokalisierung:** App-Name „Kiln". Alle sichtbaren Texte DE + EN.

8. **Manifest / Assets:** nur `INTERNET`. Adaptive Icon inkl. Monochrome-Layer für Themed Icons. Platzhalter-Icon ist ok.

9. **Qualität:** Spotless (ktlint) + detekt (Versionen am Umsetzungstag nachschlagen und ins Catalog schreiben), beide in `./gradlew check`; `.editorconfig`.

10. **Tests:** ein Beispiel-Unit-Test (JUnit 5 + MockK + Turbine) in `:core:common` und ein ViewModel-Test für einen Platzhalter-Screen. Wenn das Android-JUnit-5-Gradle-Plugin AGP 9 nicht sauber unterstützt: JUnit 5 nur für plain-JVM-Tests nutzen und das im Abschluss-Report begründen — keine erfundene Plugin-Version.

11. **CI:** `.github/workflows/ci.yml` — Checkout, JDK 21, Gradle-Caching, `./gradlew spotlessCheck detekt lint testDebugUnitTest assembleDebug`, Debug-APK als Artifact.

12. **Doku & Lizenz:**
    - Root-`LICENSE` (Apache-2.0 Text).
    - `README.md`: Was ist Kiln, Apache-2.0, Status M0, Build-Anleitung, Modulübersicht, Links auf `docs/PLAN.md`, `docs/ROADMAP.md` und `AGENTS.md`.
    - Capability-Matrix / Status in `docs/ROADMAP.md` für M0 aktualisieren.

## Randbedingungen

- Nichts aus M1 oder später. Keine zusätzlichen Permissions. Kein Shizuku, kein Room, kein Coil, kein WorkManager.
- Neue Dependencies nur über Version Catalog, jeweils mit Begründung im PR-Text.
- Kleine, sinnvolle Commits (Conventional Commits), Branch `m0-setup`.
- Kein Code aus GPL-Referenz-Apps (App Manager, Termux).

## Akzeptanzkriterien (alle einzeln im Abschluss-Report abhaken)

- [ ] `./gradlew spotlessCheck detekt lint testDebugUnitTest assembleDebug` läuft lokal grün
- [ ] CI-Workflow läuft grün und lädt das Debug-APK hoch
- [ ] App startet auf Gerät/Emulator, zeigt Bottom Navigation mit 5 Tabs (Navigation 3), jeder Tab einen Platzhalter, Tab-Wechsel erhält den Zustand (eigener Back-Stack)
- [ ] Dynamic Color funktioniert (Android 12+), Dark/Light folgt dem System, Edge-to-Edge ohne abgeschnittene Inhalte
- [ ] UI-Texte in DE und EN vorhanden, keine hartcodierten Strings
- [ ] Alle Module bauen; Feature-Module hängen nur von `:core:*` ab
- [ ] `org.jetbrains.kotlin.android` ist nirgends applied; Built-in Kotlin aktiv
- [ ] Root-`LICENSE` (Apache-2.0) und Android-`.gitignore` vorhanden
- [ ] `README.md` stimmt mit dem tatsächlichen Stand überein; `docs/ROADMAP.md` aktualisiert

## Abschluss-Report (am Ende ausgeben)

1. Was wurde gebaut (Stichpunkte, Modulübersicht)
2. Gewählte Versionen mit Quelle (inkl. KSP↔Kotlin-Entscheidung)
3. Abweichungen vom Plan und warum
4. Offene Punkte / Risiken
5. Empfehlungen für M1
