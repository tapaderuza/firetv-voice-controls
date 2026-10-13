package io.github.tapaderuza.firetv.voice

import android.view.KeyEvent

/**
 * Maps Fire TV remote keys to [Command]s. The D-pad and the select button move focus in most UIs, so they are
 * opt-in, for screens with nothing focusable such as signage, dashboards or a class timer.
 */
public class RemoteKeys(
    /** Left and Right page ([Command.PREVIOUS] and [Command.NEXT]). */
    public val dpadPages: Boolean = false,
    /** The select (centre) button toggles playback, as Fire TV's own player does while its controls are hidden. */
    public val selectToggles: Boolean = false,
    /** The fast-forward and rewind keys page instead of seeking: the Fire TV remote has no Next/Previous keys. */
    public val fastForwardSkips: Boolean = false,
) {
    private val held = mutableSetOf<Int>()

    /** The command for [keyCode], or null if these options leave the key to the UI. */
    public fun commandFor(keyCode: Int): Command? = when (keyCode) {
        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> Command.TOGGLE
        KeyEvent.KEYCODE_MEDIA_PLAY -> Command.PLAY
        KeyEvent.KEYCODE_MEDIA_PAUSE -> Command.PAUSE
        KeyEvent.KEYCODE_MEDIA_STOP -> Command.STOP
        KeyEvent.KEYCODE_MEDIA_NEXT -> Command.NEXT
        KeyEvent.KEYCODE_MEDIA_PREVIOUS -> Command.PREVIOUS
        KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> if (fastForwardSkips) Command.NEXT else Command.FAST_FORWARD
        KeyEvent.KEYCODE_MEDIA_REWIND -> if (fastForwardSkips) Command.PREVIOUS else Command.REWIND
        KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_INFO -> Command.MENU
        KeyEvent.KEYCODE_DPAD_RIGHT -> if (dpadPages) Command.NEXT else null
        KeyEvent.KEYCODE_DPAD_LEFT -> if (dpadPages) Command.PREVIOUS else null
        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> if (selectToggles) Command.TOGGLE else null
        else -> null
    }

    /**
     * Call from `Activity.dispatchKeyEvent`. A key fires once on press; its repeats and its release are consumed
     * when the press was, so a held Right pages once and the release of Menu doesn't reach the UI.
     */
    public fun dispatch(event: KeyEvent, listener: CommandListener): Boolean =
        dispatch(event.keyCode, event.action, event.repeatCount, listener)

    internal fun dispatch(keyCode: Int, action: Int, repeatCount: Int, listener: CommandListener): Boolean {
        val command = commandFor(keyCode) ?: return false
        return when (action) {
            KeyEvent.ACTION_DOWN -> if (repeatCount > 0) {
                keyCode in held
            } else {
                listener.onCommand(command, Source.REMOTE_KEY).also { handled ->
                    if (handled) held += keyCode else held -= keyCode
                }
            }
            KeyEvent.ACTION_UP -> held.remove(keyCode)
            else -> false
        }
    }
}
