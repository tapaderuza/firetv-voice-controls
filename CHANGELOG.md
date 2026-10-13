# Changelog

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
