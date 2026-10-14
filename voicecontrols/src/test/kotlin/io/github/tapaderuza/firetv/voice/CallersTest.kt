package io.github.tapaderuza.firetv.voice

import android.os.Process
import org.junit.Assert.assertEquals
import org.junit.Test

class CallersTest {
    @Test
    fun theSystemServerIsTheSystem() {
        assertEquals(Source.SYSTEM, Callers.sourceFor(Process.SYSTEM_UID))
    }

    @Test
    fun appsAndUnknownCallersAreTheMediaSession() {
        // Alexa's voice service and the Fire TV app are ordinary apps (uid 10000+); before API 28 the caller is unknown.
        for (uid in listOf(10036, 10227, 2000 /* shell */, null)) {
            assertEquals("uid $uid", Source.MEDIA_SESSION, Callers.sourceFor(uid))
        }
    }
}
