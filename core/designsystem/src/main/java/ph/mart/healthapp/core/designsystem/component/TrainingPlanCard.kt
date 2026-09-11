package ph.mart.healthapp.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.R
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums

private val CELL_RADIUS = 8.dp
private val DOT_SIZE = 6.dp
private val TapTargetMin = 44.dp

/**
 * One routine the plan asks for today. [summary] is the caller's sentence ("2 lifts · 6 sets ·
 * Mon · Wed · Fri") rather than a `Routine`, and [PlanCell] carries words rather than an epoch
 * day, for the reason this card lives here at all: `:core:designsystem` is a leaf with no project
 * dependencies, and the six enums whose labels a feature renders are the standing argument for
 * keeping it one. So `Routine`, `PlanDay` and `weekdayNames()` stay in `:core:data` and the two
 * callers map into these.
 */
data class PlannedRoutine(val id: Long, val name: String, val summary: String)

/** One day of the week strip: what it is called, what was intended, and what happened. */
data class PlanCell(
    val initial: String,
    val name: String,
    val planned: Boolean,
    val trained: Boolean,
    val isToday: Boolean,
)

/**
 * What the week's plan asks for today, and how the week is going. Drawn by Home's Workout card and
 * by the Train tab, which is why it sits here rather than in either.
 *
 * Profile → Workout routines authors — the division the supplement list already draws, so there is
 * no editing here and no way to change which days a routine falls on.
 *
 * A day with nothing planned reads **Rest day** rather than hiding the card: once a plan exists,
 * "nothing today" is the answer the user came for. The card as a whole is hidden only until the
 * first routine has days set, which the caller gates on.
 *
 * [trained] is any strength workout logged today, not "the planned routine was performed" —
 * nothing links a logged workout back to a routine, and this feature does not add that link. So
 * the Start button is replaced by a done line once *something* was lifted, rather than offering to
 * start a session that is already in the diary.
 *
 * [plannedSoFar]/[trainedSoFar] arrive counted rather than derived here: `plannedSoFar()` and
 * `trainedSoFar()` in `:core:data/exercise/TrainingPlan.kt` are the one definition of "so far",
 * and `TrainingPlanTest` is what holds them to it.
 */
@Composable
fun TrainingPlanCard(
    todayRoutines: List<PlannedRoutine>,
    week: List<PlanCell>,
    trained: Boolean,
    plannedSoFar: Int,
    trainedSoFar: Int,
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
                text = stringResource(R.string.ds_plan_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // Nothing has been asked of the week yet (a Friday-only plan read on Monday), so
            // there is no ratio to print — "0 of 0" reads as a broken counter, not as a rest.
            if (plannedSoFar > 0) {
                Text(
                    text = stringResource(R.string.ds_plan_ratio, trainedSoFar, plannedSoFar),
                    style = MaterialTheme.typography.titleSmall.tabularNums,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        when {
            todayRoutines.isEmpty() -> Text(
                text = stringResource(if (trained) R.string.ds_plan_rest_trained else R.string.ds_plan_rest),
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
private fun PlannedRoutineRow(routine: PlannedRoutine, trained: Boolean, onStart: () -> Unit) {
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
                text = routine.summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (trained) {
            Text(
                text = stringResource(R.string.ds_plan_logged),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            PrimaryButton(label = stringResource(R.string.ds_plan_start), onClick = onStart)
        }
    }
}

/**
 * Monday to Sunday, one cell a day.
 *
 * Three states, and the dot carries all three so the cell is free to say only where today is:
 * `primary` for a day that was trained, `outline` for one the plan asked for and did not get, and
 * transparent for a day nothing was planned on. Transparent rather than absent, so every label sits
 * on the same baseline whatever shape the week is.
 *
 * The whole strip carries one description rather than seven — it is a summary, and the figure
 * beside the heading already says it in words.
 */
@Composable
private fun WeekStrip(week: List<PlanCell>, modifier: Modifier = Modifier) {
    // mapNotNull is inline, so stringResource is legal inside it; joinToString is not, which is
    // why the two steps stay separate — the shape this file has always had.
    val spoken = week.mapNotNull { day ->
        if (!day.planned) {
            null
        } else {
            stringResource(
                if (day.trained) R.string.ds_plan_day_done else R.string.ds_plan_day_planned,
                day.name,
            )
        }
    }.joinToString(", ")
    val nothingPlanned = stringResource(R.string.ds_plan_nothing)
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = nothingPlanned.takeIf { spoken.isEmpty() } ?: spoken
            },
    ) {
        week.forEach { day ->
            // Today wears the same `secondaryContainer` pill the selected nav tab does — one
            // "you are here" treatment across the app, not a second one invented for a strip.
            val onCell = if (day.isToday) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
            Surface(
                shape = RoundedCornerShape(CELL_RADIUS),
                color = if (day.isToday) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                modifier = Modifier.weight(1f).heightIn(min = TapTargetMin),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Text(
                        text = day.initial,
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                        color = onCell,
                    )
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .size(DOT_SIZE)
                            .background(
                                color = when {
                                    day.trained -> MaterialTheme.colorScheme.primary
                                    day.planned -> MaterialTheme.colorScheme.outline
                                    else -> Color.Transparent
                                },
                                shape = CircleShape,
                            ),
                    )
                }
            }
        }
    }
}

private val PREVIEW_DAYS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

private fun previewWeek(todayIndex: Int, planned: Set<Int>, trainedDays: Set<Int>) =
    PREVIEW_DAYS.mapIndexed { index, name ->
        PlanCell(
            initial = name.take(1),
            name = name,
            planned = index in planned,
            trained = index in trainedDays,
            isToday = index == todayIndex,
        )
    }

private val previewPushDay = PlannedRoutine(id = 1, name = "Push day", summary = "2 lifts · 6 sets · Mon · Wed · Fri")

@PreviewLightDark
@Composable
private fun TrainingPlanCardPreview() {
    AppTheme {
        TrainingPlanCard(
            todayRoutines = listOf(previewPushDay),
            week = previewWeek(todayIndex = 2, planned = setOf(0, 2, 4), trainedDays = setOf(0)),
            trained = false,
            plannedSoFar = 2,
            trainedSoFar = 1,
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
            todayRoutines = listOf(previewPushDay),
            week = previewWeek(todayIndex = 2, planned = setOf(0, 2, 4), trainedDays = setOf(0, 2)),
            trained = true,
            plannedSoFar = 2,
            trainedSoFar = 2,
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
            plannedSoFar = 2,
            trainedSoFar = 2,
            onStart = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
