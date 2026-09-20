package ph.mart.healthapp.feature.coach.ui.components

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import ph.mart.healthapp.core.data.food.DayNutrition
import ph.mart.healthapp.core.data.food.NutritionAverages
import ph.mart.healthapp.core.data.health.StepAverages
import ph.mart.healthapp.core.data.health.StepDay
import ph.mart.healthapp.core.data.health.formatSteps
import ph.mart.healthapp.core.data.profile.DailyTargets
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.WeightTrendDisplay
import ph.mart.healthapp.core.data.profile.kgToDisplayUnit
import ph.mart.healthapp.core.data.profile.weightUnitLabel
import ph.mart.healthapp.core.data.recap.BestDay
import ph.mart.healthapp.core.data.recap.REPORT_DAYS
import ph.mart.healthapp.core.data.recap.Recap
import ph.mart.healthapp.core.data.recap.Report
import ph.mart.healthapp.core.data.recap.ReportSection
import ph.mart.healthapp.core.data.exercise.LiftRecord
import ph.mart.healthapp.core.data.exercise.StrengthTotals
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.component.DayBar
import ph.mart.healthapp.core.designsystem.component.DayBarChart
import ph.mart.healthapp.core.designsystem.component.SegmentedToggle
import ph.mart.healthapp.core.designsystem.component.formatOneDecimal
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.coach.R

/**
 * The window the coach drew, folded from Room and worked by the user.
 *
 * It is `ProposalCard`'s sibling and its opposite. Both are the app speaking inside the coach's
 * turn, and both carry figures no model was handed — but a proposal is a decision waiting on a
 * tap, so it wears the `tertiaryContainer` AI accent and has two buttons. A report has decided
 * nothing and writes nothing ever, so it is an ordinary [AppCard] with a rule under its heading.
 * Making it look like a proposal would be the card asking a question it does not have.
 *
 * **Nothing here is stored.** The transcript keeps the *window* and this re-folds it from the
 * repositories on every emission, which is why a meal logged in the Food tab moves the average on
 * a card already on screen. It is also why the period chips are free: both windows are already in
 * `CoachUiState.reports`.
 *
 * **Chips are view state, not a write.** Switching to 7 days changes what is drawn and not what
 * the turn asked for — the transcript is a record of what was said, and a tap on a card is not a
 * second question. Which is also why switching leaves the sentence above it alone: that sentence
 * introduced the window the coach chose.
 *
 * That state is held **here**, not in `CoachScreenState`, and it is the transcript's own list that
 * makes this the cheaper of the two: a `LazyColumn` item scopes its `rememberSaveable` to the
 * item's key, which is the message id — so each card in a long conversation keeps its own chip and
 * its own open section, across scrolling away and across a rotation, with no map keyed by message
 * anywhere. [drafted] is the window the *turn* asked for and is what a card opens at.
 *
 * **Two of the four sections carry a chart and two do not**, and that is the honest split rather
 * than a gap. [DayBarChart] is zero-based bars over a daily count, which is what calories and
 * steps are. A weight arc is two ends of a window and a training block is a set of totals; drawing
 * either as daily bars would be a chart that lies, and a line chart for one section is a second
 * chart idiom to keep in step across the app.
 */
