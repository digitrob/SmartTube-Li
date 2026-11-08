# M5c — Shorts pager

**Date:** 2026-11-08
**Build:** `32.72-li.1`

## Implemented

- `TouchShortsActivity` — full-screen vertical pager (`RecyclerView` + `PagerSnapHelper`),
  no new dependencies.
- A **single** `ExoPlayerController`/`SimpleExoPlayer` sits behind the pager; swiping reloads it
  with the selected Short (no per-page players, no surface churn).
- Entry: tapping a card in a Shorts shelf (Shorts section, home shelf, search shelves) opens the
  pager at that index (`ShortsIntent`).
- Page overlay: 2-line title, channel, Like / Dislike / Share, red progress bar.
- Tap = play/pause with a centre indicator.
- Swipe up/down (or DPAD up/down / media next-prev) = previous/next Short.
- Playback end automatically advances to the next page.
- Screensaver is blocked in the pager (and in the watch page), so the headunit screen no longer
  dims during playback.
- History/likes use the same `MediaItemService` calls as the watch page.

## Verified

| Check | Result |
|---|---|
| Open from Shorts shelf | ✅ |
| Portrait video renders on the 16:9 screen | ✅ |
| Title/channel overlay | ✅ (fixed 0 dp width bug) |
| Swipe to next Short | ✅ metadata + video reload + autoplay |
| Tap to pause | ✅ |
| No screen dimming during playback | ✅ |
| Crash | none |

Screenshot: `docs/screenshots/shorts-pager-1280dp.jpg`.

## Known gaps

1. Shorts opened from a regular video grid (not a shelf) still go to the watch page.
2. No prefetch of the next Short (it loads after the swipe).
3. No comments / description sheet for Shorts.
4. No long-press context menu inside the pager (use the feed for that).
5. Audio focus handling is inherited from `ExoPlayerInitializer`; ducking works but no explicit
   fade on page change.
