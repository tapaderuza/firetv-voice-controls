#!/usr/bin/env bash
# End-to-end check on a real Fire TV: installs the sample, drives it by "voice" (the adb test hook) and by remote
# keys (`input keyevent`), and saves a screenshot after every step to build/device-check/.
#
#   adb connect <fire-tv-ip>:5555 && scripts/device-check.sh
set -euo pipefail
export MSYS_NO_PATHCONV=1

pkg=io.github.tapaderuza.firetv.voice.sample
out=build/device-check
here="$(cd "$(dirname "$0")" && pwd)"
mkdir -p "$out"

./gradlew -q :sample:installDebug
adb shell input keyevent KEYCODE_WAKEUP
adb shell am start -W -n "$pkg/.MainActivity" > /dev/null
adb logcat -c
sleep 2

step=0
shot() {
  step=$((step + 1))
  sleep 1.2
  adb exec-out screencap -p > "$out/$(printf '%02d' "$step")-$1.png"
  echo "  $(printf '%02d' "$step") $1"
}

echo "voice (MediaSession):"
"$here/alexa.sh" pause > /dev/null; shot alexa-pause
"$here/alexa.sh" pause > /dev/null; shot alexa-pause-again-stays-paused
"$here/alexa.sh" next > /dev/null; shot alexa-next
"$here/alexa.sh" play > /dev/null; shot alexa-resume

echo "remote keys:"
adb shell input keyevent KEYCODE_MEDIA_PLAY_PAUSE; shot key-play-pause
adb shell input keyevent KEYCODE_DPAD_RIGHT; shot key-right
adb shell input keyevent KEYCODE_MENU; shot key-menu

echo "commands the app received:"
adb logcat -d -s FireTvVoice:I | grep "media session" | sed 's/^/  /'
echo "screenshots in $out/"
