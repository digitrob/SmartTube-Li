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

## What remains from the original M5 scope

1. **In-app mini player** — a docked player while browsing the feed. This needs a
   process-wide player/surface owner (the current player lives inside `TouchWatchActivity`),
   so it is a larger refactor than PiP.
2. **Shorts pager** — vertical full-screen swipe UI (currently Shorts appear in the feed shelf).
3. **Comments / chapters panel** on the watch page.
4. **Queue UI** for the playback queue.

## Next

- Either the in-app mini player (shared player + mini `PlayerView` in `TouchBrowseActivity`),
  or the Shorts pager / comments panel, depending on priority.
