package io.github.tapaderuza.firetv.voice.sample

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import io.github.tapaderuza.firetv.voice.Command
import io.github.tapaderuza.firetv.voice.Controls
import io.github.tapaderuza.firetv.voice.RemoteKeys
import io.github.tapaderuza.firetv.voice.Source
import io.github.tapaderuza.firetv.voice.VoiceControls

/**
 * A class board on the gym TV: slides rotate on their own, "Alexa, pause" holds the current one, "Alexa, next"
 * and the remote page them. Everything that touches the library is in this file.
 */
class MainActivity : ComponentActivity() {
    private val board = BoardState()
    private lateinit var voice: VoiceControls

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        voice = VoiceControls(
            context = this,
            listener = ::onCommand,
            // Nothing on this screen takes focus, so the D-pad, select and fast-forward/rewind can drive it too.
            keys = RemoteKeys(dpadPages = true, selectToggles = true, fastForwardSkips = true),
            title = "Class board",
            adbTestHook = BuildConfig.DEBUG,
        )
        publish()
        setContent { ClassBoard(board) }
    }

    private fun onCommand(command: Command, source: Source): Boolean {
        // Fire OS pauses the app in front when Home is pressed. A board on the wall keeps its state for when it's back.
        if (source == Source.SYSTEM) return true
        when (command) {
            // A voice command names the state it wants: a second "Alexa, pause" keeps the board paused.
            Command.PLAY -> board.playing = true
            Command.PAUSE, Command.STOP -> board.playing = false
            Command.TOGGLE -> board.playing = !board.playing
            Command.NEXT, Command.FAST_FORWARD -> board.step(+1)
            Command.PREVIOUS, Command.REWIND -> board.step(-1)
            Command.MENU -> board.helpVisible = !board.helpVisible
        }
        board.last = command to source
        publish()
        return true
    }

    /** The board always has slides to page, and is either rotating (playing) or holding one (paused). */
    private fun publish() = voice.update(
        Controls(if (board.playing) Controls.Playback.PLAYING else Controls.Playback.PAUSED, canSkip = true),
    )

    // Activity.dispatchKeyEvent is public API; lint flags it only because androidx marks its own override as restricted.
    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean = voice.dispatchKeyEvent(event) || super.dispatchKeyEvent(event)

    override fun onStart() {
        super.onStart()
        voice.isActive = true
    }

    override fun onStop() {
        voice.isActive = false
        super.onStop()
    }

    override fun onDestroy() {
        voice.close()
        super.onDestroy()
    }
}
