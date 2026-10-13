package io.github.tapaderuza.firetv.voice

import android.content.Intent
import android.content.pm.PackageManager
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.tapaderuza.firetv.voice.Controls.Playback
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * Runs on a real Fire TV (`./gradlew :voicecontrols:connectedDebugAndroidTest`). A MediaController drives the
 * session through the system's media session service, as Fire OS does when it hears "Alexa, pause".
 */
@RunWith(AndroidJUnit4::class)
class VoiceControlsDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val received = LinkedBlockingQueue<Pair<Command, Source>>()
    private lateinit var voice: VoiceControls

    @Before
    fun setUp() = instrumentation.runOnMainSync {
        voice = VoiceControls(context, { command, source -> received += command to source; true }, adbTestHook = true)
    }

    @After
    fun tearDown() = instrumentation.runOnMainSync { voice.close() }

    private fun controller() = MediaController(context, voice.sessionToken)

    private fun nextCommand(): Pair<Command, Source>? = received.poll(5, TimeUnit.SECONDS)

    private fun update(controls: Controls) = instrumentation.runOnMainSync { voice.update(controls) }

    /** The session service applies state asynchronously; wait for it instead of sleeping a fixed time. */
    private fun awaitState(predicate: (PlaybackState) -> Boolean): PlaybackState {
        val deadline = SystemClock.uptimeMillis() + 3_000
        var last: PlaybackState? = null
        while (SystemClock.uptimeMillis() < deadline) {
            last = controller().playbackState
            if (last != null && predicate(last)) return last
            SystemClock.sleep(50)
        }
        throw AssertionError("playback state never matched; last was $last")
    }

    @Test
    fun theAppRequestsFireOsVoicePermission() {
        val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        assertTrue(info.requestedPermissions.orEmpty().contains("com.amazon.permission.media.session.voicecommandcontrol"))
    }

    @Test
    fun theSessionIsActiveFromTheStart() {
        assertTrue(voice.isActive)
    }

    @Test
    fun everyTransportControlArrivesAsItsCommand() {
        update(Controls(Playback.PLAYING, canSkip = true, canSeek = true))
        val transport = controller().transportControls
        val expected = listOf(
            Command.PAUSE to { transport.pause() },
            Command.PLAY to { transport.play() },
            Command.STOP to { transport.stop() },
            Command.NEXT to { transport.skipToNext() },
            Command.PREVIOUS to { transport.skipToPrevious() },
            Command.FAST_FORWARD to { transport.fastForward() },
            Command.REWIND to { transport.rewind() },
        )
        for ((command, send) in expected) {
            send()
            assertEquals(command to Source.MEDIA_SESSION, nextCommand())
        }
    }

    @Test
    fun fireOsSeesOnlyWhatTheScreenCanDo() {
        update(Controls(Playback.PAUSED))
        val paused = awaitState { it.state == PlaybackState.STATE_PAUSED }
        assertTrue(paused.actions and PlaybackState.ACTION_PLAY != 0L)
        assertEquals(0L, paused.actions and PlaybackState.ACTION_SKIP_TO_NEXT)

        update(Controls(Playback.NONE, canSkip = true))
        val slides = awaitState { it.actions == PlaybackMapping.SKIPS }
        assertEquals(PlaybackState.STATE_PLAYING, slides.state)

        update(Controls.NOTHING)
        val nothing = awaitState { it.state == PlaybackState.STATE_NONE }
        assertEquals(0L, nothing.actions)
    }

    @Test
    fun theAdbTestHookDrivesTheSession() {
        for ((cmd, command) in listOf("pause" to Command.PAUSE, "next" to Command.NEXT, "play" to Command.PLAY)) {
            context.sendBroadcast(
                Intent(VoiceControls.testAction(context.packageName)).setPackage(context.packageName).putExtra(VoiceControls.EXTRA_COMMAND, cmd),
            )
            assertEquals(command to Source.MEDIA_SESSION, nextCommand())
        }
    }

    @Test
    fun anInactiveSessionKeepsItsToken() {
        instrumentation.runOnMainSync { voice.isActive = false }
        assertNotNull(controller().packageName)
        instrumentation.runOnMainSync { voice.isActive = true }
        assertTrue(voice.isActive)
    }
}
