# Changelog

## 0.2.0 (2026-10-14)

- `Source.SYSTEM`: commands the operating system sends on its own, told apart by the caller's uid (API 28+). On Fire OS,
  pressing Home sends PAUSE to the session of the app in front about 150 ms before `onPause`; a screen with shared
  state (a class timer on every TV in a gym) can now ignore it.
- Every delivered command is logged with the caller's package and uid (`adb logcat -s FireTvVoice`).
- New on-device test that presses Home and expects that PAUSE from the system; 15 unit and 8 on-device tests.
- The sample keeps rotating when Fire OS pauses it on Home.

## 0.1.0 (2026-10-13)

First release, extracted from ScreenDesk.

- `VoiceControls`: a MediaSession that delivers Alexa transport commands (play, pause, stop, next, previous, fast
  forward, rewind) as `Command`s, with the Fire OS voice permission merged into the app's manifest.
- `Controls`: what the screen can do, turned into the PlaybackState Fire OS offers to Alexa.
- `RemoteKeys`: the remote's transport keys as `Command`s, with opt-in D-pad paging, select-to-toggle, and
  fast-forward/rewind paging.
- An adb test hook (`adbTestHook = BuildConfig.DEBUG`) and `scripts/alexa.sh` to drive the session without
  speaking; `scripts/device-check.sh` for an end-to-end run with screenshots.
- 13 JVM unit tests and 6 on-device tests, verified on a Fire TV Stick (3rd gen) with Fire OS 7.7.1.7.
