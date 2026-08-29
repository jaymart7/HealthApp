package ph.mart.healthapp.feature.home.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentNeutral
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.SentimentVeryDissatisfied
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.mood.MoodLevel
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.Motion
import ph.mart.healthapp.core.designsystem.theme.rememberFillDirection
import ph.mart.healthapp.core.designsystem.theme.stepFillProgress

private val STEP_SIZE = 32.dp

private val MoodFaces = listOf(
    Icons.Filled.SentimentVeryDissatisfied,
    Icons.Filled.SentimentDissatisfied,
    Icons.Filled.SentimentNeutral,
    Icons.Filled.SentimentSatisfied,
    Icons.Filled.SentimentVerySatisfied,
)

/**
 * The day's reflection: how you felt, and how much you had in the tank. Both rows are 1–5 and
 * 0 means untouched, so logging one without the other is a first-class state rather than a
 * half-filled form.
 *
 * The two rows read differently on purpose. Mood is a **single choice** — only the face you
 * picked is tinted, because lighting up sad *and* neutral *and* happy to reach "good" says the
 * wrong thing. Energy is a **meter** that fills up to the level, the same read as
 * [ph.mart.healthapp.core.designsystem.component.WaterGlassRow].
 *
 * That difference carries into the motion too: energy staggers as it fills, mood doesn't, because
 * a single choice has no direction to travel in.
 *
 * Feature-local rather than `:core:designsystem`: Home is the only screen that logs mood.
 */
@Composable
fun MoodCard(
    mood: Int,
    energy: Int,
    onSetMood: (Int) -> Unit,
    onSetEnergy: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier) {
        Text(
            text = "How are you feeling?",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        ScaleRow(
            label = "Mood",
            level = mood,
            icon = { index -> MoodFaces[index] },
            active = { index -> index + 1 == mood },
            describe = { level -> "Set mood to ${MoodLevel.entries[level - 1].label}" },
            onSelect = onSetMood,
        )
        ScaleRow(
            label = "Energy",
            level = energy,
            icon = { index -> if (index < energy) Icons.Filled.Bolt else Icons.Outlined.Bolt },
            active = { index -> index < energy },
            describe = { level -> "Set energy to ${MoodLevel.entries[level - 1].label}" },
            onSelect = onSetEnergy,
            modifier = Modifier.padding(top = 4.dp),
            stagger = true,
        )
    }
}

/**
 * [active] decides which steps are tinted, [icon] which glyph each draws — the only two things
 * that differ between the mood and energy rows. [stagger] adds the third: a meter fills in
 * sequence, a single choice lands all at once.
 *
 * Tapping the level you are already on clears it back to 0, so a mis-tap is corrected by the
 * same gesture that made it and the card needs no separate undo.
 */
@Composable
private fun ScaleRow(
    label: String,
    level: Int,
    icon: (Int) -> ImageVector,
    active: (Int) -> Boolean,
    describe: (Int) -> String,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    stagger: Boolean = false,
) {
    val inactiveTint = MaterialTheme.colorScheme.outlineVariant
    val activeTint = MaterialTheme.colorScheme.primary
    val filling = rememberFillDirection(level)
    val count = MoodLevel.entries.size

    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 12.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            MoodLevel.entries.forEachIndexed { index, entry ->
                val fill = stepFillProgress(
                    active = active(index),
                    index = index,
                    count = count,
                    filling = filling,
                    stagger = stagger,
                )
                IconButton(
                    onClick = { onSelect(if (entry.value == level) 0 else entry.value) },
                    modifier = Modifier
                        .size(STEP_SIZE)
                        // Read inside the layer lambda: the pop settles in the Draw phase.
                        .graphicsLayer {
                            scaleX = 1f + (Motion.ActiveStepScale - 1f) * fill.value
                            scaleY = scaleX
                        }
                        // Announces the level the tap would set, not "face 4 of 5" — the same
                        // reasoning WaterGlassRow uses.
                        .clearAndSetSemantics { contentDescription = describe(entry.value) },
                ) {
                    Icon(
                        imageVector = icon(index),
                        contentDescription = null,
                        tint = lerp(inactiveTint, activeTint, fill.value),
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun MoodCardPreview() {
    AppTheme {
        Surface {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(16.dp),
            ) {
                MoodCard(mood = 0, energy = 0, onSetMood = {}, onSetEnergy = {})
                MoodCard(mood = 4, energy = 0, onSetMood = {}, onSetEnergy = {})
                MoodCard(mood = 2, energy = 3, onSetMood = {}, onSetEnergy = {})
            }
        }
    }
}
