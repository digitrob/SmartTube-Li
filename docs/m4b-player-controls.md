# M4b — Player controls: quality, speed, captions

**Date:** 2026-11-08
**Build:** `32.72-li.1`

## Implemented

### Quality sheet

- Reuses `AppDialogPresenter` + `TouchDialogActivity` (no new UI stack).
- Lists every format the current media exposes via `ExoPlayerController.getVideoFormats()`
  (resolution, frame rate, bitrate, codec).
- Selecting a format calls `ExoPlayerController.selectFormat()` and persists it with
  `PlayerData.setFormat()`.
- The video keeps playing behind the sheet (`mKeepPlaying` suppresses the pause in `onPause`).
- Removed the temporary 720p/3 Mbps cap; the default preset for a fresh install is now
  **1080p / 30 fps / H.264** (`FormatItem.VIDEO_FHD_AVC_30`), changeable from the sheet.

### Speed sheet

- 0.25x … 2.0x radio list; applies `ExoPlayerController.setSpeed()` and persists
  `PlayerData.setSpeed()`. Speed is re-applied on every new video.

### Captions sheet

- "Off" (`FormatItem.SUBTITLE_NONE`) + available subtitle tracks.
- Selection applies via `selectFormat` and persists.
- The list mirrors upstream: it includes auto-translate languages, so it is long.

### History write-back

- On pause/destroy the watch page calls
  `MediaItemService.updateHistoryPosition(mediaItem, positionSec)` on an IO thread, so resume
  positions now survive across sessions.

## Verified

| Check | Result |
|---|---|
| Quality sheet over playing video | ✅ |
| Select 1080p AVC → video renders | ✅ |
| Speed sheet, 1.5x selected | ✅ |
| Captions sheet opens | ✅ |
| Video continues behind sheets | ✅ |
| Crash | none |

Screenshots: `docs/screenshots/quality-sheet-1280dp.jpg`,
`speed-sheet-1280dp.jpg`, `watch-1080p-1280dp.jpg`.

## Known gaps

1. **SponsorBlock** is not wired into the custom player yet (upstream's controller stack owns it).
   This is the main remaining playback feature; candidate for M4c.
2. The saved video preset is applied on first frame, but adaptive selection can still land on a
   higher codec before the user opens Quality once. A proper fix is to port upstream's
   `PlayerTracksController` wiring.
3. Audio track selection / 5.1 / pitch are not exposed in the touch UI.
4. Caption language list includes every auto-translate language (upstream behaviour).
5. Storyboard preview on seek is not shown.

## Next

- M4c: SponsorBlock skip + storyboard seek preview + audio track sheet.
- M5: mini player / background playback continuity.
