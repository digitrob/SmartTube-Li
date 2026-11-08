# R1–R3 — Tablet/web revision

**Date:** 2026-11-08
**Build:** `32.72-li.1` (`assembleStliDebug`)

This revision moves the touch UI from "usable grid" to something that reads like YouTube on a
tablet, without touching upstream logic outside `src/stli`.

## R1 — Shell: rail, top bar, sections

- New `NavRailController` (`ui/shell/`) renders the sidebar programmatically:
  - **240 dp expanded**, **72 dp collapsed**; collapse state persisted in `LiPrefs`
    (`rail_collapsed`).
  - Icon + label rows, selected state, hamburger toggle, brand mark.
  - Shared layout `res/layout/li_shell_rail.xml`, included by both browse and search.
- `LiSections` maps the rail to upstream `MediaGroup` section ids and is the single place that
  knows icons/order. Search uses the same rail and opens the browse shell via
  `LiSections.open()` (`FLAG_ACTIVITY_REORDER_TO_FRONT`).
- Browse top bar: rounded search field (with search icon), avatar chip.
- The rail is present on **browse and search**; playback intentionally has no rail.

## R2 — Browse content

- Video cards (`li_item_video_card.xml`) now follow the YouTube card anatomy:
  thumbnail, duration badge, progress bar, channel avatar circle, two-line title,
  channel/views line, overflow button.
- Search results use a **full-width list row** (`li_item_video_row.xml`): 320×180 dp thumbnail,
  two-line title, avatar + channel/views line, duration badge, watched-progress line and
  overflow. Implemented as `FeedAdapter.setListMode(true)` so search and browse share one
  adapter and binder.
- Settings section renders as a **vertical list** (switches, category rows) instead of a grid.
- Dialogs (`TouchDialogActivity`) are **bottom sheets**: scrim, rounded top corners, drag
  handle, close button, scrollable option list.

## R3 — Watch page chrome

- Controls follow the YouTube pattern: visible on entry, **auto-hide after 3 s**, any tap on the
  video toggles them.
- A **3 dp progress line is always visible** at the bottom of the video pane
  (`li_watch_mini_progress`), independent of the control bar.
- Control bar: back arrow, play/pause, next, position, seek bar, duration, **gear**, fullscreen.
  Gear replaces the old separate Quality/Speed/Captions/SponsorBlock buttons with one
  **player sheet** (Quality / Speed / Subtitles / SponsorBlock).
- Player title is shown in a top gradient and fades with the controls.
- Icons are vectors from `res/drawable/ic_li_*.xml` (no emoji glyphs, no Material dependency).

## Fixes found during testing

1. **Black screen after ~10 s / input ANR.** `MotherActivity.onCreate` constructs
   `ScreensaverManager`, which immediately posts a dim timer. `setBlocked(true)` only affects
   *future* `enable()` calls, so the armed timer still painted a full-screen black view.
   Touch activities now call `disable()` (cancels the armed timer) **before** `setBlocked(true)`,
   add `FLAG_KEEP_SCREEN_ON`, and `LiApplication` persists "screensaver disabled / timeout never"
   for the touch build. Re-tested: UI still visible after 22 s with no interaction.
2. **Single tap did nothing in the player.** The overlay had both a `GestureDetector`
   single-tap handler and an `OnClickListener`, so one tap toggled the controls twice. The
   overlay now consumes the touch stream and the gesture detector is the only toggle path.
3. **The 1080p AVC default never applied.** `PlayerData.getFormat()` never returns `null`; it
   falls back to the device default (4K VP9 on capable devices). The one-time default is now
   gated on `LiPrefs.player_format_initialized`, and `avcOverVp9Preferred` is enabled for the
   car build, so the first video selects 1080p AVC and later user choices are kept.

## Verified on emulator (2880×1620, density 360)

| Check | Result |
|---|---|
| Rail expanded/collapsed, persistence | ✅ |
| Rail on search page, section hand-off | ✅ |
| 3-column feed + card avatar/overflow | ✅ |
| Settings rendered as list | ✅ |
| Bottom sheet (player gear) | ✅ |
| Watch: auto-hide, mini progress, title, next/gear | ✅ |
| Search results as list rows, row opens playback | ✅ |
| First-run quality = 1080p AVC (gear sheet checkmark) | ✅ |
| No black overlay after 10–22 s idle | ✅ |
| No `FATAL EXCEPTION` / no ANR in final cold start | ✅ |

## Environment caveat

The macOS test host was running at load ~14 (browser + tooling) while the emulator decoded
1080p60 VP9 at 2880×1620 in software. Cold start to first frame took 30–60 s and the emulator
itself died once. Startup timing and the earlier input ANR should be re-checked on the real
headunit (hardware decoder) before treating them as app regressions.

## Known gaps

1. No Home filter chips yet (SmartTube's data layer does not expose YouTube's chip list).
2. Settings has no dedicated page header/back; the rail stays visible.
3. Channel/library pages still use the upstream TV activities.
4. Watch page uses the `ExoPlayerController` subset; upstream's full controller stack
   (storyboard, audio tracks, PiP button parity) is still not ported.
