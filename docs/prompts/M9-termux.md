# Prompt M9 — Termux-Integration (Feature-Flag)

> Voraussetzung: M8 ist gemergt. Branch `m9-termux` von `main`. Regeln: `AGENTS.md`. Specs: `docs/PLAN.md` §4.7, Status: `docs/ROADMAP.md`.

---

Du arbeitest am Android-Projekt **Kiln** (`dev.involvex.kiln`, Apache-2.0). Lies zuerst `AGENTS.md`, `docs/PLAN.md` §4.7 und `docs/ROADMAP.md`. Bearbeite **ausschließlich Meilenstein M9**.

## Festgelegte Entscheidungen

- Nur die öffentliche RUN_COMMAND-API (Wiki): Action `com.termux.RUN_COMMAND`, Service `*.app.RunCommandService`, Permission `*.permission.RUN_COMMAND`. **Kein** GPL-Code, keine `termux-shared`-Dependency.
- `packageId` konfigurierbar (Forks). Action/Extras/Service-Klassenname leiten sich vom Package ab.
- Ergebnis über `PendingIntent` → `BroadcastReceiver` + In-Process-Bus.
- Feature-Flag (DataStore), default **aus**. Kein Kernpfad; ersetzt Shizuku nicht.
- Strings DE + EN. Settings-Einstieg wie Debloat.

## Akzeptanz

- [ ] `:feature:termux` Modul, Settings → Termux Screen
- [ ] Enable-Flag + packageId speichern
- [ ] Setup-Checkliste (installiert, Permission, allow-external-apps Hinweis)
- [ ] Echo-Smoke-Test mit PendingIntent-Ergebnis
- [ ] Unit-Tests für Path/API/Intent-Spec
- [ ] `spotlessCheck detekt lint testDebugUnitTest assembleDebug` grün
- [ ] ROADMAP / README aktualisiert
