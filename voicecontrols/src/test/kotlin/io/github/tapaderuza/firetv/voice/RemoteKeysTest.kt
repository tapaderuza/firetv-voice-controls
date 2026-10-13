package io.github.tapaderuza.firetv.voice

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteKeysTest {
    private val received = mutableListOf<Pair<Command, Source>>()
    private val handlesAll = CommandListener { command, source -> received += command to source; true }
    private val handlesNone = CommandListener { command, source -> received += command to source; false }

    private fun RemoteKeys.press(keyCode: Int, listener: CommandListener = handlesAll, repeats: Int = 0): List<Boolean> =
        buildList {
            add(dispatch(keyCode, KeyEvent.ACTION_DOWN, 0, listener))
            repeat(repeats) { add(dispatch(keyCode, KeyEvent.ACTION_DOWN, it + 1, listener)) }
            add(dispatch(keyCode, KeyEvent.ACTION_UP, 0, listener))
        }

    @Test
    fun mediaKeysAlwaysMap() {
        val keys = RemoteKeys()
        assertEquals(Command.TOGGLE, keys.commandFor(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
        assertEquals(Command.PLAY, keys.commandFor(KeyEvent.KEYCODE_MEDIA_PLAY))
        assertEquals(Command.PAUSE, keys.commandFor(KeyEvent.KEYCODE_MEDIA_PAUSE))
        assertEquals(Command.STOP, keys.commandFor(KeyEvent.KEYCODE_MEDIA_STOP))
        assertEquals(Command.NEXT, keys.commandFor(KeyEvent.KEYCODE_MEDIA_NEXT))
        assertEquals(Command.PREVIOUS, keys.commandFor(KeyEvent.KEYCODE_MEDIA_PREVIOUS))
        assertEquals(Command.FAST_FORWARD, keys.commandFor(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD))
        assertEquals(Command.REWIND, keys.commandFor(KeyEvent.KEYCODE_MEDIA_REWIND))
        assertEquals(Command.MENU, keys.commandFor(KeyEvent.KEYCODE_MENU))
        assertEquals(Command.MENU, keys.commandFor(KeyEvent.KEYCODE_INFO))
    }

    @Test
    fun focusKeysStayWithTheUiByDefault() {
        val keys = RemoteKeys()
        for (key in listOf(KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_HOME)) {
            assertNull("key $key", keys.commandFor(key))
            assertEquals(listOf(false, false), keys.press(key))
        }
        assertTrue(received.isEmpty())
    }

    @Test
    fun signageOptionsTurnTheDpadAndSelectIntoControls() {
        val keys = RemoteKeys(dpadPages = true, selectToggles = true, fastForwardSkips = true)
        assertEquals(Command.NEXT, keys.commandFor(KeyEvent.KEYCODE_DPAD_RIGHT))
        assertEquals(Command.PREVIOUS, keys.commandFor(KeyEvent.KEYCODE_DPAD_LEFT))
        assertEquals(Command.TOGGLE, keys.commandFor(KeyEvent.KEYCODE_DPAD_CENTER))
        assertEquals(Command.NEXT, keys.commandFor(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD))
        assertEquals(Command.PREVIOUS, keys.commandFor(KeyEvent.KEYCODE_MEDIA_REWIND))
        // Back and Up/Down are never taken: leaving the app and scrolling must keep working.
        assertNull(keys.commandFor(KeyEvent.KEYCODE_BACK))
        assertNull(keys.commandFor(KeyEvent.KEYCODE_DPAD_UP))
        assertNull(keys.commandFor(KeyEvent.KEYCODE_DPAD_DOWN))
    }

    @Test
    fun aHeldKeyFiresOnceAndItsRepeatsAndReleaseAreConsumed() {
        val keys = RemoteKeys(dpadPages = true)
        assertEquals(listOf(true, true, true, true, true), keys.press(KeyEvent.KEYCODE_DPAD_RIGHT, repeats = 3))
        assertEquals(listOf(Command.NEXT to Source.REMOTE_KEY), received)
    }

    @Test
    fun aKeyTheAppDeclinesFallsThroughToTheUi() {
        val keys = RemoteKeys(dpadPages = true)
        assertEquals(listOf(false, false, false), keys.press(KeyEvent.KEYCODE_DPAD_RIGHT, handlesNone, repeats = 1))
        assertEquals(listOf(Command.NEXT to Source.REMOTE_KEY), received)
    }

    @Test
    fun aDeclinedPressDoesNotLeaveTheKeyHeld() {
        val keys = RemoteKeys(dpadPages = true)
        keys.press(KeyEvent.KEYCODE_DPAD_RIGHT, handlesAll)
        assertEquals(listOf(false, false), keys.press(KeyEvent.KEYCODE_DPAD_RIGHT, handlesNone))
    }

    @Test
    fun aReleaseWithoutAPressIsNotConsumed() {
        assertFalse(RemoteKeys().dispatch(KeyEvent.KEYCODE_MENU, KeyEvent.ACTION_UP, 0, handlesAll))
    }
}
