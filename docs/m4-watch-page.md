# M4a — Watch page (in progress)

**Date:** 2026-11-08
**Build:** `32.72-li.1`

We jumped to the watch page before M3 (search/settings) because it is the main remaining
"tablet" experience.

## Implemented

`TouchWatchActivity` — a true 2-pane watch page, no Leanback:

| Area | Behaviour |
|---|---|
| Video | Inline ExoPlayer (`ExoPlayerController` + `PlayerView`), 65 % left pane |
| Right pane | Up next list (real suggestions from `MediaItemMetadata.getSuggestions()`) |
| Metadata | Title, channel, views/date, description (3 lines) |
| Actions | Like / Dislike (`MediaItemService`), Share (`ACTION_SEND`) |
| Controls | Persistent (car UX): play/pause, seek bar, position/duration, fullscreen |
| Gestures | Double-tap left/right = ±10 s; tap toggles controls |
| Fullscreen | Hides side pane + metadata, video fills the screen |
| Media keys | Play/pause, next (first Up next item), previous (−10 s) |
| Playback end | Auto-plays the first Up next item |

Entry point: tapping a feed card calls `WatchIntent.start(...)`; back returns to the touch feed.
Long-press on a card still opens the context menu.

### Playback stack reuse

- `ExoPlayerInitializer` + `CustomOverridesRenderersFactory` + `RestoreTrackSelector`.
- `ExoPlayerController` opens SABR / DASH / HLS / URL formats with the same priority order as
  upstream's `VideoLoaderController`.
- `SimplePlayerListener` (no-op adapter for `PlayerEventListener`) handles buffering, errors,
  play end and first-frame events.

Format selection is currently capped at **720p / 30 fps / 3 Mbps** because the TV track-selection
controller is not wired yet; without the cap the player picked AV1 at 1080p, which the emulator
could not decode (frames dropped, black surface).

## Verified

| Check | Result |
|---|---|
| VOD playback inline | ✅ |
| Live stream playback (DVR) | ✅ |
| Up next populated | ✅ |
| Metadata + actions | ✅ |
| Play/pause, seek, fullscreen | ✅ |
| Back to feed | ✅ |
| Media keys | code path present |

Screenshots: `docs/screenshots/watch-2pane-1280dp.jpg`,
`watch-live-controls-1280dp.jpg`.

## Known gaps

1. **Track selection** — use `PlayerData` presets / `TrackSelectorManager` instead of the 720p cap.
2. **Quality / speed / captions / SponsorBlock** UI — the controller supports tracks; the touch
   sheets are not built yet.
3. **History / resume** — resume position is read (`pendingPosMs`, percent watched) but not written
   back on exit.
4. **Live streams** show a DVR seek bar; seek/live-edge handling is basic.
5. **Comments / chapters** tabs are not built (Up next only).
6. **Mini player / background audio** — leaving the page stops playback (M5).
7. Overlay tap-to-toggle works via the buttons; tapping the video surface itself is not wired
   (we kept controls persistent instead).
8. The back icon uses a system "previous" glyph; replace with a proper back arrow.

## Next

1. M3: touch search + settings sheet (so the app is complete enough to use daily).
2. M4b: quality/speed/captions sheets, SponsorBlock, history write-back, track-selection presets.
3. M5: mini player + background playback continuity.
