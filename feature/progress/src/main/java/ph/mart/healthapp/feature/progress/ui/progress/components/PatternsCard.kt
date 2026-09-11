package ph.mart.healthapp.feature.progress.ui.progress.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.R
import ph.mart.healthapp.feature.progress.ui.progress.PATTERN_WINDOW_DAYS
import ph.mart.healthapp.feature.progress.ui.progress.Pattern
import ph.mart.healthapp.feature.progress.ui.progress.PatternUnit

/**
 * What moves with what, in the user's own log — see [ph.mart.healthapp.feature.progress.ui.progress.patterns]
 * for what the app is and isn't willing to compare.
 *
 * An ordinary [AppCard], deliberately: the projection card above it is this screen's one
 * `tertiaryContainer` and nothing here comes from a model. No status dot either — the dot means
 * on-track, and a comparison is not a verdict.
 *
 * Empty draws nothing at all rather than a card explaining its own absence, which is the rule the
 * recap card and the insight card already follow.
 */
@Composable
internal fun PatternsCard(patterns: List<Pattern>, modifier: Modifier = Modifier) {
    if (patterns.isEmpty()) return
    AppCard(modifier = modifier) {
        Text(
            text = stringResource(R.string.progress_patterns_title),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            patterns.forEach { pattern ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(pattern.title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        // Both day counts ride in the sentence: "12 days against 9" is what tells
                        // the reader how much to trust the line.
                        text = stringResource(
                            pattern.sentence,
                            pattern.highDays,
                            formatOutcome(pattern.highOutcome, pattern.unit),
                            pattern.lowDays,
                            formatOutcome(pattern.lowOutcome, pattern.unit),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Text(
            text = stringResource(R.string.progress_patterns_note, PATTERN_WINDOW_DAYS),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

/** The fold hands over a unit and a number and leaves the words here, because a resource needs a
 * Composable and the derivation has to stay testable on the JVM. */
@Composable
private fun formatOutcome(value: Double, unit: PatternUnit): String = when (unit) {
    PatternUnit.Kcal -> stringResource(R.string.progress_kcal, value.roundToInt())
    PatternUnit.Grams -> stringResource(R.string.progress_recap_grams, value.roundToInt())
    PatternUnit.Score -> stringResource(R.string.progress_patterns_score, value)
}

private val PREVIEW_PATTERNS = listOf(
    Pattern(
        title = R.string.progress_pattern_sleep_calories_title,
        sentence = R.string.progress_pattern_sleep_calories,
        unit = PatternUnit.Kcal,
        highDays = 12,
        lowDays = 9,
        highOutcome = 1_842.0,
        lowOutcome = 2_104.0,
        strength = 0.9,
    ),
    Pattern(
        title = R.string.progress_pattern_training_protein_title,
        sentence = R.string.progress_pattern_training_protein,
        unit = PatternUnit.Grams,
        highDays = 8,
        lowDays = 17,
        highOutcome = 148.0,
        lowOutcome = 126.0,
        strength = 0.7,
    ),
    Pattern(
        title = R.string.progress_pattern_steps_mood_title,
        sentence = R.string.progress_pattern_steps_mood,
        unit = PatternUnit.Score,
        highDays = 11,
        lowDays = 11,
        highOutcome = 4.2,
        lowOutcome = 3.4,
        strength = 0.6,
    ),
)

@PreviewLightDark
@Composable
private fun PatternsCardPreview() {
    AppTheme {
        Surface {
            PatternsCard(patterns = PREVIEW_PATTERNS, modifier = Modifier.padding(16.dp))
        }
    }
}
