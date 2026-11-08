#!/usr/bin/env bash
#
# Guardrail: our fork may only touch an allowlisted set of upstream files.
# Everything else must live under smarttubetv/src/stli/ (or docs/scripts/CI).
#
#   ./scripts/check-upstream-diff.sh                 # compares upstream/master...HEAD
#   ./scripts/check-upstream-diff.sh upstream/master
#
set -euo pipefail

BASE="${1:-upstream/master}"
ALLOW='^(smarttubetv/src/stli/|smarttubetv/build\.gradle$|common/src/stli/|\.github/workflows/li-|scripts/|docs/|\.gitignore$|README-LI\.md$)'

if ! git rev-parse --verify "$BASE" >/dev/null 2>&1; then
    echo "error: base ref '$BASE' not found." >&2
    echo "       git remote add upstream https://github.com/yuliskov/SmartTube.git" >&2
    echo "       git fetch upstream --tags" >&2
    exit 2
fi

MERGE_BASE="$(git merge-base "$BASE" HEAD)"
CHANGED="$(git diff --name-only "$MERGE_BASE" HEAD)"

echo "Base:       $BASE ($(git rev-parse --short "$BASE"))"
echo "Merge base: $(git rev-parse --short "$MERGE_BASE")"
echo "Changed files: $(printf '%s\n' "$CHANGED" | grep -c . || true)"
echo

BAD="$(printf '%s\n' "$CHANGED" | grep -vE "$ALLOW" || true)"

if [[ -n "$BAD" ]]; then
    echo "FAIL: changes outside the allowlist:" >&2
    printf '%s\n' "$BAD" >&2
    echo >&2
    echo "Move the code into smarttubetv/src/stli/, or extend the allowlist in this script." >&2
    exit 1
fi

echo "OK: only allowlisted paths changed since upstream."
