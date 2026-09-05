package ph.mart.healthapp.feature.home.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.res.stringResource
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
import ph.mart.healthapp.feature.home.R

private val STEP_ICON = 24.dp
private val NAME_WIDTH = 52.dp

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
 * **Both rows are meters** — they fill up to the level, the same read as
 * [ph.mart.healthapp.core.designsystem.component.WaterGlassRow], energy, water, cycle flow and
 * supplements. Mood used to be a single choice on the argument that lighting up sad *and* neutral
 * *and* happy to reach "good" says the wrong thing; the redesign reversed that deliberately. Every
 * other tappable row on Home is a meter, and one row that looked identical and answered a tap
 * differently was the odd one out — the glyphs already carry the difference between a 2 and a 5,
 * so the fill does not have to.
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
    // Resolved here rather than in the `describe` lambdas: those are plain lambdas, and an
    // @StringRes Int interpolated into a string template renders as its number.
    val levelLabels = MoodLevel.entries.map { stringResource(it.label) }
    val moodDescriptions = levelLabels.map { stringResource(R.string.home_mood_set_mood, it) }
    val energyDescriptions = levelLabels.map { stringResource(R.string.home_mood_set_energy, it) }
    AppCard(modifier = modifier) {
        Text(
            text = stringResource(R.string.home_mood_title),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        ScaleRow(
            label = stringResource(R.string.home_mood_mood),
            level = mood,
            icon = { index -> MoodFaces[index] },
            describe = { level -> moodDescriptions[level - 1] },
            onSelect = onSetMood,
        )
        ScaleRow(
            label = stringResource(R.string.home_mood_energy),
            level = energy,
            icon = { index -> if (index < energy) Icons.Filled.Bolt else Icons.Outlined.Bolt },
            describe = { level -> energyDescriptions[level - 1] },
            onSelect = onSetEnergy,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/**
 * One meter: steps `1..level` are tinted, [icon] decides which glyph each draws, and that glyph is
 * the only thing that differs between the mood and energy rows.
 *
 * The steps **divide the row's width** rather than sitting at a fixed size, exactly as
 * [ph.mart.healthapp.core.designsystem.component.WaterGlassRow]'s glasses do — that is what holds
 * the [TapTargetMin] floor on a narrow screen without wrapping the row onto a second line.
 *
 * Tapping the level you are already on clears it back to 0, so a mis-tap is corrected by the
 * same gesture that made it and the card needs no separate undo.
 */
@Composable
private fun ScaleRow(
    label: String,
    level: Int,
    icon: (Int) -> ImageVector,
    describe: (Int) -> String,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
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
            modifier = Modifier.width(NAME_WIDTH),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
            MoodLevel.entries.forEachIndexed { index, entry ->
                val fill = stepFillProgress(
                    active = index < level,
                    index = index,
                    count = count,
                    filling = filling,
                    stagger = true,
                )
                IconButton(
                    onClick = { onSelect(if (entry.value == level) 0 else entry.value) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = TapTargetMin)
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
                        modifier = Modifier.size(STEP_ICON),
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
