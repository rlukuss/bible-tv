#!/usr/bin/env bash
# Freebox Player POP: turns Free's own home screen back on and makes it the default again.
#
#   tools/restore-free-launcher.sh 192.168.1.50
set -euo pipefail

if [ $# -lt 1 ]; then
  echo "Usage: tools/restore-free-launcher.sh <TV IP address>" >&2
  exit 1
fi

target="$1:5555"
adb connect "$target"
adb -s "$target" shell pm enable net.oqee.launcher
adb -s "$target" shell cmd package set-home-activity net.oqee.launcher/net.oqee.tvlauncher.ui.home.HomeActivity
echo "Free's home screen is back."
