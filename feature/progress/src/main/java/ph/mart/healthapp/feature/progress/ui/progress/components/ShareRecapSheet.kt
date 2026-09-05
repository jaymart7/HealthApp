package ph.mart.healthapp.feature.progress.ui.progress.components

import android.graphics.Picture
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ph.mart.healthapp.core.data.food.NutritionAverages
import ph.mart.healthapp.core.data.mood.MoodAverages
import ph.mart.healthapp.core.data.profile.DailyTargets
import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.WeightTrendDisplay
import ph.mart.healthapp.core.data.progress.GoalProjection
import ph.mart.healthapp.core.designsystem.component.AppBottomSheet
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.R
import ph.mart.healthapp.feature.progress.ui.progress.BestDay
import ph.mart.healthapp.feature.progress.ui.progress.Recap
import ph.mart.healthapp.feature.progress.ui.progress.RecapPeriod
import ph.mart.healthapp.feature.progress.ui.shared.captureToPicture
import ph.mart.healthapp.feature.progress.ui.shared.sharePng

/**
 * Preview-then-share for the recap: the sheet shows exactly the PNG that leaves the app, which is
 * why the branding can exist here without ever appearing on the Progress screen.
 *
 * The image is one card rather than the whole recap page on purpose — `captureToPicture` records
 * what was *drawn*, so capturing a scrolling column would hand the chooser a screenshot clipped
 * at the fold.
 *
 * [RecapCard] is rendered verbatim — every figure, and every colour rule behind it, stays
 * the card's. This only adds the opaque ground a shared image needs (a captured layer is
 * transparent wherever nothing painted) and the footer that says which app drew it.
 *
 * No `NavigationEventHandler`: [AppBottomSheet] delegates to `ModalBottomSheet`, which already
 * takes back, and this is a leaf with no sub-level of its own.
 */
@Composable
internal fun ShareRecapSheet(
    recap: Recap,
    goal: Goal?,
    unit: UnitSystem,
    projection: GoalProjection?,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val picture = remember { Picture() }

    AppBottomSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .captureToPicture(picture)
                .background(MaterialTheme.colorScheme.surface)
                .padding(vertical = 8.dp),
        ) {
            RecapCard(recap = recap, goal = goal, unit = unit, projection = projection)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                MascotAvatar(state = MascotState.Happy, size = 24.dp)
                Text(
                    text = stringResource(R.string.progress_strip_brand),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        PrimaryButton(
            label = stringResource(R.string.progress_share),
            onClick = {
                scope.launch {
                    // Zero until the sheet has drawn a frame — a tap that fast would otherwise
                    // hand the chooser an empty file.
                    if (picture.width > 0) {
                        sharePng(context, picture, "fitpulse-recap.png")
                        onDismiss()
                    }
                }
            },
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

@PreviewLightDark
@Composable
private fun ShareRecapSheetPreview() {
    AppTheme {
        ShareRecapSheet(
            recap = Recap(
                period = RecapPeriod.Week,
                daysLogged = 6,
                averages = NutritionAverages(1940, 141, 196, 68, daysLogged = 5),
                targets = DailyTargets(calories = 2000, proteinG = 150, carbsG = 200, fatG = 67, floor = 1500),
                weightTrend = WeightTrendDisplay(currentKg = 76.0, deltaKg = -0.8, hasPrior = true),
                moodAverages = MoodAverages(mood = 3.6, energy = 3.1, daysLogged = 6),
                bestDay = BestDay(dateEpochDay = 20_690, calories = 1985),
            ),
            goal = Goal.Lose,
            unit = UnitSystem.Metric,
            projection = GoalProjection(
                goalWeightKg = 72.0,
                kgPerWeek = -0.4,
                targetEpochDay = 20_760,
                reached = false,
            ),
            onDismiss = {},
        )
    }
}
