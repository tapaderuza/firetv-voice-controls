package io.github.tapaderuza.firetv.voice

import android.media.session.PlaybackState

/**
 * What the screen can do right now. Fire OS offers Alexa only these commands; for anything else Alexa answers
 * that the app doesn't support it, which is better than a command the screen silently ignores.
 */
public data class Controls(
    val playback: Playback = Playback.NONE,
    /** Next and previous make sense: slides, rounds, exercises, pages. */
    val canSkip: Boolean = false,
    /** Fast forward and rewind make sense. */
    val canSeek: Boolean = false,
) {
    public enum class Playback {
        /** Nothing to pause or resume. */
        NONE,
        PLAYING,
        PAUSED,
    }

    public companion object {
        /** Nothing to control: Alexa answers that the command isn't supported. */
        public val NOTHING: Controls = Controls()
    }
}

/** Translates [Controls] into the [PlaybackState] Fire OS reads. Pure, so it is unit-tested on the JVM. */
internal object PlaybackMapping {
    const val PLAY_PAUSE_STOP: Long = PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or
        PlaybackState.ACTION_PLAY_PAUSE or PlaybackState.ACTION_STOP
    const val SKIPS: Long = PlaybackState.ACTION_SKIP_TO_NEXT or PlaybackState.ACTION_SKIP_TO_PREVIOUS
    const val SEEKS: Long = PlaybackState.ACTION_FAST_FORWARD or PlaybackState.ACTION_REWIND

    fun actions(controls: Controls): Long {
        var actions = 0L
        if (controls.playback != Controls.Playback.NONE) actions = actions or PLAY_PAUSE_STOP
        if (controls.canSkip) actions = actions or SKIPS
        if (controls.canSeek) actions = actions or SEEKS
        return actions
    }

    fun state(controls: Controls): Int = when (controls.playback) {
        Controls.Playback.PLAYING -> PlaybackState.STATE_PLAYING
        Controls.Playback.PAUSED -> PlaybackState.STATE_PAUSED
        // Nothing to pause, but slides to page: the slideshow is what's playing.
        Controls.Playback.NONE -> if (controls.canSkip || controls.canSeek) PlaybackState.STATE_PLAYING else PlaybackState.STATE_NONE
    }

    fun speed(controls: Controls): Float = if (state(controls) == PlaybackState.STATE_PLAYING) 1f else 0f
}
