# Upstream sync — how "update from upstream" works

The point of this design: **upstream features arrive for free, merges stay boring.**

## What propagates automatically

| Upstream change | Where it lives | Cost to us |
|---|---|---|
| Innertube/API fixes, codecs, playback, SponsorBlock, login, casting | `common/`, `youtubeapi`, `MediaServiceCore`, ExoPlayer | **none** — merged |
| Settings & options | `AppDialogPresenter` + `OptionCategory` | **none** — our touch sheet renders all categories |
| New player buttons/flags | `PlayerTweaksData`, `R.id.action_*` | small — add to the touch overlay map |
| New TV screens | new `*View` + `*Activity` | port on our schedule |

## What we are allowed to change upstream

Everything else lives in `smarttubetv/src/stli/` (+ `common/src/stli/` for resources).
`scripts/check-upstream-diff.sh` (run in CI) fails the build if a diff touches anything outside:

```
smarttubetv/src/stli/
common/src/stli/
smarttubetv/build.gradle
.github/workflows/li-*
scripts/
docs/
.gitignore
README-LI.md
```

`smarttubetv/build.gradle` is the only upstream build file touched: it adds the `stli` flavor.
Library modules keep their `stbeta/ststable/stfdroid` flavors; the `stli` flavor maps to their
`ststable` variant with `missingDimensionStrategy`, so no submodule is patched.

## Branch model

- `master` — our integration branch (upstream + Li commits).
- Upstream is a remote, not a fork of the tree: `git fetch upstream --tags`.
- Weekly: merge upstream into `master`; resolve (usually nothing).

## Weekly runbook

```bash
git fetch upstream --tags
git checkout master
git merge upstream/master            # or a tag such as 32.72s
git submodule sync --recursive
git submodule update --init --recursive
./scripts/check-upstream-diff.sh
./gradlew assembleStliDebug
./scripts/install-debug.sh arm64-v8a <headunit-ip>:5555
```

If the merge conflicts, it can only be in `smarttubetv/build.gradle` (the flavor block or the APK
naming line). Resolve once; the allowlist check tells you immediately if anything else slipped in.

Submodule rule: **never** `git submodule update --remote`. Take the pins that upstream committed and
tested together.

## Channels

| Channel | Base | Cadence | Purpose |
|---|---|---|---|
| `stli` (debug/beta) | `upstream/master` | nightly CI canary (`.github/workflows/li-upstream-sync.yml`) | catch breakage early, get API fixes fast |
| `stli` (stable) | latest upstream tag | weekly, after a car smoke test | the build that lives on the headunit |

Versioning: keep upstream `versionName` and append `-li.N` (already set as `versionNameSuffix`).
Bump `N` for our own releases so the updater sees a new version.

## In-app updates

`AppUpdatePresenter` reads `R.array.update_urls`. The `stli` flavor overrides it with our own feed:

```
https://github.com/digitrob/SmartTube-Li/releases/download/latest/li.json
```

The feed format (see `AppVersionChecker`) is:

```json
{
  "package": { "downloadUrl": "https://.../SmartTube_li_32.72-li.2_arm64-v8a.apk" },
  "32.72-li.2": { "versionCode": 3272002, "changelog": ["upstream 32.72", "touch: ..."] }
}
```

Without root, Android still shows the install prompt; SmartTube's updater already handles that.

## Graceful degradation (required for auto-updates)

When upstream adds something we don't know yet, the touch UI must not crash:

- unknown `BrowseSection` type → default grid renderer
- unknown `OptionCategory` type → generic list
- unknown player button id → "More" sheet

These rules are part of the M2/M3 acceptance criteria.
