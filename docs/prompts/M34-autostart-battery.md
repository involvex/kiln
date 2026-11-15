# M34 — HyperOS Autostart + battery controls

> Branch `m34-autostart-battery`. After M33 (#52). HyperOS Settings Autostart UI often desyncs; Shizuku `appops` works.

## In scope

- Settings → **Background controls** (next to Permission audit)
- HyperOS: Autostart toggle via AppOps `10008` + companion `10021` (allow/ignore)
- All devices (with privilege): battery Unrestricted / Optimized / Restricted via `dumpsys deviceidle whitelist` ± package + `RUN_ANY_IN_BACKGROUND`
- All mutations through `PrivilegedExecutor` + fixed ShellAllowlist; gate on `MANAGE_APP_OPS`
- DE + EN; activity log; unit tests for allowlist + mode mapping

## Out of scope

- Aurogon / Millet / FCM hacks, freeze/hibernate, dual-user, GPL App Manager code

## Done when

- [ ] HyperOS: Autostart switch read/write works under Shizuku
- [ ] Battery tristate maps correctly; honest gate without capability
- [ ] Non-HyperOS: Autostart section explains Xiaomi-only; battery still works
- [ ] Gates green; HyperOS checklist in PR
