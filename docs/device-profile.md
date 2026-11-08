# M0 — Device profile: Li Auto headunit

Goal: capture the facts that drive UI defaults, install strategy and car integration, before writing
any touch UI.

Two parts:

1. **Automatic** — `scripts/collect-device-profile.sh` dumps everything adb can tell us.
2. **Manual** — a short in-car checklist (things adb cannot observe, e.g. what happens while driving).

## 1. Connect

Enable ADB on the headunit (developer mode / engineering menu, per your working method), then:

```bash
adb devices
# wireless:
adb connect <headunit-ip>:5555

./scripts/collect-device-profile.sh
# or: ./scripts/collect-device-profile.sh <ip>:5555
```

Reports land in `docs/device-profiles/`.

## 2. Install the stock build and capture a baseline

```bash
./gradlew assembleStstableDebug        # stock SmartTube, no Li changes yet
./scripts/install-debug.sh arm64-v8a
adb exec-out screencap -p > docs/device-profiles/stock-browse.png
adb exec-out screencap -p > docs/device-profiles/stock-player.png   # after starting a video
```

## 3. Manual checklist (fill in on the car)

| # | Check | How | Result |
|---|---|---|---|
| 1 | App appears in the Li OS launcher | look for "SmartTube" | |
| 2 | Launch works without GMS prompts | tap icon | |
| 3 | Touch: tap = select, or tap = activate? | tap a card once | |
| 4 | Touch: vertical drag scrolls rows? | drag up on home | |
| 5 | Touch: horizontal drag scrolls a row? | drag left in a row | |
| 6 | Third-party keyboard appears in a text field | open search | |
| 7 | Sign-in screen readable + QR scannable | Settings → Sign in | |
| 8 | Video plays (AVC/VP9/AV1) | play 1080p | |
| 9 | Video visible while "driving" (or simulated) | engage drive/reverse, or car's demo mode | |
| 10 | Steering-wheel next/prev/play/pause | press while playing | |
| 11 | Volume / mute keys reach the app | press | |
| 12 | Audio focus: navigation prompt ducks/pauses | start nav guidance | |
| 13 | Night mode follows the car | switch headlights | |
| 14 | Second display (passenger screen) can host the app | try moving app | |
| 15 | App survives sleep / ACC cycle | lock car, return | |
| 16 | App survives a Li OS OTA | not now — re-check after next update | |
| 17 | Screen resolution/density reported correctly | `wm size`, `wm density` | |
| 18 | 4G data usage while playing 1080p | car data counter | |

## 4. What each result changes

| Finding | Consequence |
|---|---|
| `wm size` / `wm density` | confirms the 1280×720 dp canvas at `uiScale=0.75` (see plan) |
| IME present | normal `EditText` search; no bundled keyboard |
| Video blocked while driving on center screen | target passenger screen or accept parked-only video |
| Second display launch blocked | stay center-screen only (expected without root) |
| Media keys not delivered | add a media-button receiver + on-screen controls only |
| No night-mode signal | add manual day/night toggle in settings |
| Density mismatch | adjust the car UI-scale default per device profile |

## 5. Expected build facts (for reference)

- `compileSdk 34`, `buildTools 30.0.3`, `targetSdk 27`, `minSdk 17`, AGP 7.4.2 / Gradle 7.5, JDK 17.
- Submodules pinned: SmartTube `a945135` (32.72), `SharedModules` `5b6b5d1`, `MediaServiceCore` `db543d4`.
