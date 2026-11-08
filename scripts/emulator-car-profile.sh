#!/usr/bin/env bash
#
# Turn the local emulator into a stand-in for the Li Auto center screen:
# 2880x1620 (16:9) at 360 dpi -> exactly the 1280x720 dp canvas used by the touch UI.
#
#   ./scripts/emulator-car-profile.sh          # apply
#   ./scripts/emulator-car-profile.sh reset    # restore emulator defaults
#
# Note: SmartTube further overrides its own density via MotherActivity.initDpi()
# (1920px @ 2.0 canvas * uiScale), so inside the app the virtual canvas is
# 2880/2.25 = 1280dp wide at uiScale=0.75.
#
set -euo pipefail

TARGET="${2:-}"
ADB=(adb)
[[ -n "$TARGET" ]] && ADB=(adb -s "$TARGET")

if [[ "${1:-apply}" == "reset" ]]; then
    "${ADB[@]}" shell wm size reset
    "${ADB[@]}" shell wm density reset
    "${ADB[@]}" shell settings put system accelerometer_rotation 1
    "${ADB[@]}" shell settings put system user_rotation 0
    echo "Emulator display reset."
    exit 0
fi

# 2880 px / 1280 dp = 2.25 px/dp -> 360 dpi
"${ADB[@]}" shell settings put system accelerometer_rotation 0
"${ADB[@]}" shell settings put system user_rotation 0
"${ADB[@]}" shell wm size 2880x1620
"${ADB[@]}" shell wm density 360

echo "Applied car preview: 2880x1620 @ 360dpi (1280x720 dp, landscape)."
"${ADB[@]}" shell wm size
"${ADB[@]}" shell wm density
