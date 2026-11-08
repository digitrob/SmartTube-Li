# M0 — Status

**Date:** 2026-11-08
**Repo:** https://github.com/digitrob/SmartTube-Li (fork of `yuliskov/SmartTube`)
**Base commit:** `a945135ee` — upstream 32.72
**Branch:** `master`
**Submodule pins:** `SharedModules` `5b6b5d1`, `MediaServiceCore` `db543d4`

## Toolchain (verified locally)

| Component | Version |
|---|---|
| JDK | Temurin 17.0.20.1 |
| Gradle | 7.5 (wrapper) |
| Android Gradle Plugin | 7.4.2 |
| compileSdk / targetSdk / minSdk | 34 / 27 / 17 |
| build-tools | 30.0.3 |
| NDK | 21.4.7075529 (symlinked as `21.0.6113669`, the pin in `smarttubetv/build.gradle`; no native code is compiled) |
| platforms | android-34 |

`local.properties` points at `$HOME/Library/Android/sdk` (git-ignored).

## Build

```
./gradlew assembleStstableDebug
```

- Result: **BUILD SUCCESSFUL in 3m 13s** (clean first build, 559 tasks)
- APKs in `smarttubetv/build/outputs/apk/ststable/debug/`:
  - `SmartTube_stable_32.72_universal.apk` — 38 MB
  - `SmartTube_stable_32.72_arm64-v8a.apk` — 29 MB
  - `SmartTube_stable_32.72_armeabi-v7a.apk` — 28 MB
  - `SmartTube_stable_32.72_x86.apk` — 31 MB

No changes to upstream code were needed for the baseline.

## Device validation

### Emulator (`digitdns-test`, API 36, arm64)

| Check | Result |
|---|---|
| Install (`adb install -r -g`) | ✅ |
| Launch (`SplashActivity` → `BrowseActivity`) | ✅ cold start 3.4 s |
| Render at 2880×1620 / 360 dpi = 1280×720 dp | ✅ (`docs/screenshots/stock-browse-1280dp.png`) |
| Crash | none in logcat |
| IME available (`GoogleInputMethodService` attached) | ✅ |
| Network | ❌ AVD DNS is broken (`UnknownHostException: www.youtube.com`); ICMP works, DNS does not — **test-environment issue, not an app issue** |

Baseline screenshots: `docs/screenshots/stock-browse-1280dp.png`, `stock-settings-1280dp.png`.
They confirm the planned delta: oversized TV text, D-pad focus visuals, no touch grid.

### Real headunit

Pending — run on the car:

```bash
./scripts/collect-device-profile.sh                # wireless: <ip>:5555
./scripts/install-debug.sh arm64-v8a <ip>:5555
```

Then fill in the manual checklist in `docs/device-profile.md`.

## Deliverables added in M0

- `scripts/collect-device-profile.sh` — adb dump (build, display, input, features, packages, install perms, network, rotation, audio)
- `scripts/install-debug.sh` — installs the newest debug APK and launches it
- `scripts/emulator-car-profile.sh` — 2880×1620 @ 360 dpi car preview
- `docs/device-profile.md` — automatic + manual checklist and how findings map to UI defaults
- `docs/screenshots/` — stock UI baselines
- `.gitignore` — device reports excluded (may contain serials)

## Open items before M1 starts

1. Real car profile capture (network, IME, media keys, video-while-driving, second display).
2. Decide default density: 3 columns (`minCardWidth 300 dp`) — recommended.
3. Decide watch page: 2-pane — recommended.
4. Optional: fix the emulator AVD DNS so end-to-end playback can be tested locally.
