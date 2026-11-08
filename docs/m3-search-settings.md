# M3 — Search & settings sheets

**Date:** 2026-11-08
**Build:** `32.72-li.1`

Replaces the remaining Leanback surfaces touch users hit daily.

## 1. Touch search — `TouchSearchActivity`

- Real `EditText` + system IME (works with the car's third-party keyboard), auto-focused.
- Enter / IME search action submits; clear button resets.
- Results render in the same 3-column card grid as the feed (`FeedAdapter.applySearchGroup`).
- Tapping a result opens the watch page; long-press opens the touch context menu.
- Pull-to-refresh repeats the query.
- IME insets: `adjustResize|stateVisible` in the manifest.

Verified with "cats": 3-column grid of real results.

## 2. Touch option sheet — `TouchDialogActivity`

Implements upstream `AppDialogView`, so **every** `AppDialogPresenter` menu becomes touch-native:

| `OptionCategory` type | Rendering |
|---|---|
| Radio list | rows + check mark, **explicit exclusivity** (Leanback does this in the fragment) |
| Checkbox list | toggle rows |
| Single switch | On/Off row |
| Single button | action row |
| String list | rows that open nested dialogs |
| Long text | paragraph |
| Chat / comments | placeholder |

- Nested categories push a local back stack; back pops, then closes.
- `isTransparent` / `isOverlay` / `getViewId` supported; the sheet uses a translucent scrim.
- Closing calls `AppDialogPresenter.onFinish()` exactly once.

Also used by the long-press context menu (verified: opens `TouchDialogActivity`).

## 3. Settings section

`SettingsGroup` → `SettingsAdapter` (icon + title + chevron). Items run their upstream
`SettingsItem.onClick`, which opens the touch option sheet. The 3-column grid comes from the
feed's `GridLayoutManager`; the rows remain readable and clickable.

## Verified

| Check | Result |
|---|---|
| Search field + IME + submit | ✅ |
| Results grid | ✅ |
| Settings tiles | ✅ |
| General settings sheet | ✅ |
| Radio selection (Boot to section: Home → Shorts) | ✅ exclusive |
| Long-press context menu | ✅ |
| Crash on settings/section swap | fixed (`FeedAdapter` span lookup bounds) |

Screenshots: `docs/screenshots/search-1280dp.jpg`, `settings-grid-1280dp.jpg`,
`settings-sheet-1280dp.jpg`, `context-menu-1280dp.jpg`.

## Known gaps

1. Search tag/history chips (`setTagsProvider`) are accepted but not rendered.
2. Voice search shows a message (no car voice bridge).
3. Chat/comments categories show a placeholder.
4. Expandable categories are rendered fully expanded (acceptable for car use).
5. Settings tiles use the feed's 3-column grid rather than a dedicated list layout.
6. "Accounts" on the emulator launches GMS; on the car it uses SmartTube's TV-code sign-in.

## Next

- M4b: quality / speed / captions / SponsorBlock sheets, history write-back, track-selection
  presets (removes the 720p cap).
- M5: mini player / background playback continuity.
