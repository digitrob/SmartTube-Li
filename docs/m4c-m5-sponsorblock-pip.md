# M4c + M5a — SponsorBlock and PiP continuity

**Date:** 2026-11-08
**Build:** `32.72-li.1`

## M4c — SponsorBlock

Implemented in the touch watch page using upstream's prefs and API (no controller-stack port):

| Piece | Implementation |
|---|---|
| Segment fetch | `MediaItemService.getSponsorSegmentsObserve(videoId, enabledCategories)` |
| Categories/actions | `SponsorBlockData` (same toggles as the TV app) |
| Skip | on the 500 ms progress tick, when inside the first 2 s of a segment → `setPositionMs(endMs)` |
| Mute segments | volume 0 for the segment duration, restored after |
| Feedback | "Skipped: <category>" toast for `SKIP_WITH_TOAST` |
| Skip once | `mLastSkipEndMs` prevents loops |
| Quick toggle | **SB** button in the watch action row |
| Full settings | long-press **SB**, or Settings → SponsorBlock (both open the touch sheet) |

Verified: SponsorBlock settings sheet renders the master switch, action list and category rows
(`docs/screenshots/sponsorblock-settings-1280dp.jpg`).

Not implemented:
- Segment markers/colors on the seek bar (needs a custom SeekBar).
- `SHOW_DIALOG` confirm prompt (currently falls back to toast + skip).
- SponsorBlock is disabled for live streams (upstream behaviour).

## M5a — Picture-in-Picture continuity

- `TouchWatchActivity` now declares `supportsPictureInPicture` + `resizeableActivity`.
- `onUserLeaveHint()` enters PiP when a video is playing (16:9).
- In PiP: side panel, metadata and controls are hidden; playback continues.
- Leaving PiP restores the normal watch layout (or fullscreen, if it was).
- `onPause` does not pause while in PiP, so switching to navigation or another car app keeps
  the video/audio alive.

Verified: pressing Home while playing shows the system PiP window on the launcher
(`docs/screenshots/pip-1280dp.jpg`).

## M5b — In-app mini player

- `LiNowPlaying` — tiny process-wide bridge holding the current video, play state and a
  `Host` (the watch activity) with `onMiniPlayPause` / `onMiniClose`.
- Pressing **Back** in the watch page no longer stops playback: the watch activity stays alive
  behind the feed and the feed shows a 64 dp mini player (thumbnail, title, play/pause, close).
- Tapping the mini player brings the existing watch activity forward (`REORDER_TO_FRONT`);
  tapping a new card reuses the same player (`onNewIntent`) so there is only ever one player.
- Closing the mini player finishes the watch activity and releases the player.
- The feed was changed from `singleTask` to `singleTop`; `singleTask` cleared the stack above
  it and destroyed the player when reordering.

Verified: back → mini bar; tap → expand; pause → icon/state; new card → single watch instance.

Screenshot: `docs/screenshots/mini-player-1280dp.jpg`.

## What remains from the original M5 scope

1. **Shorts pager** — vertical full-screen swipe UI (currently Shorts appear in the feed shelf).
2. **Comments / chapters panel** on the watch page.
3. **Queue UI** for the playback queue.
4. Mini player survives process death only through PiP; a foreground media service would make
   background audio robust (not required for car use while the app is alive).

## Next

- Either the in-app mini player (shared player + mini `PlayerView` in `TouchBrowseActivity`),
  or the Shorts pager / comments panel, depending on priority.
