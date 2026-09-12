package ph.mart.healthapp.feature.progress.ui.progress

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ph.mart.healthapp.core.data.food.DayNutrition
import ph.mart.healthapp.core.data.health.SleepNight
import ph.mart.healthapp.core.data.profile.DailyTargets
import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.core.data.profile.TrendDirection
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.progress.MeasurementEntry
import ph.mart.healthapp.core.data.progress.MeasurementPart
import ph.mart.healthapp.core.data.progress.ProgressPhoto
import ph.mart.healthapp.core.data.progress.WeightEntry

private const val TODAY = 20_000L

private val TARGETS = DailyTargets(calories = 2000, proteinG = 150, carbsG = 200, fatG = 67, floor = 1500)

private fun summaryFor(subject: Subject, state: ProgressUiState) = summarize(subject, state, TODAY)

class SubjectSummaryTest {

    /** The whole of "nothing tracked": a null value is what draws the dashed card and the empty
     * page, so every subject has to reach it on an empty state rather than reporting a zero. */
    @Test
    fun `every subject is untracked on an empty state`() {
        val summaries = summarizeAll(ProgressUiState(), TODAY)
        assertEquals(Subject.entries.size, summaries.size)
        Subject.entries.forEach { subject ->
            assertFalse(subject.name, summaries.getValue(subject).tracked)
        }
    }

    @Test
    fun `weight reports the latest reading and a goal-relative trend`() {
        val summary = summaryFor(
            Subject.Weight,
            ProgressUiState(
                weightEntries = listOf(
                    WeightEntry(TODAY - 14, 84.0),
                    WeightEntry(TODAY - 7, 83.4),
                    WeightEntry(TODAY, 82.7),
                ),
                goal = Goal.Lose,
            ),
        )
        assertEquals("82.7", summary.value)
        assertEquals("kg", summary.unit)
        assertEquals(TrendDirection.OnTrack, summary.trend)
        assertEquals(TrendArrow.Down, summary.arrow)
        assertTrue(summary.footnote, summary.footnote.contains("on track"))
    }

    /** The same fall of 0.7 kg against a Build goal. Colour is never the thing that changes on its
     * own — the words move with it. */
    @Test
    fun `the same drop reads off track for a build goal`() {
        val entries = listOf(WeightEntry(TODAY - 7, 83.4), WeightEntry(TODAY, 82.7))
        val summary = summaryFor(Subject.Weight, ProgressUiState(weightEntries = entries, goal = Goal.Build))
        assertEquals(TrendDirection.OffTrack, summary.trend)
        assertTrue(summary.footnote, summary.footnote.contains("off track"))
    }

    /** One weigh-in is not a trend, and inventing the other end would be a delta nobody measured. */
    @Test
    fun `a single weigh-in is tracked but has no arrow`() {
        val summary = summaryFor(Subject.Weight, ProgressUiState(weightEntries = listOf(WeightEntry(TODAY, 82.7))))
        assertTrue(summary.tracked)
        assertEquals(null, summary.arrow)
        assertEquals(TrendDirection.Neutral, summary.trend)
    }

    /** Averaged over logged days only — averaging the zero-filled gaps in would report a number
     * the user never ate. */
    @Test
    fun `nutrition averages logged days and names the gap to target`() {
        val days = listOf(1800, 0, 1600).mapIndexed { index, kcal ->
            DayNutrition(TODAY - 2 + index, kcal, kcal / 16, kcal / 10, kcal / 30)
        }
        val summary = summaryFor(Subject.Nutrition, ProgressUiState(dailyNutrition = days, targets = TARGETS))
        assertEquals("1700", summary.value)
        assertEquals("300 kcal under target", summary.footnote)
    }

    /** The gap still has to be reported when the day was overshot, and in the same shape. */
    @Test
    fun `nutrition says over target when it is`() {
        val days = listOf(DayNutrition(TODAY, 2400, 150, 240, 80))
        val summary = summaryFor(Subject.Nutrition, ProgressUiState(dailyNutrition = days, targets = TARGETS))
        assertEquals("400 kcal over target", summary.footnote)
    }

    /** A zero-filled day is a day that happened and held nothing; it must reach the preview as a
     * zero rather than being dropped, or the strip would close the gap. */
    @Test
    fun `nutrition keeps a zero day in its preview`() {
        val days = listOf(1800, 0, 1600).mapIndexed { index, kcal ->
            DayNutrition(TODAY - 2 + index, kcal, kcal / 16, kcal / 10, kcal / 30)
        }
        val preview = summaryFor(Subject.Nutrition, ProgressUiState(dailyNutrition = days)).preview
        assertEquals(SubjectPreview.Bars(listOf(1800, 0, 1600)), preview)
    }

    @Test
    fun `photos count shots and date the newest`() {
        val summary = summaryFor(
            Subject.Photos,
            ProgressUiState(
                photos = listOf(
                    ProgressPhoto(id = 1, dateEpochDay = TODAY - 30, filePath = "a"),
                    ProgressPhoto(id = 2, dateEpochDay = TODAY - 3, filePath = "b"),
                ),
            ),
        )
        assertEquals("2", summary.value)
        assertEquals("shots", summary.unit)
        assertEquals("Last one 3 days ago", summary.footnote)
    }

    /** The shots carry their own weights, so the card reports the run rather than only its date —
     * the same oldest-to-newest reading the comparison overlay gives a hand-picked pair. */
    @Test
    fun `photos report the weight change across the run`() {
        val summary = summaryFor(
            Subject.Photos,
            ProgressUiState(
                photos = listOf(
                    ProgressPhoto(id = 1, dateEpochDay = TODAY - 95, filePath = "a", weightKg = 79.0),
                    ProgressPhoto(id = 2, dateEpochDay = TODAY - 3, filePath = "b", weightKg = 76.9),
                ),
                goal = Goal.Lose,
            ),
        )
        assertEquals("2.1 kg over 92 days · last one 3 days ago", summary.footnote)
        assertEquals(TrendArrow.Down, summary.arrow)
        assertEquals(TrendDirection.OnTrack, summary.trend)
    }

