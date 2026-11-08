# M2 — Feed (in progress)

**Date:** 2026-11-08
**Build:** `32.72-li.1` (`com.digitrob.smarttube.li`)

## Implemented

### Feed renderer (`FeedAdapter`)

One vertical `RecyclerView` + `GridLayoutManager`:

- **3 columns** at 1280 dp (`minCardWidth 300 dp`, floor division — no rounding up to 4).
- Adaptive span count: wider screens (e.g. the 29″ 6K car) automatically get more columns.
- Item types:
  - **Header** (full span) — group title when a section has more than one group.
  - **Video card** (1 span) — 16:9 thumbnail, duration/live/new badge, resume progress,
    2-line title, channel/metadata.
  - **Shorts shelf** (full span) — horizontal row, 6 cards, 9:16 thumbnails.
- Thumbnails via Glide with rounded corners and a solid placeholder (no network = no blank cards).
- `VideoGroup` actions handled: `REPLACE`, `APPEND` (pagination), `REMOVE`, `REMOVE_AUTHOR`,
  `SYNC`; continuation buttons filtered out.
- Tap → `BrowsePresenter.onVideoItemClicked`; long-press → `onVideoItemLongClicked`.
- Scroll-to-end → `BrowsePresenter.onScrollEnd(lastVideo)` (deduplicated per item).
- Pull-to-refresh → `BrowsePresenter.refresh()`.
- Empty/error hint overlay; spinner via `SwipeRefreshLayout`.

### Navigation fix

Upstream registers `BrowseActivity` (TV) as the parent of `PlaybackActivity` and every other
view. Returning from the player therefore landed on the TV UI. `LiUiBinder` now re-registers all
child views with `TouchBrowseActivity` as parent while keeping the TV activity implementations.

Verified: tap card → `PlaybackActivity` → back → `TouchBrowseActivity`.

### Density

`LiMetrics` forces the 1280×720 dp canvas; `MotherActivity` re-applies TV density on resume, so
the touch activity re-applies its own density in `onResume` and `onConfigurationChanged`.

## Verified on the emulator (2880×1620, API 36)

| Check | Result |
|---|---|
| Feed loads real YouTube data | ✅ (Music section, anonymous) |
| 3 columns | ✅ |
| Thumbnails, badges, metadata | ✅ |
| Group headers ("Music", "New releases") | ✅ |
| Tap → player | ✅ |
| Back → touch home | ✅ |
| Long-press → context dialog | ✅ (TV dialog until M3) |
| Crash | none |

Screenshots: `docs/screenshots/feed-3col-1280dp.jpg`, `player-tv-m2.png`.

## Notes

- Upstream intentionally boots into **Music** on first run when not signed in and there is no
  history ("Empty Home on first run fix"). Once signed in / with history, it boots to Home.
- The local AVD `digitdns-test` has a broken resolver; it must be started with
  `-dns-server 1.1.1.1` for content to load:
  ```bash
  ~/Library/Android/sdk/emulator/emulator -avd digitdns-test -no-snapshot -no-audio \
    -no-boot-anim -gpu swiftshader_indirect -dns-server 1.1.1.1
  ```

## Known gaps (next)

1. **Watch page** (M4): cards currently open the TV player.
2. **Touch context menu** (M3): long-press opens the Leanback dialog.
3. **Search** (M3): search pill is a placeholder.
4. **Settings** (M3): settings section shows a placeholder.
5. **Shorts shelf** and **pagination** are implemented but not yet exercised by data.
6. **Scroll position restore** on section change (`selectSectionItem` no-op).
