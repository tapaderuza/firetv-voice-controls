package io.github.tapaderuza.firetv.voice

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.util.Log
import android.view.KeyEvent

/**
 * "Alexa, pause", "Alexa, next" and the remote's transport keys for a Fire TV app that isn't a video player:
 * a class timer, a slideshow, a dashboard, a game.
 *
 * Fire OS routes Alexa's transport commands (from the remote's microphone or a paired Echo) to the active
 * MediaSession of the app on screen, provided the app holds the voice permission this library's manifest adds
 * and its PlaybackState lists the action ([Amazon's guide](https://developer.amazon.com/docs/fire-tv/mediasession-api-integration.html)).
 * Nothing in that path needs audio, so a screen that never plays a sound can use it too.
 *
 * Create it on the main thread (in `onCreate`), describe what the screen can do with [update] whenever that
 * changes, forward keys from `dispatchKeyEvent`, and [close] it in `onDestroy`. Commands reach [listener] on
 * the main thread.
 */
public class VoiceControls(
    context: Context,
    private val listener: CommandListener,
    private val keys: RemoteKeys = RemoteKeys(),
    title: String = "Controls",
    /**
     * Debug builds only (pass `BuildConfig.DEBUG`): lets `adb shell am broadcast` drive the session through a
     * MediaController, the same path Fire OS uses for a spoken command. See [testAction].
     */
    adbTestHook: Boolean = false,
) : AutoCloseable {
    private val appContext = context.applicationContext

    private val session = MediaSession(appContext, title).apply {
        setCallback(object : MediaSession.Callback() {
            override fun onPlay() = deliver(Command.PLAY)
            override fun onPause() = deliver(Command.PAUSE)
            override fun onStop() = deliver(Command.STOP)
            override fun onSkipToNext() = deliver(Command.NEXT)
            override fun onSkipToPrevious() = deliver(Command.PREVIOUS)
            override fun onFastForward() = deliver(Command.FAST_FORWARD)
            override fun onRewind() = deliver(Command.REWIND)
        })
        setMetadata(MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE, title).build())
        isActive = true
    }

    private val testHook: BroadcastReceiver? = if (!adbTestHook) null else object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val transport = session.controller.transportControls
            when (intent.getStringExtra(EXTRA_COMMAND)) {
                "play" -> transport.play()
                "pause" -> transport.pause()
                "stop" -> transport.stop()
                "next" -> transport.skipToNext()
                "previous" -> transport.skipToPrevious()
                "fastforward" -> transport.fastForward()
                "rewind" -> transport.rewind()
                else -> Log.w(TAG, "test hook: unknown cmd '${intent.getStringExtra(EXTRA_COMMAND)}'")
            }
        }
    }

    init {
        testHook?.let {
            val filter = IntentFilter(testAction(appContext.packageName))
            // adb's broadcasts come from the shell user, so the receiver has to be exported.
            if (Build.VERSION.SDK_INT >= 33) appContext.registerReceiver(it, filter, Context.RECEIVER_EXPORTED)
            else appContext.registerReceiver(it, filter)
        }
        update(Controls.NOTHING)
    }

    /** The session's token, for a `MediaController` in tests or a companion surface. */
    public val sessionToken: MediaSession.Token get() = session.sessionToken

    /**
     * Whether the session takes commands. Turn it off in `onStop`, so "Alexa, pause" said while another app is on
     * screen never reaches this one, and back on in `onStart`.
     */
    public var isActive: Boolean
        get() = session.isActive
        set(value) {
            session.isActive = value
        }

    /** Tells Fire OS what the screen can do now. Call it on every change; it is cheap. */
    public fun update(controls: Controls) {
        session.setPlaybackState(
            PlaybackState.Builder()
                .setActions(PlaybackMapping.actions(controls))
                .setState(PlaybackMapping.state(controls), PlaybackState.PLAYBACK_POSITION_UNKNOWN, PlaybackMapping.speed(controls))
                .build(),
        )
    }

    /** Call from `Activity.dispatchKeyEvent`; returns true when the key became a command the app handled. */
    public fun dispatchKeyEvent(event: KeyEvent): Boolean = keys.dispatch(event, listener)

    override fun close() {
        testHook?.let { appContext.unregisterReceiver(it) }
        session.release()
    }

    private fun deliver(command: Command) {
        // INFO, not DEBUG: Fire OS drops DEBUG lines, and `adb logcat -s FireTvVoice` is how a command is traced.
        Log.i(TAG, "media session: $command")
        listener.onCommand(command, Source.MEDIA_SESSION)
    }

    public companion object {
        /** The test hook's extra: play, pause, stop, next, previous, fastforward or rewind. */
        public const val EXTRA_COMMAND: String = "cmd"

        private const val TAG = "FireTvVoice"

        /**
         * The test hook's broadcast action for [packageName]:
         * `adb shell am broadcast -a <package>.firetv.voice.TEST -p <package> --es cmd pause`.
         */
        public fun testAction(packageName: String): String = "$packageName.firetv.voice.TEST"
    }
}
