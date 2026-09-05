package ph.mart.healthapp.feature.home.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.exercise.PlanDay
import ph.mart.healthapp.core.data.exercise.Routine
import ph.mart.healthapp.core.data.exercise.RoutineLift
import ph.mart.healthapp.core.data.exercise.dayLabel
import ph.mart.healthapp.core.data.exercise.plannedSoFar
import ph.mart.healthapp.core.data.exercise.totalSets
import ph.mart.healthapp.core.data.exercise.trainedSoFar
import ph.mart.healthapp.core.data.exercise.weekdayInitials
import ph.mart.healthapp.core.data.exercise.weekdayNames
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.home.R

/**
 * What the week's plan asks for today, and how the week is going.
 *
 * Home renders; Profile → Workout routines authors — the division the supplement list already
 * draws, so there is no editing here and no way to change which days a routine falls on.
 *
 * A day with nothing planned reads **Rest day** rather than hiding the card: once a plan exists,
 * "nothing today" is the answer the user came for. The card as a whole is hidden only until the
 * first routine has days set, which the caller gates on.
 *
 * [trained] is any strength workout logged today, not "the planned routine was performed" —
 * nothing links a logged workout back to a routine, and this feature does not add that link. So
 * the Start button is replaced by a done line once *something* was lifted, rather than offering to
 * start a session that is already in the diary.
 */
@Composable
fun TrainingPlanCard(
    todayRoutines: List<Routine>,
    week: List<PlanDay>,
    trained: Boolean,
    onStart: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.home_plan_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // Nothing has been asked of the week yet (a Friday-only plan read on Monday), so
            // there is no ratio to print — "0 of 0" reads as a broken counter, not as a rest.
            if (week.plannedSoFar() > 0) {
                Text(
                    text = stringResource(R.string.home_plan_ratio, week.trainedSoFar(), week.plannedSoFar()),
                    style = MaterialTheme.typography.titleSmall.tabularNums,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        when {
            todayRoutines.isEmpty() -> Text(
                text = stringResource(if (trained) R.string.home_plan_rest_trained else R.string.home_plan_rest),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            else -> todayRoutines.forEach { routine ->
                PlannedRoutineRow(routine = routine, trained = trained, onStart = { onStart(routine.id) })
            }
        }

        WeekStrip(week = week, modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun PlannedRoutineRow(routine: Routine, trained: Boolean, onStart: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = routine.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(
                    R.string.home_plan_summary,
                    pluralStringResource(R.plurals.home_plan_lifts, routine.lifts.size, routine.lifts.size),
                    routine.totalSets(),
                    routine.dayLabel(),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (trained) {
            Text(
                text = stringResource(R.string.home_plan_logged),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            PrimaryButton(label = stringResource(R.string.home_plan_start), onClick = onStart)
        }
    }
}

/**
 * Monday to Sunday: a filled dot for a day that was trained, a ringed one for a day that was
 * planned and wasn't, and a faint one for a day the plan never asked for.
 *
 * The whole strip carries one description rather than seven — the dots are a summary, and the
 * figure beside the heading already says it in words.
 */
@Composable
private fun WeekStrip(week: List<PlanDay>, modifier: Modifier = Modifier) {
    val names = weekdayNames()
    val spoken = week.mapIndexedNotNull { index, day ->
        if (!day.planned) {
            null
        } else {
            stringResource(
                if (day.trained) R.string.home_plan_day_done else R.string.home_plan_day_planned,
                names[index],
            )
        }
    }.joinToString(", ")
    val nothingPlanned = stringResource(R.string.home_plan_nothing)
    val initials = weekdayInitials()
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = nothingPlanned.takeIf { spoken.isEmpty() } ?: spoken
            },
    ) {
        week.forEachIndexed { index, day ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = CircleShape,
                    color = when {
                        day.trained -> MaterialTheme.colorScheme.primary
                        day.planned -> MaterialTheme.colorScheme.surfaceContainerHighest
                        else -> MaterialTheme.colorScheme.surface
                    },
                    border = if (day.planned && !day.trained) {
                        BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                    } else {
                        null
                    },
                    content = {},
                    modifier = Modifier.size(if (day.isToday) 18.dp else 14.dp),
                )
                Text(
                    text = initials[index],
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    color = if (day.isToday) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

private fun previewWeek(todayIndex: Int, planned: Set<Int>, trainedDays: Set<Int>) =
    (0..6).map { index ->
        PlanDay(
            epochDay = 20_000L + index,
            planned = index in planned,
            trained = index in trainedDays,
            isToday = index == todayIndex,
        )
    }

private val pushDay = Routine(
    id = 1,
    name = "Push day",
    lifts = listOf(RoutineLift("Bench press", 3, 8), RoutineLift("Overhead press", 3, 8)),
    days = 0b0010101,
)

@PreviewLightDark
@Composable
private fun TrainingPlanCardPreview() {
    AppTheme {
        TrainingPlanCard(
            todayRoutines = listOf(pushDay),
            week = previewWeek(todayIndex = 2, planned = setOf(0, 2, 4), trainedDays = setOf(0)),
            trained = false,
            onStart = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

/** Today's session is already in the diary: the button goes, the dot fills. */
@PreviewLightDark
@Composable
private fun TrainingPlanCardDonePreview() {
    AppTheme {
        TrainingPlanCard(
            todayRoutines = listOf(pushDay),
            week = previewWeek(todayIndex = 2, planned = setOf(0, 2, 4), trainedDays = setOf(0, 2)),
            trained = true,
            onStart = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

/** Nothing planned today — said out loud rather than by an absent card. */
@PreviewLightDark
@Composable
private fun TrainingPlanCardRestDayPreview() {
    AppTheme {
        TrainingPlanCard(
            todayRoutines = emptyList(),
            week = previewWeek(todayIndex = 3, planned = setOf(0, 2, 4), trainedDays = setOf(0, 2)),
            trained = false,
            onStart = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
