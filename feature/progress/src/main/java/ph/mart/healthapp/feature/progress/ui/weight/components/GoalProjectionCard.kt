package ph.mart.healthapp.feature.progress.ui.weight.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.kgToDisplayUnit
import ph.mart.healthapp.core.data.profile.weightUnitLabel
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.component.goalProjectionLine
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.ui.progress.components.Note
import ph.mart.healthapp.core.data.progress.GoalProjection
import ph.mart.healthapp.core.data.progress.PROJECTION_FLAT_KG_PER_WEEK
import ph.mart.healthapp.core.data.progress.PROJECTION_WINDOW_DAYS

/**
 * "When do I get there?", under the stat row that already says how far there is. Everything is
 * derived in [ph.mart.healthapp.core.data.progress.goalProjection]; this only formats.
 *
 * Deliberately monochrome — no `error` colour even when the trend points away from the goal.
 * [WeightStatRow] directly above already colours the change cell via `goalRelativeTrend`, so
 * nothing is hidden, and a red projection sentence reads as a verdict on the week rather than
 * a fact about the data.
 */
@Composable
fun GoalProjectionCard(
    projection: GoalProjection,
    unit: UnitSystem,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier) {
        Text(
            text = "Goal projection",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        val goalWeight = "${formatKg(projection.goalWeightKg.kgToDisplayUnit(unit))} ${unit.weightUnitLabel()}"
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            // The sentence itself lives in `:core:designsystem` — Home's weight card and the
            // weekly recap say the same words, and one copy is what keeps them saying them.
            Text(
                text = goalProjectionLine(
                    goalWeightLabel = goalWeight,
                    targetEpochDay = projection.targetEpochDay,
                    reached = projection.reached,
                    windowDays = PROJECTION_WINDOW_DAYS,
                ),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Note(rateNote(projection.kgPerWeek, unit))
        }
    }
}

/** The rate is reported in every state — it is most useful exactly when there is no date. */
private fun rateNote(kgPerWeek: Double, unit: UnitSystem): String {
    if (abs(kgPerWeek) < PROJECTION_FLAT_KG_PER_WEEK) return "Holding steady this month."
    val value = formatKg(kgPerWeek.kgToDisplayUnit(unit))
    return "Trending ${if (kgPerWeek > 0) "+" else ""}$value ${unit.weightUnitLabel()} per week."
}

@PreviewLightDark
@Composable
private fun GoalProjectionCardPreview() {
    AppTheme {
        Surface {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(16.dp),
            ) {
                GoalProjectionCard(
                    projection = GoalProjection(
                        goalWeightKg = 72.0,
                        kgPerWeek = -0.4,
                        targetEpochDay = 20_760,
                        reached = false,
                    ),
                    unit = UnitSystem.Metric,
                )
                // Trending away from a Lose goal: a neutral line, never a red verdict.
                GoalProjectionCard(
                    projection = GoalProjection(
                        goalWeightKg = 72.0,
                        kgPerWeek = 0.3,
                        targetEpochDay = null,
                        reached = false,
                    ),
                    unit = UnitSystem.Metric,
                )
                GoalProjectionCard(
                    projection = GoalProjection(
                        goalWeightKg = 72.0,
                        kgPerWeek = -0.05,
                        targetEpochDay = null,
                        reached = true,
                    ),
                    unit = UnitSystem.Imperial,
                )
            }
        }
    }
}
