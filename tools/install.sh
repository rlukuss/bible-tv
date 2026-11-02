#!/usr/bin/env bash
# Builds Bible TV and installs it as the home screen of the TV at the given address.
#
#   tools/install.sh 192.168.1.50                         Android TV, Google TV
#   tools/install.sh 192.168.1.50 --disable-free-launcher  Freebox Player POP: also turns Free's home screen off
#
# The TV needs developer options and network debugging on (see AGENTS.md); it shows a prompt to allow this computer
# the first time. Free's home screen is only disabled, never uninstalled: tools/restore-free-launcher.sh brings it back.
set -euo pipefail

if [ $# -lt 1 ]; then
  echo "Usage: tools/install.sh <TV IP address> [--disable-free-launcher]" >&2
  exit 1
fi

target="$1:5555"
disable_free=false
[ "${2:-}" = "--disable-free-launcher" ] && disable_free=true
cd "$(dirname "$0")/.."

adb connect "$target"
echo "If the TV asks to allow USB debugging from this computer, choose Allow."
adb -s "$target" wait-for-device
state=$(adb -s "$target" get-state 2>&1 || true)
if [ "$state" != "device" ]; then
  echo "The TV did not accept this computer ($state). Allow it on the TV and run this again." >&2
  exit 1
fi

./gradlew assemblePlayDebug
adb -s "$target" install -r app/build/outputs/apk/play/debug/app-play-debug.apk

home=com.yigitcan.matinee/com.tcltv.sahne.MainActivity
if adb -s "$target" shell cmd package set-home-activity "$home" >/dev/null 2>&1; then
  echo "Bible TV is the default home screen."
else
  echo "This TV does not let apps change the home screen: turn on the Home button in Bible TV's settings (see AGENTS.md)."
fi

if $disable_free; then
  if adb -s "$target" shell pm list packages | grep -q "package:net.oqee.launcher"; then
    adb -s "$target" shell pm disable-user --user 0 net.oqee.launcher
    echo "Free's home screen is disabled (not uninstalled)."
  else
    echo "Free's home screen (net.oqee.launcher) is not on this TV; nothing to disable."
  fi
fi

adb -s "$target" shell am start -n "$home" >/dev/null
echo "Done. Bible TV is open on the TV."