@Composable
internal fun ReportCard(
    reports: Map<Int, Report>,
    drafted: Int,
    onOpenSection: (ReportSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    var days by rememberSaveable { mutableStateOf(drafted) }
    // The enum's `name` rather than the enum: `rememberSaveable` takes what a Bundle takes, and
    // a String round-trips where an enum constant does not. Null is every section closed, which
    // is how a card opens.
    var openName by rememberSaveable { mutableStateOf<String?>(null) }
    val expanded = openName?.let { name -> ReportSection.entries.firstOrNull { it.name == name } }
    // A chip for a window that is somehow not folded falls back to the drafted one rather than
    // drawing nothing — the screen has already checked that *that* one exists.
    val report = reports[days] ?: reports[drafted] ?: return

    AppCard(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.coach_report_title, report.days),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        SegmentedToggle(
            options = REPORT_DAYS.map { stringResource(R.string.coach_report_period, it) },
            selectedIndex = REPORT_DAYS.indexOf(report.days).coerceAtLeast(0),
            onSelect = { days = REPORT_DAYS[it] },
            modifier = Modifier.padding(top = 12.dp),
        )

        val recap = report.recap
        if (recap == null) {
            // Day one, or a window the user simply did not log in. The recap screen's own reading:
            // say so in a line rather than drawing four sections of zeros, which would report an
            // emptiness the app invented.
            Text(
                text = stringResource(R.string.coach_report_empty, report.days),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp),
            )
            return@AppCard
        }

        Text(
            text = stringResource(R.string.coach_report_days_logged, recap.daysLogged, report.days),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
        )

        ReportSection.entries.forEach { section ->
            ReportRow(
                section = section,
                report = report,
                recap = recap,
                open = section == expanded,
                // One open at a time: tapping a second closes the first, which is what keeps the
                // card short enough to sit in a scrolling transcript.
                onToggle = { openName = section.name.takeIf { it != openName } },
                onOpen = { onOpenSection(section) },
            )
        }
    }
}

/**
 * One section: a headline that is always readable, and the detail behind a tap.
 *
 * Collapsed by default and **one open at a time** — the card sits in a scrolling transcript above
 * an input bar, and four open sections would push the conversation off screen. The headline is the
 * part that has to survive being scrolled past, so it is the figure and never a label alone.
 *
 * The whole row is the toggle. Opening the Progress page is the separate row underneath, inside
 * the disclosure: a tap that leaves the conversation is not something to put under the thumb of
 * someone reading it.
 */
@Composable
private fun ReportRow(
    section: ReportSection,
    report: Report,
    recap: Recap,
    open: Boolean,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
) {
    val headline = section.headline(report, recap)
    HorizontalDivider(
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.padding(top = 12.dp),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            // 48dp, the app's rule — the row is the target and the glyph inside it is 20dp.
            .heightIn(min = 48.dp)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(section.label),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = headline ?: stringResource(R.string.coach_report_none),
                style = MaterialTheme.typography.bodyLarge.tabularNums,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Icon(
                imageVector = if (open) AppIcons.ChevronUp else AppIcons.ChevronDown,
                // Resolved one line above the semantics block below could not read it — a
                // semantics lambda cannot call stringResource.
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp).size(20.dp),
            )
        }
    }
    AnimatedVisibility(visible = open) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            section.Detail(report = report, recap = recap)
            OpenPageRow(label = stringResource(section.open), onClick = onOpen)
        }
    }
}

/** The way out of the conversation, inside the disclosure it belongs to. `DestinationRow`'s job
 * one card over, and deliberately not that component: this one is a section's own footer rather
 * than a turn's, and it carries no icon because four of them stacked would read as a menu. */
