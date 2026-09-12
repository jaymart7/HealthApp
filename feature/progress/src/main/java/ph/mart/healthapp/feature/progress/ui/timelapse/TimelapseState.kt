package ph.mart.healthapp.feature.progress.ui.timelapse

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

@Composable
internal fun rememberTimelapseState(): TimelapseState =
    rememberSaveable(saver = TimelapseState.Saver()) { TimelapseState() }

/** The middle of [TIMELAPSE_FPS] — four frames a second reads as a timelapse rather than a
 * slideshow or a flicker. */
private const val DEFAULT_SPEED = 1

/** UI-only: which frame is up, whether it is running, how fast, and whether the share sheet is
 * open. None of it means anything outside the player, and all of it rides the saver — a rotation
 * mid-playback that restarted from frame one would lose the week you had scrubbed to. */
internal class TimelapseState(
    index: Int = 0,
    playing: Boolean = true,
    speed: Int = DEFAULT_SPEED,
    sharing: Boolean = false,
) {
    var index: Int by mutableIntStateOf(index)
    var playing: Boolean by mutableStateOf(playing)
    var speed: Int by mutableIntStateOf(speed)
    var sharing: Boolean by mutableStateOf(sharing)

    /** Scrubbing takes over: a timeline that kept advancing under the finger would fight whoever is
     * looking for one particular week. */
    fun scrubTo(frame: Int, frameCount: Int) {
        playing = false
        index = frame.coerceIn(0, (frameCount - 1).coerceAtLeast(0))
    }

    /** One control, three speeds — a chip that cycles rather than three pills that don't fit
     * beside the transport row. */
    fun cycleSpeed() {
        speed = (speed + 1) % TIMELAPSE_FPS.size
    }

    companion object {
        fun Saver(): Saver<TimelapseState, Any> = listSaver(
            save = { listOf(it.index, it.playing, it.speed, it.sharing) },
            restore = { saved ->
                TimelapseState(saved[0] as Int, saved[1] as Boolean, saved[2] as Int, saved[3] as Boolean)
            },
        )
    }
}
