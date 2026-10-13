package io.github.tapaderuza.firetv.voice.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import io.github.tapaderuza.firetv.voice.Command
import io.github.tapaderuza.firetv.voice.Source
import kotlinx.coroutines.delay

private data class Slide(val kicker: String, val title: String, val body: String)

private val SLIDES = listOf(
    Slide("6:00 PM · Strength", "Back squat 5×5", "Rest two minutes between sets"),
    Slide("Workout of the day", "AMRAP 12", "5 pull-ups · 10 push-ups · 15 air squats"),
    Slide("Cool-down", "Stretch it out", "Hips, hamstrings and shoulders, five minutes"),
    Slide("Front desk", "Bring a friend Friday", "Your first class together is on us"),
)

private const val SLIDE_MS = 6_000L
private val Background = Color(0xFF101418)
private val Accent = Color(0xFFFF5A1F)
private val Muted = Color(0xFFB8C0CC)

class BoardState {
    var playing by mutableStateOf(true)
    var slide by mutableIntStateOf(0)
        private set
    var helpVisible by mutableStateOf(false)
    var last by mutableStateOf<Pair<Command, Source>?>(null)

    fun step(by: Int) {
        slide = Math.floorMod(slide + by, SLIDES.size)
    }
}

@Composable
fun ClassBoard(board: BoardState) {
    // Rotate while playing; any page change, by hand or by voice, gives the new slide a full turn.
    LaunchedEffect(board.playing, board.slide) {
        if (board.playing) {
            delay(SLIDE_MS)
            board.step(+1)
        }
    }
    LaunchedEffect(board.helpVisible) {
        if (board.helpVisible) {
            delay(8_000)
            board.helpVisible = false
        }
    }
    val slide = SLIDES[board.slide]
    // 5% overscan margins: TVs crop the edges of the picture.
    Box(Modifier.fillMaxSize().background(Background).padding(horizontal = 48.dp, vertical = 27.dp)) {
        StatusChip(board.playing, Modifier.align(Alignment.TopStart))
        Text("${board.slide + 1} / ${SLIDES.size}", Modifier.align(Alignment.TopEnd), color = Muted, fontSize = 22.sp)
        Column(Modifier.align(Alignment.CenterStart)) {
            Text(slide.kicker.uppercase(), color = Accent, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(slide.title, color = Color.White, fontSize = 72.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(8.dp))
            Text(slide.body, color = Muted, fontSize = 32.sp)
        }
        Column(Modifier.align(Alignment.BottomStart)) {
            Text(lastCommand(board.last), color = Color.White, fontSize = 22.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                "Say “Alexa, pause”, “Alexa, resume”, “Alexa, next” · or use ⏯  ◀ ▶  and Menu",
                color = Muted,
                fontSize = 18.sp,
            )
        }
        if (board.helpVisible) Help(Modifier.align(Alignment.Center))
    }
}

private fun lastCommand(last: Pair<Command, Source>?): String {
    if (last == null) return "Waiting for a command"
    val (command, source) = last
    val via = if (source == Source.MEDIA_SESSION) "Alexa / media session" else "remote key"
    return "Last command: ${command.name.lowercase().replace('_', ' ')} · via $via"
}

@Composable
private fun StatusChip(playing: Boolean, modifier: Modifier) {
    Row(
        modifier.background(Color(0x33FFFFFF), RoundedCornerShape(50)).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(12.dp).background(if (playing) Color(0xFF3DDC84) else Color(0xFFFFC107), CircleShape))
        Text(if (playing) "PLAYING" else "PAUSED", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Help(modifier: Modifier) {
    val rows = listOf(
        "“Alexa, pause” · “Alexa, stop”" to "hold this slide",
        "“Alexa, resume”" to "rotate again",
        "“Alexa, next” · “Alexa, previous”" to "page the slides",
        "⏯ or select" to "pause / resume",
        "◀ ▶ · rewind, fast-forward" to "page the slides",
        "Menu" to "this panel",
    )
    Column(
        modifier.width(760.dp).background(Color(0xFF1B2028), RoundedCornerShape(16.dp)).padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Controls", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        for ((what, does) in rows) {
            Row {
                Text(what, Modifier.width(440.dp), color = Color.White, fontSize = 22.sp)
                Text(does, color = Muted, fontSize = 22.sp)
            }
        }
    }
}
