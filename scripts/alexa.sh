#!/usr/bin/env bash
# Sends a transport command to an app's MediaSession through the library's adb test hook (debug builds), the same
# MediaController path Fire OS uses when it hears "Alexa, pause". No microphone, no speaking, works in CI.
#
#   scripts/alexa.sh pause                       # the sample app
#   scripts/alexa.sh next com.example.myapp      # your app, built with adbTestHook = BuildConfig.DEBUG
set -euo pipefail
export MSYS_NO_PATHCONV=1 # Git Bash on Windows would rewrite the arguments otherwise

cmd="${1:-}"
pkg="${2:-io.github.tapaderuza.firetv.voice.sample}"
case "$cmd" in
  play | pause | stop | next | previous | fastforward | rewind) ;;
  *) echo "usage: $0 <play|pause|stop|next|previous|fastforward|rewind> [package]" >&2; exit 2 ;;
esac

adb shell am broadcast -a "$pkg.firetv.voice.TEST" -p "$pkg" --es cmd "$cmd" > /dev/null
echo "sent '$cmd' to $pkg"
