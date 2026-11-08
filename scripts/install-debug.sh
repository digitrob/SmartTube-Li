#!/usr/bin/env bash
#
# Install the freshly built debug APK and launch it.
#
#   ./scripts/install-debug.sh              # universal APK
#   ./scripts/install-debug.sh arm64-v8a    # pick ABI
#   ./scripts/install-debug.sh arm64-v8a 192.168.1.50:5555
#
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ABI="${1:-universal}"
TARGET="${2:-}"

ADB=(adb)
[[ -n "$TARGET" ]] && ADB=(adb -s "$TARGET")

APK_DIR="$ROOT/smarttubetv/build/outputs/apk"
if [[ ! -d "$APK_DIR" ]]; then
    echo "No build output. Run: ./gradlew assembleStstableDebug" >&2
    exit 1
fi

APK="$(find "$APK_DIR" -type f -name "*.apk" -path "*debug*" | grep -E "_${ABI}\.apk$" | head -1 || true)"
if [[ -z "$APK" ]]; then
    APK="$(find "$APK_DIR" -type f -name "*.apk" -path "*debug*" | head -1 || true)"
fi
if [[ -z "$APK" ]]; then
    echo "No debug APK found under $APK_DIR" >&2
    exit 1
fi

echo "Installing: $APK"
"${ADB[@]}" install -r -g "$APK"

PKG="$("${ADB[@]}" shell pm list packages 2>/dev/null | tr -d '\r' | grep -E 'smarttube' | sed 's/package://' | head -1)"
if [[ -z "$PKG" ]]; then
    echo "Installed, but could not detect package name." >&2
    exit 0
fi

echo "Launching: $PKG"
"${ADB[@]}" shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1 || \
    "${ADB[@]}" shell monkey -p "$PKG" 1 >/dev/null 2>&1 || true

echo "Done. Logs: adb logcat -s SmartTube:*"
