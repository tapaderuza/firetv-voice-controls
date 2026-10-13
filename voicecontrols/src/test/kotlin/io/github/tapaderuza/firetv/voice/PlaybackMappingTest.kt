package io.github.tapaderuza.firetv.voice

import android.media.session.PlaybackState
import io.github.tapaderuza.firetv.voice.Controls.Playback
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PlaybackMappingTest {
    private fun offers(controls: Controls, action: Long) = PlaybackMapping.actions(controls) and action != 0L

    @Test
    fun nothingToControlOffersNothing() {
        assertEquals(0L, PlaybackMapping.actions(Controls.NOTHING))
        assertEquals(PlaybackState.STATE_NONE, PlaybackMapping.state(Controls.NOTHING))
        assertEquals(0f, PlaybackMapping.speed(Controls.NOTHING))
    }

    @Test
    fun aRunningScreenOffersPauseAndStopButNotSkips() {
        val running = Controls(Playback.PLAYING)
        assertEquals(PlaybackState.STATE_PLAYING, PlaybackMapping.state(running))
        assertEquals(1f, PlaybackMapping.speed(running))
        for (action in listOf(PlaybackState.ACTION_PAUSE, PlaybackState.ACTION_PLAY, PlaybackState.ACTION_PLAY_PAUSE, PlaybackState.ACTION_STOP)) {
            assertEquals("action $action", true, offers(running, action))
        }
        assertEquals(false, offers(running, PlaybackState.ACTION_SKIP_TO_NEXT))
        assertEquals(false, offers(running, PlaybackState.ACTION_FAST_FORWARD))
    }

    @Test
    fun aPausedScreenStillOffersPlay() {
        val paused = Controls(Playback.PAUSED, canSkip = true)
        assertEquals(PlaybackState.STATE_PAUSED, PlaybackMapping.state(paused))
        assertEquals(0f, PlaybackMapping.speed(paused))
        assertEquals(true, offers(paused, PlaybackState.ACTION_PLAY))
        assertEquals(true, offers(paused, PlaybackState.ACTION_SKIP_TO_PREVIOUS))
    }

    @Test
    fun slidesWithoutATimerArePlayingAndOnlySkip() {
        val slides = Controls(Playback.NONE, canSkip = true)
        assertEquals(PlaybackState.STATE_PLAYING, PlaybackMapping.state(slides))
        assertEquals(PlaybackMapping.SKIPS, PlaybackMapping.actions(slides))
        assertEquals(false, offers(slides, PlaybackState.ACTION_PAUSE))
    }

    @Test
    fun seekingIsItsOwnCapability() {
        val seekable = Controls(Playback.PLAYING, canSeek = true)
        assertEquals(true, offers(seekable, PlaybackState.ACTION_FAST_FORWARD))
        assertEquals(true, offers(seekable, PlaybackState.ACTION_REWIND))
        assertEquals(false, offers(seekable, PlaybackState.ACTION_SKIP_TO_NEXT))
        assertNotEquals(PlaybackState.STATE_NONE, PlaybackMapping.state(Controls(canSeek = true)))
    }

    @Test
    fun everyCombinationIsConsistent() {
        for (playback in Playback.entries) for (skip in listOf(false, true)) for (seek in listOf(false, true)) {
            val controls = Controls(playback, skip, seek)
            val actions = PlaybackMapping.actions(controls)
            val state = PlaybackMapping.state(controls)
            // Something on offer means a live state; nothing on offer means STATE_NONE, so Alexa says "not supported".
            assertEquals(controls.toString(), actions == 0L, state == PlaybackState.STATE_NONE)
            assertEquals(controls.toString(), playback != Playback.NONE, actions and PlaybackMapping.PLAY_PAUSE_STOP == PlaybackMapping.PLAY_PAUSE_STOP)
            assertEquals(controls.toString(), skip, actions and PlaybackMapping.SKIPS == PlaybackMapping.SKIPS)
            assertEquals(controls.toString(), seek, actions and PlaybackMapping.SEEKS == PlaybackMapping.SEEKS)
            assertEquals(controls.toString(), state == PlaybackState.STATE_PLAYING, PlaybackMapping.speed(controls) == 1f)
        }
    }
}
