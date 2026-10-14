package io.github.tapaderuza.firetv.voice

import android.os.Process

/** Who called the session. Pure, so it is unit-tested on the JVM. */
internal object Callers {
    /**
     * The system server runs as [Process.SYSTEM_UID]; Fire OS's Home-key policy lives there. Alexa's voice service
     * and the Fire TV mobile app are ordinary apps with their own uids, and the adb test hook calls from the app's
     * own process.
     */
    fun sourceFor(callerUid: Int?): Source =
        if (callerUid == Process.SYSTEM_UID) Source.SYSTEM else Source.MEDIA_SESSION
}
