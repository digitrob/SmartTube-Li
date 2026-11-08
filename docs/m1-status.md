# M1 — Status

**Date:** 2026-11-08
**Fork:** `digitrob/SmartTube-Li`
**Flavor:** `stli`
**Package:** `com.digitrob.smarttube.li`
**Version:** `32.72-li.1`

## What landed

### 1. `stli` product flavor

- `applicationId com.digitrob.smarttube.li`, `versionNameSuffix "-li.1"`.
- APK naming fixed for the flavor: `SmartTube_li_<version>_<abi>.apk`.
- Library modules (`common`, `youtubeapi`, `leanbackassistant`, …) only define
  `stbeta/ststable/stfdroid`, so the flavor uses
  `missingDimensionStrategy 'default', 'ststable'`.
  **No submodule was modified.**

### 2. Manifest overlay (`smarttubetv/src/stli/AndroidManifest.xml`)

Verified with `aapt2 dump badging`:

| Feature | Result |
|---|---|
| `android.hardware.touchscreen` | **required** |
| `android.software.leanback` | not required |
| `android.hardware.screen.landscape` | required (all activities `sensorLandscape`) |
| `application` class | `com.digitrob.smarttube.li.LiApplication` |
| `TouchBrowseActivity` | declared, `LiTheme` |

The upstream `LAUNCHER` intent-filter is retained, so the app appears in a normal
(non-Android-TV) launcher.

### 3. Touch/ TV switch

`LiApplication extends MainApplication` and calls `LiUiBinder.bind()` after upstream
registration. No upstream Java file was modified.

Three ways to switch:

1. Stored preference `li_prefs/touch_ui` (default **off** until M2 ships content).
2. Dev override (no permissions needed):
   ```bash
   adb shell settings put global smarttube_touch_ui 1   # touch
   adb shell settings put global smarttube_touch_ui 0   # TV fallback
   adb shell settings delete global smarttube_touch_ui
   ```
3. M3 adds a proper Settings entry.

### 4. Touch app shell — `TouchBrowseActivity`

- 112 dp labeled nav rail populated from the real `BrowsePresenter`
  (`addSection`/`removeSection`/`selectSection`), 14 sections on the emulator.
- 56 dp top bar with search pill and account entry.
- Density: `LiMetrics` forces the **1280×720 dp** canvas (2.25 px/dp) on 2880×1620,
  re-applied after `MotherActivity` restores the TV density on resume.
- Screenshot: `docs/screenshots/touch-shell-1280dp.png`.

### 5. Upstream guardrails

- `scripts/check-upstream-diff.sh` — allowlist check; currently **OK** (8 files changed,
  all allowlisted).
- `.github/workflows/li-ci.yml` — allowlist + `assembleStliDebug` + APK artifact.
- `.github/workflows/li-upstream-sync.yml` — nightly merge of `upstream/master`, build canary.
- `docs/upstream-sync.md` — what propagates automatically, weekly runbook, channels.
- `stli` update feed override: `smarttubetv/src/stli/res/values/update_urls.xml`.

## Verification

| Check | Result |
|---|---|
| `assembleStliDebug` | ✅ 18 s incremental (clean ~3 min) |
| APK | `SmartTube_li_32.72-li.1_arm64-v8a.apk` |
| Install/launch | ✅ cold start 2.2 s |
| Touch shell | ✅ 2880×1620, 14 sections, no crash |
| TV fallback (`smarttube_touch_ui=0`) | ✅ opens `BrowseActivity` |
| Touch (`smarttube_touch_ui=1`) | ✅ opens `TouchBrowseActivity` |
| IME | attaches (Google keyboard on emulator; car uses the third-party keyboard) |
| Network | emulator AVD DNS still broken; car profile pending |

Screenshots: `docs/screenshots/touch-shell-1280dp.png`, `tv-fallback-1280dp.png`.

## Known gaps (by design, M2/M3)

- Content area is a placeholder; the video grid is M2.
- Search pill and Account are placeholders (toast).
- Settings still opens the TV dialog if reached via the TV path.
- No watch page yet (M4).

## Next (M2)

1. `VideoGridAdapter` over `VideoGroup` (3 columns, `minCardWidth 300 dp`, adaptive).
2. Shorts shelf (6 per row).
3. Tap → watch page (M4 stub: open TV player for now), long-press → context menu sheet.
4. `onScrollEnd` pagination; pull-to-refresh; skeleton loading.
5. Tab/chip row for section filters.