    /** The same 2.1 kg lost against a Build goal. The colour is `goalRelativeTrend`'s call, never
     * the sign of the delta — losing weight is the wrong way for this reader. */
    @Test
    fun `the same photo run reads off track for a build goal`() {
        val summary = summaryFor(
            Subject.Photos,
            ProgressUiState(
                photos = listOf(
                    ProgressPhoto(id = 1, dateEpochDay = TODAY - 95, filePath = "a", weightKg = 79.0),
                    ProgressPhoto(id = 2, dateEpochDay = TODAY - 3, filePath = "b", weightKg = 76.9),
                ),
                goal = Goal.Build,
            ),
        )
        assertEquals(TrendDirection.OffTrack, summary.trend)
        assertEquals(TrendArrow.Down, summary.arrow)
    }

    /** One weighed shot is not a run: a delta needs two ends, and the card says what it always
     * said rather than reporting a change over no time at all. */
    @Test
    fun `one weighed photo keeps the date-only footnote`() {
        val summary = summaryFor(
            Subject.Photos,
            ProgressUiState(
                photos = listOf(
                    ProgressPhoto(id = 1, dateEpochDay = TODAY - 30, filePath = "a"),
                    ProgressPhoto(id = 2, dateEpochDay = TODAY - 1, filePath = "b", weightKg = 76.9),
                ),
            ),
        )
        assertEquals("Last one yesterday", summary.footnote)
        assertEquals(null, summary.arrow)
        assertEquals(TrendDirection.Neutral, summary.trend)
    }

    /** The part measured most recently leads, because that is the one being worked on. */
    @Test
    fun `measurements lead with the most recently measured part`() {
        val summary = summaryFor(
            Subject.Measurements,
            ProgressUiState(
                measurements = mapOf(
                    MeasurementPart.Chest to listOf(MeasurementEntry(MeasurementPart.Chest, TODAY - 20, 101.0)),
                    MeasurementPart.Waist to listOf(
                        MeasurementEntry(MeasurementPart.Waist, TODAY - 10, 89.5),
                        MeasurementEntry(MeasurementPart.Waist, TODAY, 88.0),
                    ),
                ),
            ),
        )
        assertEquals("88", summary.value)
        assertEquals("cm waist", summary.unit)
        assertEquals(TrendDirection.OnTrack, summary.trend)
        assertTrue(summary.footnote, summary.footnote.contains("2 parts"))
    }

    /** Body fat is stored as a percentage in the same column the tape readings use, so the card
     * has to ask the part rather than the unit toggle: converted, 18.5% would read as 7.3 in. */
    @Test
    fun `body fat leads as a percentage and never converts`() {
        val measurements = mapOf(
            MeasurementPart.Waist to listOf(MeasurementEntry(MeasurementPart.Waist, TODAY - 10, 89.5)),
            MeasurementPart.BodyFat to listOf(
                MeasurementEntry(MeasurementPart.BodyFat, TODAY - 5, 19.5),
                MeasurementEntry(MeasurementPart.BodyFat, TODAY, 18.5),
            ),
        )
        listOf(UnitSystem.Metric, UnitSystem.Imperial).forEach { unit ->
            val summary = summaryFor(
                Subject.Measurements,
                ProgressUiState(measurements = measurements, preferredUnit = unit),
            )
            assertEquals(unit.name, "18.5", summary.value)
            assertEquals(unit.name, "% body fat", summary.unit)
            assertEquals(unit.name, TrendDirection.OnTrack, summary.trend)
            assertTrue(summary.footnote, summary.footnote.startsWith("1 % · "))
        }
    }

    /** A part with only one reading has no delta, and a false 0.0 would be worse than none. */
    @Test
    fun `a single measurement reports no delta`() {
        val summary = summaryFor(
            Subject.Measurements,
            ProgressUiState(
                measurements = mapOf(
                    MeasurementPart.Waist to listOf(MeasurementEntry(MeasurementPart.Waist, TODAY, 88.0)),
                ),
            ),
        )
        assertEquals(null, summary.arrow)
        assertEquals("1 part", summary.footnote)
    }

    @Test
    fun `sleep averages its nights`() {
        val nights = listOf(420, 480).mapIndexed { index, minutes -> SleepNight(TODAY - 1 + index, minutes) }
        val summary = summaryFor(Subject.Sleep, ProgressUiState(sleepNights = nights))
        assertEquals("7h 30m", summary.value)
        assertEquals(SubjectPreview.Bars(listOf(420, 480)), summary.preview)
    }

    /** Badges is the one subject with no group — it is a row under the grids, not a card in one. */
    @Test
    fun `badges is the only ungrouped subject and every group holds its own`() {
        assertEquals(listOf(Subject.Badges), Subject.entries.filter { it.group == null })
        val grouped = SubjectGroup.entries.flatMap { subjectsIn(it) }
        assertEquals(Subject.entries.size - 1, grouped.size)
        assertEquals(grouped.size, grouped.distinct().size)
    }

    /** The row and the page behind it read one fold, so they can never disagree about how many
     * badges are lit. */
    @Test
    fun `the badge tally counts every family`() {
        val tally = badgeTally(ProgressUiState(activeDays = setOf(TODAY)), TODAY)
        assertTrue(tally.total > 0)
        assertTrue(tally.families > 0)
        assertTrue(tally.earned <= tally.total)
    }
}
