#!/usr/bin/env bash
#
# M0: collect the information we need from the Li Auto headunit.
#
#   ./scripts/collect-device-profile.sh                 # uses the only connected adb device
#   ./scripts/collect-device-profile.sh 192.168.1.50:5555
#
# The report is written to docs/device-profiles/<serial>-<timestamp>.txt and printed.
#
set -uo pipefail

ADB="adb"
if [[ $# -gt 0 ]]; then
    ADB="adb -s $1"
fi

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT_DIR="$ROOT/docs/device-profiles"
mkdir -p "$OUT_DIR"

SERIAL="$($ADB shell getprop ro.serialno 2>/dev/null | tr -d '\r')"
STAMP="$(date +%Y%m%d-%H%M%S)"
OUT="$OUT_DIR/${SERIAL:-unknown}-$STAMP.txt"

section() { printf '\n\n===== %s =====\n' "$1"; }
run()     { printf '\n$ %s\n' "$*"; "$@" 2>&1 | tr -d '\r'; }
sh_run()  { printf '\n$ adb shell %s\n' "$*"; $ADB shell "$@" 2>&1 | tr -d '\r'; }

{
    printf 'SmartTube Li Edition - device profile\n'
    printf 'collected: %s\n' "$(date)"
    printf 'adb target: %s\n' "${1:-default}"

    section "Build / product"
    sh_run getprop ro.build.version.release
    sh_run getprop ro.build.version.sdk
    sh_run getprop ro.build.version.security_patch
    sh_run getprop ro.build.type
    sh_run getprop ro.debuggable
    sh_run getprop ro.build.characteristics
    sh_run getprop ro.product.manufacturer
    sh_run getprop ro.product.model
    sh_run getprop ro.product.device
    sh_run getprop ro.product.name
    sh_run getprop ro.hardware
    sh_run getprop ro.board.platform
    sh_run getprop ro.product.cpu.abi
    sh_run getprop ro.sf.lcd_density
    sh_run getprop persist.sys.timezone
    sh_run id

    section "Display"
    sh_run wm size
    sh_run wm density
    printf '\n$ adb shell dumpsys window displays | grep -E "Display:|init=|cur=|app="\n'
    $ADB shell dumpsys window displays 2>/dev/null | tr -d '\r' | grep -E 'Display:|init=|cur=|app=' | head -30
    printf '\n$ adb shell dumpsys display | grep -E "mDisplayId|uniqueId|DisplayDeviceInfo"\n'
    $ADB shell dumpsys display 2>/dev/null | tr -d '\r' | grep -E 'mDisplayId|uniqueId|DisplayDeviceInfo' | head -30
    sh_run settings get system screen_brightness_mode
    sh_run settings get secure ui_night_mode

    section "Input"
    sh_run dumpsys input | grep -E '^ *[0-9]+: |Touch|Keyboard|Source:' 
    printf '\n$ adb shell dumpsys input | grep -iE "touch|mouse|keyboard" | head -20\n'
    $ADB shell dumpsys input 2>/dev/null | tr -d '\r' | grep -iE 'touch|mouse|keyboard' | head -20

    section "Features"
    sh_run pm list features
    printf '\n$ adb shell pm list features | grep -Ei "touch|leanback|television|automotive|wifi|ethernet|usb.host|microphone|audio|gamepad"\n'
    $ADB shell pm list features 2>/dev/null | tr -d '\r' | grep -Ei 'touch|leanback|television|automotive|wifi|ethernet|usb.host|microphone|audio|gamepad'

    section "Installed / system packages of interest"
    printf '\n$ adb shell pm list packages | grep -Ei "youtube|gms|vending|smarttube|launcher|settings"\n'
    $ADB shell pm list packages 2>/dev/null | tr -d '\r' | grep -Ei 'youtube|gms|vending|smarttube|launcher|settings'

    section "Install permissions"
    sh_run settings get secure install_non_market_apps
    sh_run settings get global install_non_market_apps
    sh_run appops get com.android.packageinstaller REQUEST_INSTALL_PACKAGES
    sh_run pm list users

    section "Network"
    sh_run getprop dhcp.wlan0.ipaddress
    sh_run ping -c 1 -W 2 8.8.8.8
    sh_run ping -c 1 -W 2 youtube.com
    printf '\n$ adb shell dumpsys connectivity | grep -E "Active default network|NetworkAgentInfo" | head -10\n'
    $ADB shell dumpsys connectivity 2>/dev/null | tr -d '\r' | grep -E 'Active default network|NetworkAgentInfo' | head -10

    section "Foreground / rotation"
    printf '\n$ adb shell dumpsys activity activities | grep -E "ResumedActivity|mResumedActivity" | head\n'
    $ADB shell dumpsys activity activities 2>/dev/null | tr -d '\r' | grep -E 'ResumedActivity|mResumedActivity' | head
    sh_run settings get system accelerometer_rotation
    sh_run settings get system user_rotation

    section "Audio"
    printf '\n$ adb shell dumpsys audio | grep -E "mode:|mMusicActive|STREAM_MUSIC" | head -20\n'
    $ADB shell dumpsys audio 2>/dev/null | tr -d '\r' | grep -E 'mode:|mMusicActive|STREAM_MUSIC' | head -20

    section "Icons / density hints"
    sh_run getprop ro.sf.lcd_density
    sh_run wm size

    printf '\n\nDone. Also complete the manual checklist in docs/device-profile.md\n'
} | tee "$OUT"

printf '\n\nSaved: %s\n' "$OUT"