@Composable
private fun OpenPageRow(label: String, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
    ) {
        Row(
            modifier = Modifier.heightIn(min = 48.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Icon(
                imageVector = AppIcons.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** The section's own name. `@StringRes val` and never `const`, the app's rule — `const` inlines
 * the placeholder 0 and crashes at the call site rather than failing to compile. */
private val ReportSection.label: Int
    @StringRes get() = when (this) {
        ReportSection.Nutrition -> R.string.coach_report_nutrition
        ReportSection.Steps -> R.string.coach_report_steps
        ReportSection.Training -> R.string.coach_report_training
        ReportSection.Weight -> R.string.coach_report_weight
    }

/** What the row under the detail says. Each one names its destination, for the reason the diary
 * door names the meal it wrote to: a link that says "open" is a direction, not a link. */
private val ReportSection.open: Int
    @StringRes get() = when (this) {
        ReportSection.Nutrition -> R.string.coach_report_open_nutrition
        ReportSection.Steps -> R.string.coach_report_open_steps
        ReportSection.Training -> R.string.coach_report_open_training
        ReportSection.Weight -> R.string.coach_report_open_weight
    }

/**
 * The one figure that survives the section being collapsed, or null where the window holds none.
 *
 * A composable because every one of them resolves a string — *composables resolve, ViewModels
 * name*, and there is no ViewModel between `Recap` and this card at all.
 */
@Composable
private fun ReportSection.headline(report: Report, recap: Recap): String? = when (this) {
    ReportSection.Nutrition -> recap.averages.takeIf { it.daysLogged > 0 }
        ?.let { stringResource(R.string.coach_report_kcal_avg, it.calories) }
    ReportSection.Steps -> recap.steps.averageSteps
        ?.let { stringResource(R.string.coach_report_steps_avg, formatSteps(it)) }
    ReportSection.Training -> recap.workouts.takeIf { it > 0 }
        ?.let { stringResource(R.string.coach_report_workouts, it) }
    // The arc, not the seven-day trend: this card is headed with a window and the figure under
    // that heading has to be the window's own. `weightArcKg` is null on fewer than two weigh-ins,
    // which is an arc nobody measured rather than a zero.
    ReportSection.Weight -> recap.weightArcKg?.let { arc ->
        val shown = formatOneDecimal(abs(arc).kgToDisplayUnit(report.unit))
        val unit = report.unit.weightUnitLabel()
        if (arc < 0) stringResource(R.string.coach_report_weight_down, shown, unit)
        else stringResource(R.string.coach_report_weight_up, shown, unit)
    }
}

/** The detail behind the disclosure, chart included where the series is a daily count. */
@Composable
private fun ReportSection.Detail(report: Report, recap: Recap) {
    val today = report.calories.lastOrNull()?.dateEpochDay
        ?: report.steps.lastOrNull()?.dateEpochDay
    when (this) {
        ReportSection.Nutrition -> {
            if (today != null) {
                DayBarChart(
                    bars = report.calories.map { DayBar(it.dateEpochDay, it.calories) },
                    fromEpochDay = today - (report.days - 1),
                    toEpochDay = today,
                    // The target as the dashed line, which is the only thing a calorie bar is
                    // judged against — and the same figure the Nutrition tab draws it at.
                    goalValue = recap.targets?.calories,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            Fact(R.string.coach_report_protein, "${recap.averages.proteinG} g")
            Fact(R.string.coach_report_carbs, "${recap.averages.carbsG} g")
            Fact(R.string.coach_report_fat, "${recap.averages.fatG} g")
            recap.bestDay?.let {
                Fact(R.string.coach_report_best_day, stringResource(R.string.coach_report_kcal, it.calories))
            }
        }

        ReportSection.Steps -> {
            if (today != null) {
                DayBarChart(
                    bars = report.steps.map { DayBar(it.dateEpochDay, it.steps) },
                    fromEpochDay = today - (report.days - 1),
                    toEpochDay = today,
                    minAxisValue = report.stepGoal,
                    goalValue = report.stepGoal,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            recap.steps.bestSteps?.let { Fact(R.string.coach_report_best_steps, formatSteps(it)) }
            Fact(
                R.string.coach_report_goal_days,
                stringResource(
                    R.string.coach_report_of_days,
                    recap.steps.daysHitGoal,
                    recap.steps.days,
                ),
            )
        }

        ReportSection.Training -> {
            Fact(R.string.coach_report_sets, "${recap.strength.sets}")
            Fact(R.string.coach_report_burned, stringResource(R.string.coach_report_kcal, recap.burnedKcal))
            recap.topLift?.let { Fact(R.string.coach_report_top_lift, it.exerciseName) }
        }

        ReportSection.Weight -> {
            val unit = report.unit.weightUnitLabel()
            recap.startWeightKg?.let {
                Fact(
                    R.string.coach_report_start,
                    stringResource(R.string.coach_report_weight_value, formatOneDecimal(it.kgToDisplayUnit(report.unit)), unit),
                )
            }
            recap.endWeightKg?.let {
                Fact(
                    R.string.coach_report_end,
                    stringResource(R.string.coach_report_weight_value, formatOneDecimal(it.kgToDisplayUnit(report.unit)), unit),
                )
            }
            // The seven-day trend beside the window's arc, labelled as the seven-day figure it is
            // — `trendVsSevenDaysAgo` anchors to the latest entry and does not move with `days`.
            recap.weightTrend?.takeIf { it.hasPrior }?.let {
                Fact(
                    R.string.coach_report_seven_day,
                    stringResource(R.string.coach_report_weight_value, formatOneDecimal(it.deltaKg.kgToDisplayUnit(report.unit)), unit),
                )
            }
        }
    }
}

/** A label and its figure. One screen reader utterance rather than two, because "Protein" and
 * "141 g" read apart are two rows of nothing. */
@Composable
private fun Fact(@StringRes label: Int, value: String) {
    val spoken = stringResource(R.string.coach_report_fact, stringResource(label), value)
    Row(
        modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = spoken },
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.tabularNums,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private const val PREVIEW_TODAY = 20_000L

private val PREVIEW_REPORT = Report(
    days = 30,
    recap = Recap(
        days = 30,
        daysLogged = 24,
        averages = NutritionAverages(1910, 141, 196, 68, daysLogged = 21),
        targets = DailyTargets(calories = 2000, proteinG = 150, carbsG = 200, fatG = 67, floor = 1500),
        weightTrend = WeightTrendDisplay(currentKg = 76.0, deltaKg = -0.6, hasPrior = true),
        moodAverages = null,
        bestDay = BestDay(dateEpochDay = PREVIEW_TODAY - 3, calories = 1985),
        startWeightKg = 77.4,
        endWeightKg = 76.0,
        strength = StrengthTotals(workouts = 9, sets = 74, volumeKg = 41_820.0),
        topLift = LiftRecord(
            exerciseName = "Squat",
            bestWeightKg = 100.0,
            bestReps = 5,
            bestOneRepMaxKg = 116.7,
            dateEpochDay = PREVIEW_TODAY - 5,
            sets = 27,
        ),
        burnedKcal = 8_940,
        workouts = 11,
        steps = StepAverages(averageSteps = 8_420, bestSteps = 16_002, daysHitGoal = 9, days = 26),
    ),
    calories = (0 until 30).map {
        DayNutrition(PREVIEW_TODAY - 29 + it, 1700 + (it * 137) % 700, 130, 190, 62)
    },
    steps = (0 until 30).map { StepDay(PREVIEW_TODAY - 29 + it, 6_000 + (it * 911) % 7_000, 220) },
    stepGoal = 10_000,
    unit = UnitSystem.Metric,
)

private val PREVIEW_REPORTS = mapOf(
    7 to PREVIEW_REPORT.copy(days = 7),
    30 to PREVIEW_REPORT,
)

/** Collapsed, which is how every report opens. */
@PreviewLightDark
@Composable
private fun ReportCardPreview() {
    AppTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                ReportCard(reports = PREVIEW_REPORTS, drafted = 30, onOpenSection = {})
            }
        }
    }
}

/**
 * A section open — the chart, its facts and the way through to the page.
 *
 * The row rather than the whole card, because the disclosure is the card's own `rememberSaveable`
 * and a preview has no way in. This is the half worth looking at anyway: the chart's height
 * against the facts under it is what decides whether an open section still leaves the
 * conversation visible above the input bar.
 */
@PreviewLightDark
@Composable
private fun ReportRowOpenPreview() {
    val report = PREVIEW_REPORT
    val recap = report.recap!!
    AppTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                ReportRow(
                    section = ReportSection.Nutrition,
                    report = report,
                    recap = recap,
                    open = true,
                    onToggle = {},
                    onOpen = {},
                )
                // Beneath it, one that carries no chart — the split is the thing to check.
                ReportRow(
                    section = ReportSection.Weight,
                    report = report,
                    recap = recap,
                    open = true,
                    onToggle = {},
                    onOpen = {},
                )
            }
        }
    }
}

/** A window with nothing in it: the line, and no sections at all. */
@PreviewLightDark
@Composable
private fun ReportCardEmptyPreview() {
    AppTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                ReportCard(
                    reports = mapOf(7 to PREVIEW_REPORT.copy(days = 7, recap = null)),
                    drafted = 7,
                    onOpenSection = {},
                )
            }
        }
    }
}
