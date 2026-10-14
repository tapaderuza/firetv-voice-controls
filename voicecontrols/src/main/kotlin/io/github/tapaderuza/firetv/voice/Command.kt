package io.github.tapaderuza.firetv.voice

/** A transport command, however it reached the app. */
public enum class Command {
    /** "Alexa, play" or "Alexa, resume". */
    PLAY,

    /** "Alexa, pause". */
    PAUSE,

    /**
     * The remote's Play/Pause button. A key means "the other state"; a voice command names the state it wants,
     * so an app should never turn PAUSE into a toggle (a second "Alexa, pause" must not resume).
     */
    TOGGLE,

    /** "Alexa, stop". Screens that show something on a wall usually treat it as [PAUSE]. */
    STOP,

    /** "Alexa, next", or a Next key. */
    NEXT,

    /** "Alexa, previous", or a Previous key. */
    PREVIOUS,

    /** "Alexa, fast forward", or the remote's fast-forward key. */
    FAST_FORWARD,

    /** "Alexa, rewind", or the remote's rewind key. */
    REWIND,

    /** The remote's Menu (or Info) key. */
    MENU,
}

/** Where a [Command] came from. */
public enum class Source {
    /**
     * The app's MediaSession: Alexa (the remote's microphone or a paired Echo), the Fire TV mobile app,
     * Bluetooth controls, or the adb test hook.
     */
    MEDIA_SESSION,

    /**
     * The app's MediaSession, called by the operating system itself rather than by a person. On Fire OS, pressing
     * Home sends PAUSE to the active session about 150 ms before the activity's `onPause`. A video player wants
     * that; a screen whose state is shared (a class timer on every TV in a gym) usually ignores it. Detected on
     * API 28+ (Fire OS 7 and later); older versions report [MEDIA_SESSION].
     */
    SYSTEM,

    /** A key on the remote, through the activity's `dispatchKeyEvent`. */
    REMOTE_KEY,
}

/** Receives commands on the main thread. */
public fun interface CommandListener {
    /**
     * Returns true if the app acted on [command]. A remote key whose command returns false reaches the UI as usual
     * (focus navigation, clicks); for [Source.MEDIA_SESSION] the result is ignored.
     */
    public fun onCommand(command: Command, source: Source): Boolean
}
