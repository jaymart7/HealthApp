# Filipino Translation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship Filipino (`fil`) as FitPulse's first translation — every screen, notification, the widget, the watch, and AI replies — with English output unchanged.

**Architecture:** User-visible English still in Kotlin moves behind a `Phrase` value (`:core:data`) that composables resolve with `LocalResources.current`; every module gains a Filipino `strings.xml`; a `checkTranslations` root task holds key/placeholder/plural parity; one `replyLanguageLine()` tells Gemini which language to write in.

**Tech Stack:** Kotlin, Jetpack Compose (BOM 2026.09.00 → `LocalResources`), Android resources, Gradle Kotlin DSL, Firebase AI Logic.

**Spec:** `docs/superpowers/specs/2026-10-04-filipino-translation-design.md`

## Global Constraints

- English output stays byte-identical, except the two deliberate fixes named in Task 2 (`1 nights` → `1 night`).
- No new dependency (no AppCompat, no Robolectric, no MockK).
- Composables resolve; ViewModels name. No `Context` reaches a ViewModel.
- Keys are `<module>_<screen>_<thing>`; `:core:data` keys start `data_`.
- A resource id is never a `const val`.
- Persisted or compared values stay English: enum `name`s, portion units, `COMMON_FOODS`, `QUICK_ADD_NAME`, prompts, `@SerialName`s.
- Unit symbols (kg, lb, cm, in, kcal, g, mg, ml, bpm, mmHg, h, m) are not copy.
- Placeholders in this app are only `%s`, `%d`, `%n$s`, `%n$d`, `%.nf`, `%n$.nf`, `%%`. Strings with 2+ args are positional.
- Commit on `main` after each task (CLAUDE.md working agreement); say what was and wasn't verified.

## Deviations from the spec (found while planning)

1. `Phrase` is a sealed interface — `Res` / `Plural` / `Raw` — not one data class: `SubjectSummary.unit` mixes copy ("shots") with unit symbols ("kg") and `ageBandFor` returns a formatted month, and both need a value that is already final.
2. `EarnedCalories.kt` (four lines: Home's ring, the save snackbar, the insight's workout rule) is English the gate never saw. It converts with the rest.
3. `MeasurementPart` nouns ("cm waist") come from new lowercase resources in `:feature:progress`, not the capitalized `label`.
4. The Filipino resource qualifier is decided on the emulator in Task 3: every Google library ships `values-tl`; the plan uses `values-fil` if a `fil` app locale resolves it, else `values-tl`.
5. The AI language line must leave **portion units** and **saved-food names** in English: `preferMyFoods` matches names exactly and `portionStep` switches on `"g"`/`"cup"`.

---

### Task 1: `Phrase`, and `:core:data`'s English moves behind it

**Files:**
- Create: `core/data/src/main/java/ph/mart/healthapp/core/data/Phrase.kt`
- Modify: `core/data/src/main/java/ph/mart/healthapp/core/data/insight/Insight.kt:113-150`
- Modify: `core/data/src/main/java/ph/mart/healthapp/core/data/exercise/EarnedCalories.kt:20-66`
- Modify: `core/data/src/main/java/ph/mart/healthapp/core/data/exercise/Strength.kt:183-224`
- Create: `core/data/src/main/java/ph/mart/healthapp/core/data/progress/GoalProjectionLine.kt` (moved from `:core:designsystem`)
- Delete: `core/designsystem/src/main/java/ph/mart/healthapp/core/designsystem/component/GoalProjectionLine.kt`
- Move test: `core/designsystem/src/test/.../GoalProjectionLineTest.kt` → `core/data/src/test/java/ph/mart/healthapp/core/data/progress/GoalProjectionLineTest.kt`
- Modify: `core/data/src/main/res/values/strings.xml` (append)
- Modify tests: `InsightTest.kt`, `EarnedCaloriesTest.kt`, `StrengthTest.kt`
- Modify callers: `feature/home/.../components/HomeCards.kt:113`, `feature/home/.../components/CalorieRingCard.kt:136`, `feature/home/.../components/WeightMetricCard.kt:89`, `feature/progress/.../shared/components/RecapCard.kt:180`, `feature/coach/.../CoachData.kt:282` (+ `CoachViewModel.kt:254`, `components/CoachNotice.kt`), `app/.../ui/AppScaffold.kt:391,402`, `feature/training/.../components/StrengthSetList.kt:90`, `feature/training/.../components/StrengthSetEditor.kt:71`, `feature/progress/.../strength/components/LiftRecordRow.kt:57`, `feature/food/.../diary/components/ExerciseSection.kt:150`, debug fakes `core/data/src/debug/.../fake/FakeAiRepositories.kt:83`, `FakeCoachRepository.kt:400`

**Interfaces:**
- Produces:
  ```kotlin
  sealed interface Phrase { data class Res(@StringRes val id: Int, val args: List<Any> = emptyList()); data class Plural(@PluralsRes val id: Int, val quantity: Int, val args: List<Any> = emptyList()); data class Raw(val text: String) }
  fun phrase(@StringRes id: Int, vararg args: Any): Phrase
  fun plural(@PluralsRes id: Int, quantity: Int, vararg args: Any): Phrase
  fun Phrase.resolve(res: Resources): String
  fun insightFor(totals, targets, trend, burnedKcal = 0): Phrase?      // and insightFor(request): Phrase?
  fun earnedFood(kcal: Int): Phrase?                                    // was earnedFoodPhrase(): String?
  fun earnedSavedLine(kcal: Int): Phrase; fun earnedRingLine(kcal: Int): Phrase; fun earnedInsightLine(kcal: Int): Phrase
  fun StrengthSet.loadLabel(unit: UnitSystem): Phrase
  fun List<StrengthSet>.summaryLabel(unit: UnitSystem): Phrase?        // null when empty (was "")
  fun LiftPerformance.label(unit: UnitSystem): Phrase
  fun goalProjectionLine(goalWeightLabel: String, targetDateLabel: String?, reached: Boolean, windowDays: Long): Phrase  // package core.data.progress
  ```

- [ ] **Step 1: Create `Phrase.kt`**

```kotlin
package ph.mart.healthapp.core.data

import android.content.res.Resources
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes

/**
 * Words a pure function has chosen and a composable will spell — the split that lets a branch be
 * tested on the JVM while its wording lives in `strings.xml`.
 *
 * Composables resolve; ViewModels name. A function that used to return an English sentence returns
 * one of these, its test asserts *which* phrase with *which* figures, and the screen resolves it
 * with `LocalResources.current`. An argument that is itself a [Phrase] is resolved first, which is
 * how "Last: 60 kg × 8 · 3 sets" nests a load inside a plural inside a sentence.
 *
 * [Raw] is for text that is already final — a unit symbol, a formatted date, a figure — sitting
 * where a sentence could also sit.
 */
sealed interface Phrase {
    data class Res(@StringRes val id: Int, val args: List<Any> = emptyList()) : Phrase
    data class Plural(@PluralsRes val id: Int, val quantity: Int, val args: List<Any> = emptyList()) : Phrase
    data class Raw(val text: String) : Phrase
}

fun phrase(@StringRes id: Int, vararg args: Any): Phrase = Phrase.Res(id, args.toList())

fun plural(@PluralsRes id: Int, quantity: Int, vararg args: Any): Phrase = Phrase.Plural(id, quantity, args.toList())

/** No-argument resources are read unformatted: `"% taken"` is a literal, and running it through
 * `String.format` would throw. */
fun Phrase.resolve(res: Resources): String = when (this) {
    is Phrase.Res -> if (args.isEmpty()) res.getString(id) else res.getString(id, *args.resolvedIn(res))
    is Phrase.Plural ->
        if (args.isEmpty()) res.getQuantityString(id, quantity) else res.getQuantityString(id, quantity, *args.resolvedIn(res))
    is Phrase.Raw -> text
}

private fun List<Any>.resolvedIn(res: Resources): Array<Any> =
    map { if (it is Phrase) it.resolve(res) else it }.toTypedArray()
```

- [ ] **Step 2: Append the `:core:data` strings** (English exactly as the Kotlin wrote it)

```xml
    <!-- insightFor() — Home's rule-based line and the coach's fallback. -->
    <string name="data_insight_over_budget">You\'re %1$d kcal over today\'s budget.</string>
    <string name="data_insight_protein_short">You\'re %1$dg short on protein today.</string>
    <string name="data_insight_weight_trend">%1$s kg over the last week — keep it steady.</string>

    <!-- EarnedCalories — what a credit buys, in whole portions. -->
    <string name="data_earned_food_burger">a burger and fries</string>
    <string name="data_earned_food_chicken">a chicken breast with rice</string>
    <string name="data_earned_food_sandwich">a peanut-butter sandwich</string>
    <string name="data_earned_food_yoghurt">yoghurt and berries</string>
    <string name="data_earned_food_banana">a banana</string>
    <string name="data_earned_food_apple">an apple</string>
    <string name="data_earned_saved">Nice work — +%1$d kcal on today\'s budget.</string>
    <string name="data_earned_saved_about">Nice work — +%1$d kcal on today\'s budget, about %2$s.</string>
    <string name="data_earned_ring">+%1$d kcal earned today</string>
    <string name="data_earned_ring_about">+%1$d kcal earned — about %2$s</string>
    <string name="data_earned_insight">Today\'s activity bought you %1$d kcal more than a rest day.</string>
    <string name="data_earned_insight_about">Today\'s activity bought you %1$d kcal more than a rest day — about %2$s.</string>

    <!-- Strength labels. -->
    <string name="data_strength_bodyweight">Bodyweight × %1$d</string>
    <plurals name="data_strength_exercises">
        <item quantity="one">%d exercise</item>
        <item quantity="other">%d exercises</item>
    </plurals>
    <plurals name="data_strength_sets">
        <item quantity="one">%d set</item>
        <item quantity="other">%d sets</item>
    </plurals>
    <string name="data_strength_summary">%1$s · %2$s</string>
    <string name="data_strength_summary_volume">%1$s · %2$s · %3$s</string>
    <string name="data_strength_last">Last: %1$s · %2$s</string>

    <!-- goalProjectionLine() — Home's weight card, the projection card and the recap. -->
    <string name="data_projection_reached">You\'re at your goal weight.</string>
    <string name="data_projection_date">On the last %1$d days\' trend, %2$s around %3$s.</string>
    <string name="data_projection_none">No date to project at this pace.</string>
```

- [ ] **Step 3: Convert the tests first (they fail to compile until Step 4)**

`InsightTest` — every `insightFor` assertion becomes a `Phrase`; `sanitizeInsight` tests are untouched:

```kotlin
assertEquals(phrase(R.string.data_insight_over_budget, 200), insightFor(DiaryTotals(2200, 150, 200, 67), TARGETS, flatTrend))
assertEquals(phrase(R.string.data_insight_protein_short, 70), insightFor(DiaryTotals(1000, 80, 100, 30), TARGETS, flatTrend))
assertEquals(phrase(R.string.data_insight_weight_trend, "-0.6"), insightFor(DiaryTotals(1000, 140, 100, 30), TARGETS, WeightTrendDisplay(76.0, -0.6, true)))
assertEquals(phrase(R.string.data_insight_over_budget, 100), insightFor(DiaryTotals(2500, 150, 200, 67), TARGETS, flatTrend, burnedKcal = 400))
assertEquals(earnedInsightLine(400), insightFor(DiaryTotals(2300, 150, 200, 67), TARGETS, flatTrend, burnedKcal = 400))
assertEquals(earnedInsightLine(220), insightFor(short, TARGETS, flatTrend, burnedKcal = 220))
```

`EarnedCaloriesTest`:

```kotlin
assertEquals(phrase(R.string.data_earned_food_sandwich), earnedFood(320))
assertEquals(phrase(R.string.data_earned_ring_about, 320, phrase(R.string.data_earned_food_sandwich)), earnedRingLine(320))
assertEquals(phrase(R.string.data_earned_insight_about, 320, phrase(R.string.data_earned_food_sandwich)), earnedInsightLine(320))
// a line with no phrase to offer still reads as a sentence
assertEquals(phrase(R.string.data_earned_saved, 10), earnedSavedLine(10))
assertEquals(phrase(R.string.data_earned_ring, 10), earnedRingLine(10))
assertEquals(phrase(R.string.data_earned_insight, 10), earnedInsightLine(10))
```
(and each existing `earnedFoodPhrase(n)` threshold assertion becomes `earnedFood(n)` against the matching `data_earned_food_*`).

`StrengthTest` (lines 176-203):

```kotlin
assertEquals(Phrase.Raw("60 kg × 8"), StrengthSet("Squat", 8, 60.0).loadLabel(UnitSystem.Metric))
assertEquals(Phrase.Raw("62.5 kg × 5"), StrengthSet("Squat", 5, 62.5).loadLabel(UnitSystem.Metric))
assertEquals(phrase(R.string.data_strength_bodyweight, 20), StrengthSet("Push-up", 20, 0.0).loadLabel(UnitSystem.Metric))
assertEquals(
    phrase(R.string.data_strength_summary_volume, plural(R.plurals.data_strength_exercises, 2, 2), plural(R.plurals.data_strength_sets, 3, 3), "1,280 kg"),
    sets.summaryLabel(UnitSystem.Metric),
)
assertEquals(
    phrase(R.string.data_strength_summary, plural(R.plurals.data_strength_exercises, 1, 1), plural(R.plurals.data_strength_sets, 1, 1)),
    listOf(StrengthSet("Push-up", 20, 0.0)).summaryLabel(UnitSystem.Metric),
)
assertNull(emptyList<StrengthSet>().summaryLabel(UnitSystem.Metric))
assertEquals(
    phrase(R.string.data_strength_last, Phrase.Raw("60 kg × 8"), plural(R.plurals.data_strength_sets, 3, 3)),
    performance.label(UnitSystem.Metric),
)
```

`GoalProjectionLineTest` (moved into `core.data.progress`):

```kotlin
assertEquals(phrase(R.string.data_projection_reached), goalProjectionLine("72 kg", "Aug 1, 2025", reached = true, windowDays = 30))
assertEquals(phrase(R.string.data_projection_date, 30L, "72 kg", "Aug 1, 2025"), goalProjectionLine("72 kg", "Aug 1, 2025", reached = false, windowDays = 30))
assertEquals(phrase(R.string.data_projection_none), goalProjectionLine("159 lb", null, reached = false, windowDays = 30))
```

- [ ] **Step 4: Convert the functions**

`Insight.kt` — return type `Phrase?`, each branch:
```kotlin
totals.calories > budget -> phrase(R.string.data_insight_over_budget, totals.calories - budget)
burnedKcal >= EARNED_MIN_KCAL -> earnedInsightLine(burnedKcal)
targets.proteinG > 0 && totals.calories > 0 && totals.proteinG < targets.proteinG * 0.6 ->
    phrase(R.string.data_insight_protein_short, targets.proteinG - totals.proteinG)
trend.hasPrior && abs(trend.deltaKg) >= TREND_ARROW_DEADBAND_KG ->
    phrase(R.string.data_insight_weight_trend, formatDelta(trend.deltaKg))
else -> null
```
Replace the "sentences stay in Kotlin for now" KDoc paragraph with: "Returns a [Phrase]: the branch and its figures are tested here, the wording is `data_insight_*`."

`EarnedCalories.kt`:
```kotlin
fun earnedFood(kcal: Int): Phrase? = when {
    kcal >= 700 -> phrase(R.string.data_earned_food_burger)
    kcal >= 450 -> phrase(R.string.data_earned_food_chicken)
    kcal >= 300 -> phrase(R.string.data_earned_food_sandwich)
    kcal >= 200 -> phrase(R.string.data_earned_food_yoghurt)
    kcal >= 120 -> phrase(R.string.data_earned_food_banana)
    kcal >= EARNED_MIN_KCAL -> phrase(R.string.data_earned_food_apple)
    else -> null
}
fun earnedSavedLine(kcal: Int): Phrase =
    earnedFood(kcal)?.let { phrase(R.string.data_earned_saved_about, kcal, it) } ?: phrase(R.string.data_earned_saved, kcal)
fun earnedRingLine(kcal: Int): Phrase =
    earnedFood(kcal)?.let { phrase(R.string.data_earned_ring_about, kcal, it) } ?: phrase(R.string.data_earned_ring, kcal)
fun earnedInsightLine(kcal: Int): Phrase =
    earnedFood(kcal)?.let { phrase(R.string.data_earned_insight_about, kcal, it) } ?: phrase(R.string.data_earned_insight, kcal)
```

`Strength.kt` — delete the "English below stays in Kotlin" comment block's last two sentences; then:
```kotlin
fun StrengthSet.loadLabel(unit: UnitSystem): Phrase =
    if (weightKg <= 0.0) {
        phrase(R.string.data_strength_bodyweight, reps)
    } else {
        // Figures and a unit symbol only — nothing here a translator touches.
        Phrase.Raw("${formatLoad(weightKg.kgToDisplayUnit(unit))} ${unit.weightUnitLabel()} × $reps")
    }

fun List<StrengthSet>.summaryLabel(unit: UnitSystem): Phrase? {
    if (isEmpty()) return null
    val lifts = distinctBy { it.exerciseName }.size
    val volume = volumeKg()
    val exercises = plural(R.plurals.data_strength_exercises, lifts, lifts)
    val sets = plural(R.plurals.data_strength_sets, size, size)
    return if (volume > 0) {
        phrase(R.string.data_strength_summary_volume, exercises, sets, volumeLabel(volume, unit))
    } else {
        phrase(R.string.data_strength_summary, exercises, sets)
    }
}

fun LiftPerformance.label(unit: UnitSystem): Phrase =
    phrase(R.string.data_strength_last, topSet.loadLabel(unit), plural(R.plurals.data_strength_sets, sets, sets))
```

`progress/GoalProjectionLine.kt` (new; carry the old KDoc over, minus the "Primitives rather than…/Stays in Kotlin" paragraphs, plus one line: "[targetDateLabel] is formatted by the caller — `formatEpochDay` lives in `:core:designsystem`, which this module cannot see."):
```kotlin
fun goalProjectionLine(
    goalWeightLabel: String,
    targetDateLabel: String?,
    reached: Boolean,
    windowDays: Long,
): Phrase = when {
    reached -> phrase(R.string.data_projection_reached)
    targetDateLabel != null -> phrase(R.string.data_projection_date, windowDays, goalWeightLabel, targetDateLabel)
    else -> phrase(R.string.data_projection_none)
}
```

- [ ] **Step 5: Run the `:core:data` tests**

Run: `./gradlew :core:data:testDebugUnitTest`
Expected: PASS (callers in other modules don't compile yet — next step).

- [ ] **Step 6: Update every caller** — each resolves at the composable with `val resources = LocalResources.current` (`androidx.compose.ui.platform.LocalResources`):

- `HomeCards.kt:113`: `?: targets?.let { insightFor(uiState.totals, it, trend, uiState.creditedKcal) }?.resolve(resources)`
- `CalorieRingCard.kt:136`: `text = earnedRingLine(burnedKcal).resolve(LocalResources.current)`
- `WeightMetricCard.kt:89` / `RecapCard.kt:180`: import `core.data.progress.goalProjectionLine`; `targetDateLabel = it.targetEpochDay?.let(::formatEpochDay)`; append `.resolve(LocalResources.current)`
- `CoachData.kt:282`: `data class CoachFailure(val offline: Boolean, val insight: Phrase?, val question: String)`; `CoachViewModel.kt:254` unchanged in shape; `CoachNotice` resolves `failure.insight?.resolve(LocalResources.current)`; its previews pass `Phrase.Raw("You're 88 g short on protein today.")`
- `AppScaffold.kt`: read `val resources = LocalResources.current` in composition; `showSnackbar(earnedSavedLine(creditedKcal).resolve(resources))`; line 402 `earnedSavedLine(creditedKcal).resolve(resources)`
- `StrengthSetList.kt:90`, `LiftRecordRow.kt:57`: `.loadLabel(unit).resolve(LocalResources.current)`
- `StrengthSetEditor.kt:71`: `lastPerformance.label(unit).resolve(LocalResources.current)`
- `ExerciseSection.kt:150`: `entry.sets.summaryLabel(unit)?.resolve(LocalResources.current).orEmpty()`
- Debug fakes: `FakeInsightRepository` and the coach fake take `Resources` (`androidContext().resources` where `debugInsight()`/`debugCoach()` are called from the Koin module) and resolve `insightFor(request)`.

- [ ] **Step 7: Build and test everything**

Run: `./gradlew assembleDebug testDebugUnitTest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 8: Commit** — `Localization: core:data's English moves behind Phrase`

---

### Task 2: Feature-side English moves behind `Phrase`; the gate shrinks and learns templates

**Files:**
- Modify: `feature/home/.../ui/HomeData.kt:186-197`, `components/HomeCards.kt:143`
- Modify: `feature/food/.../diary/components/DiaryDateHeader.kt:41-45,133,180,183`, `CopyDaySheet.kt:60`, `DiarySheets.kt:74`
- Modify: `feature/progress/.../supplement/components/SupplementCatchUpCard.kt:47-51,128`
- Modify: `feature/food/.../history/FoodHistoryData.kt:106-150` (+ `HistoryBand`, `FoodHistoryScreen.kt`, `components/HistoryAgeBand.kt`, `components/HistoryRow.kt`)
- Modify: `feature/progress/.../achievement/components/BadgeGroupCard.kt:96-140`
- Modify: `feature/progress/.../progress/SubjectSummary.kt` (whole fold), `components/SubjectCard.kt:111,154` (+ previews in `SubjectCard.kt`, `GroupSection.kt`)
- Modify: `app/.../reminder/Reminders.kt:35-90`, `ReminderWorker.kt:89-90`
- Modify: `feature/{home,food,progress}/src/main/res/values/strings.xml`, `app/src/main/res/values/strings.xml`
- Modify tests: `HomeDataTest.kt`, `DiaryDateHeaderTest.kt`, `SupplementCatchUpTest.kt`, `FoodHistoryTest.kt`, `AchievementsTest.kt`, `SubjectSummaryTest.kt`
- Modify: `build.gradle.kts:42-52` (`literalExceptions`), `:72-84` (template rule)

**Interfaces:**
- Consumes: `Phrase`, `phrase`, `plural`, `resolve` (Task 1)
- Produces: `@StringRes fun greetingFor(hour: Int): Int`, `@StringRes fun greetingSubFor(hour: Int): Int`, `diaryDateLabel(...): Phrase`, `catchUpDateLabel(...): Phrase`, `ageBandFor(...): Phrase`, `relativeAgeLabel(...): Phrase`, `HistoryBand(label: Phrase, …)`, `captionFor(...): Phrase`, `SubjectSummary(unit: Phrase? = null, footnote: Phrase? = null, …)`, `Reminder(title: @StringRes Int, body: @StringRes Int, …)`

- [ ] **Step 1: Greeting.** Strings (`feature/home`): `home_greeting_morning` "Good morning", `home_greeting_afternoon` "Good afternoon", `home_greeting_evening` "Good evening", `home_greeting_sub_morning` "Ready for breakfast?", `home_greeting_sub_afternoon` "How\'s the day going?", `home_greeting_sub_evening` "Almost there for today." Functions return `R.string.*` with `@StringRes`; `HomeCards.kt:143` wraps both in `stringResource(...)`. Add to `HomeDataTest`:

```kotlin
@Test
fun `the greeting turns at noon and at six`() {
    assertEquals(R.string.home_greeting_morning, greetingFor(11))
    assertEquals(R.string.home_greeting_afternoon, greetingFor(12))
    assertEquals(R.string.home_greeting_evening, greetingFor(18))
    assertEquals(R.string.home_greeting_sub_afternoon, greetingSubFor(17))
}
```

- [ ] **Step 2: The two date labels.** Reuse `food_today`/`food_yesterday`; add `progress_catchup_today` "Today", `progress_catchup_yesterday` "Yesterday".

```kotlin
internal fun diaryDateLabel(epochDay: Long, today: Long): Phrase = when (epochDay) {
    today -> phrase(R.string.food_today)
    today - 1 -> phrase(R.string.food_yesterday)
    else -> Phrase.Raw(formatEpochDay(epochDay))
}
```
(`catchUpDateLabel` identical with the `progress_catchup_*` ids.) Callers resolve: `diaryDateLabel(selectedDate, today).resolve(LocalResources.current)`. Tests: `assertEquals(phrase(R.string.food_today), diaryDateLabel(20_000, 20_000))`, `…food_yesterday…19_999…`, and `Phrase.Raw(formatEpochDay(19_990))` for the absolute case; same three for `catchUpDateLabel`.

- [ ] **Step 3: Food history.** Strings (`feature/food`): `food_history_band_this_week` "This week", `food_history_band_earlier_month` "Earlier this month", plurals `food_history_days_ago` (one "%d day ago" / other "%d days ago"), `food_history_weeks_ago` ("%d week ago"/"%d weeks ago"), `food_history_months_ago` ("%d month ago"/"%d months ago"). `ageBandFor` returns `phrase(...)` / `Phrase.Raw(formatMonthYear(epochDay))`; `relativeAgeLabel` returns `phrase(R.string.food_today)` / `phrase(R.string.food_yesterday)` / `plural(R.plurals.food_history_days_ago, days.toInt(), days)` etc. `HistoryBand.label: Phrase` — band grouping keeps working because `Phrase` is a data class. `HistoryAgeBand(label: String)` keeps its `String` parameter; the screen resolves. Tests: each string assertion becomes the matching `phrase`/`plural`/`Phrase.Raw` — e.g. `assertEquals(listOf(phrase(R.string.food_history_band_this_week), Phrase.Raw("July")), bands.map { it.label })`, `assertEquals(plural(R.plurals.food_history_weeks_ago, 2, 2L), relativeAgeLabel(19_986, today = 20_000))`.

- [ ] **Step 4: Badge captions.** Strings (`feature/progress`):
  - `progress_badge_caption_all` "Every badge earned."
  - `progress_badge_caption_first` "Your first %1$s earns a badge."
  - `progress_badge_caption_fast` "A %1$s fast earns the next badge."
  - `progress_badge_caption_weight` "Reach %1$s %2$s for the next badge."
  - plurals `progress_badge_caption_more_day` (one "%1$d more day to your %2$d-day badge." / other "%1$d more days to your %2$d-day badge."), and the same for `_workout`, `_fast`, `_photo`
  - nouns `progress_badge_noun_day` "day", `_workout` "workout", `_fast` "fast", `_hour` "hour", `_photo` "photo"
  - descriptions `progress_badge_desc_weight` "%1$s %2$s badge", `progress_badge_desc_day` "%1$s-day badge", `_workout` "%1$s-workout badge", `_fast` "%1$s-fast badge", `_hour` "%1$s-hour badge", `_photo` "%1$s-photo badge"

```kotlin
internal fun captionFor(group: BadgeGroup, unit: UnitSystem): Phrase {
    val next = group.next ?: return phrase(R.string.progress_badge_caption_all)
    if (next == 1) return phrase(R.string.progress_badge_caption_first, nounFor(group.family, unit))
    return when (group.family) {
        BadgeFamily.LongestFast -> phrase(R.string.progress_badge_caption_fast, tierLabel(group.family, next, unit))
        BadgeFamily.WeightMoved -> phrase(R.string.progress_badge_caption_weight, tierNumber(group.family, next, unit), unit.weightUnitLabel())
        else -> {
            val remaining = next - group.current
            plural(moreFor(group.family), remaining, remaining, next)
        }
    }
}
private fun nounFor(family: BadgeFamily, unit: UnitSystem): Phrase = when (family) {
    BadgeFamily.Streak, BadgeFamily.DaysLogged -> phrase(R.string.progress_badge_noun_day)
    BadgeFamily.WeightMoved -> Phrase.Raw(unit.weightUnitLabel())
    BadgeFamily.Workouts -> phrase(R.string.progress_badge_noun_workout)
    BadgeFamily.Fasts -> phrase(R.string.progress_badge_noun_fast)
    BadgeFamily.LongestFast -> phrase(R.string.progress_badge_noun_hour)
    BadgeFamily.Photos -> phrase(R.string.progress_badge_noun_photo)
}
@PluralsRes
private fun moreFor(family: BadgeFamily): Int = when (family) {
    BadgeFamily.Workouts -> R.plurals.progress_badge_caption_more_workout
    BadgeFamily.Fasts -> R.plurals.progress_badge_caption_more_fast
    BadgeFamily.Photos -> R.plurals.progress_badge_caption_more_photo
    else -> R.plurals.progress_badge_caption_more_day   // Streak, DaysLogged; the other two returned above
}
private fun descriptionFor(family: BadgeFamily, tier: Int, unit: UnitSystem): Phrase {
    val label = tierNumber(family, tier, unit)
    return when (family) {
        BadgeFamily.WeightMoved -> phrase(R.string.progress_badge_desc_weight, label, unit.weightUnitLabel())
        BadgeFamily.Streak, BadgeFamily.DaysLogged -> phrase(R.string.progress_badge_desc_day, label)
        BadgeFamily.Workouts -> phrase(R.string.progress_badge_desc_workout, label)
        BadgeFamily.Fasts -> phrase(R.string.progress_badge_desc_fast, label)
        BadgeFamily.LongestFast -> phrase(R.string.progress_badge_desc_hour, label)
        BadgeFamily.Photos -> phrase(R.string.progress_badge_desc_photo, label)
    }
}
```
The card resolves both with `LocalResources.current`. `AchievementsTest` caption assertions become e.g. `assertEquals(plural(R.plurals.progress_badge_caption_more_day, 4, 4, 7), captionFor(BadgeGroup(BadgeFamily.Streak, listOf(3, 7, 14), current = 3), UnitSystem.Metric))`.

- [ ] **Step 5: Subject summaries.** `SubjectSummary.unit: Phrase? = null`, `footnote: Phrase? = null` (the `""` default becomes `null`). `trendWord` returns `@StringRes Int`; `daysAgo` returns `Phrase`. Each branch, old → new (strings in `feature/progress`, English verbatim):

| Branch | `unit` | `footnote` |
|---|---|---|
| Weight | `Phrase.Raw(unit.weightUnitLabel())` | `phrase(progress_summary_weight_week, "<fig> <unit>", phrase(trendWord(..)))` "%1$s this week · %2$s" / `phrase(progress_summary_one_reading)` "One reading so far" |
| Photos | `plural(progress_summary_shots, n)` (one "shot" / other "shots") | `phrase(progress_summary_photos_arc, "<fig> <unit>", plural(progress_summary_over_days, days.toInt(), days), ago)` "%1$s over %2$s · last one %3$s" with `over_days` "%d day"/"%d days" / `phrase(progress_summary_last_one, ago)` "Last one %1$s" |
| Measurements | percent → `phrase(progress_summary_unit_body_fat)` "% body fat"; else `phrase(progress_summary_unit_part, unit.lengthUnitLabel(), phrase(partNoun(lead.key)))` "%1$s %2$s", nouns `progress_summary_part_{chest,waist,hips,arms,thighs,body_fat}` lowercase | delta → `phrase(progress_summary_measure_delta, "<fig> <unitLabel>", parts)` "%1$s · %2$s"; else `parts` = `plural(progress_summary_parts, n, n)` "%d part"/"%d parts" |
| Nutrition | `phrase(progress_summary_unit_kcal_avg)` "kcal avg" | `progress_summary_days_logged_of` "%1$d of %2$d days logged" / `progress_summary_kcal_under` "%1$d kcal under target" / `progress_summary_kcal_over` "%1$d kcal over target" / `progress_summary_on_target` "On target" |
| Water | `progress_summary_unit_glasses_avg` "glasses avg" | `progress_summary_water_goal_days` "%1$d of %2$d days hit goal" |
| Fasting | `progress_summary_unit_avg` "avg" | `progress_summary_fast_goals` "%1$d of %2$d goals hit" |
| Supplements | `progress_summary_unit_taken` "% taken" | `plural(progress_summary_days_logged, n, n)` "%d day logged"/"%d days logged" |
| Activity | `progress_summary_unit_steps` "steps" | `progress_summary_steps_goal` "Daily average · %1$d of %2$d hit goal" |
| Strength | `plural(progress_summary_workouts, n)` "workout"/"workouts" | `progress_summary_lifted` "Lifted %1$s" |
| Sleep | `progress_summary_unit_avg` | `plural(progress_summary_watch_nights, n, n)` "From your watch · %d night" / "From your watch · %d nights" — **fixes "1 nights"** |
| Mood | `Phrase.Raw("/ ${MOOD_SCALE.last}")` | `plural(progress_summary_days_logged, n, n)` |
| Cycle | `progress_summary_unit_cycle_day` "day of cycle" | `progress_summary_avg_cycle` "Average cycle %1$d days" / `plural(progress_summary_days_logged, n, n)` |
| Heart | `progress_summary_unit_bpm_avg` "bpm avg" | `progress_summary_lowest_bpm` "Lowest %1$d bpm" / `progress_summary_from_watch` "From your watch" |
| BloodPressure | `progress_summary_unit_mmhg_avg` "mmHg avg" | `plural(progress_summary_readings, n, n)` "%d reading"/"%d readings" |
| Badges | `phrase(progress_summary_badges_of, tally.total)` "of %1$d earned" | `plural(progress_summary_families, n, n)` "%d family"/"%d families" |

`trendWord`: `progress_summary_trend_steady` "steady", `_on_track` "on track", `_off_track` "off track". `daysAgo`: `progress_summary_ago_today` "today", `progress_summary_ago_yesterday` "yesterday", `plural(progress_summary_ago_days, days.toInt(), days)` "%d days ago" (one "%d day ago"). `"<fig> <unit>"` stays one pre-formatted `String` argument, exactly as built today. `SubjectCard` resolves `summary.unit?.resolve(resources)` and `summary.footnote?.resolve(resources).orEmpty()`; previews wrap their literals in `Phrase.Raw`. `SubjectSummaryTest` assertions become `Phrase` equality, e.g. `assertEquals(Phrase.Raw("kg"), summary.unit)`, `assertEquals(phrase(R.string.progress_summary_kcal_under, 300), summary.footnote)`, `assertEquals(phrase(R.string.progress_summary_trend_on_track), (summary.footnote as Phrase.Res).args[1])`, `assertEquals(plural(R.plurals.progress_summary_parts, 1, 1), summary.footnote)`.

- [ ] **Step 6: Reminders.** Add to `app/strings.xml` (`app_reminder_<name>_title` / `_body`, English verbatim from `Reminders.kt:68-86`, apostrophes escaped). `Reminder.title`/`body` become `@StringRes val title: Int` / `@StringRes val body: Int`; entries pass `R.string.app_reminder_breakfast_title, R.string.app_reminder_breakfast_body` etc. `ReminderWorker.kt:89-90`: `context.getString(reminder.title)`, `context.getString(reminder.body)`. Rewrite the KDoc paragraph at `Reminders.kt:35-38`: "[title] and [body] are resources, resolved by [ReminderWorker] with its own Context when it posts."

- [ ] **Step 7: Shrink the gate and add the template rule.** In `build.gradle.kts`:
  - `literalExceptions = listOf("MascotAvatar.kt")`, and its KDoc becomes "Files whose English is a proper name, recorded at its definition…"
  - add to `positional`: `Regex(""""\$(\{[^}]*\}|[\w.]+) (?!kcal\b|bpm\b)[a-z]{3,}"""),  // a template opening a sentence: "$n days ago"`
  - delete the ponytail's "or one starting with a template" clause.
  - Delete each converted file's "Stays in Kotlin" comment.

- [ ] **Step 8: Verify**

Run: `./gradlew assembleDebug testDebugUnitTest checkUiLiterals`
Expected: BUILD SUCCESSFUL. Then `grep -rn "pure-function-with-a-test\|stays in Kotlin for now\|Stays in Kotlin: a pure function" --include=*.kt .` → no hits.

- [ ] **Step 9: Commit** — `Localization: the last English in Kotlin moves to strings.xml`

---

### Task 3: `checkTranslations`, the locale config, the Language row, and the qualifier

**Files:**
- Modify: `build.gradle.kts` (new task after `checkUiLiterals`)
- Modify: `.github/workflows/build.yml:32`
- Create: `app/src/main/res/xml/locales_config.xml`
- Modify: `app/src/main/AndroidManifest.xml:21` (`android:localeConfig`)
- Modify: `core/designsystem/.../icon/AppIcons.kt` (`Language`)
- Modify: `feature/profile/.../settings/components/SettingsNavRows.kt`, `settings/SettingsScreen.kt`, `feature/profile/src/main/res/values/strings.xml`
- Create (probe): `feature/profile/src/main/res/values-fil/strings.xml`

- [ ] **Step 1: Add the task** (after `checkUiLiterals` in `build.gradle.kts`):

```kotlin
/** The language this build ships besides English, as its resource folder names it. */
val translationQualifier = "values-fil"

/**
 * Fails when a translation and its English drift: a key on one side only, a string whose
 * placeholders differ, or a plural missing a category Filipino needs. Placeholders are the sharp
 * one — a `%1$s` the English has and the translation lost is a crash at the call site, not a typo.
 *
 * Only the forms this app uses are placeholders (`%s`, `%d`, `%1$s`, `%.1f`, `%%`), so a literal
 * `"% taken"` is text.
 */
tasks.register("checkTranslations") {
    group = "verification"
    description = "Fails if a translation's keys, placeholders or plurals differ from the English."
    val modules = localizedModules.map { file("$it/src/main/res") }
    val qualifier = translationQualifier
    doLast {
        val placeholder = Regex("""%(\d+\$)?,?(\.\d+)?[sdf]|%%""")
        fun parse(file: File): Map<String, Pair<String, Map<String, String>>> {
            if (!file.exists()) return emptyMap()
            val doc = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
            val out = linkedMapOf<String, Pair<String, Map<String, String>>>()
            val nodes = doc.documentElement.childNodes
            for (i in 0 until nodes.length) {
                val el = nodes.item(i) as? org.w3c.dom.Element ?: continue
                if (el.getAttribute("translatable") == "false") continue
                val name = el.getAttribute("name")
                when (el.tagName) {
                    "string" -> out[name] = "string" to mapOf("" to el.textContent)
                    "plurals" -> {
                        val items = el.getElementsByTagName("item")
                        out[name] = "plurals" to (0 until items.length).associate {
                            val item = items.item(it) as org.w3c.dom.Element
                            item.getAttribute("quantity") to item.textContent
                        }
                    }
                }
            }
            return out
        }
        fun marks(text: String) = placeholder.findAll(text).map { it.value }.sorted().toList()
        val problems = modules.flatMap { res ->
            val english = parse(File(res, "values/strings.xml"))
            if (english.isEmpty()) return@flatMap emptyList()
            val translated = parse(File(res, "$qualifier/strings.xml"))
            val where = res.path
            buildList {
                (english.keys - translated.keys).forEach { add("$where: missing $it") }
                (translated.keys - english.keys).forEach { add("$where: extra $it") }
                english.keys.intersect(translated.keys).forEach { key ->
                    val (kind, en) = english.getValue(key)
                    val (_, tr) = translated.getValue(key)
                    if (kind == "plurals") {
                        listOf("one", "other").filter { it !in tr }.forEach { add("$where: $key lacks quantity=\"$it\"") }
                        tr.values.forEach { if (marks(it) != marks(en.getValue("other"))) add("$where: $key placeholders differ") }
                    } else if (marks(en.getValue("")) != marks(tr.getValue(""))) {
                        add("$where: $key placeholders ${marks(en.getValue(""))} vs ${marks(tr.getValue(""))}")
                    }
                }
            }
        }
        if (problems.isNotEmpty()) throw GradleException("Translation drift:\n" + problems.joinToString("\n"))
    }
}
```

(The plurals check compares each item against `other`'s placeholders. A `one` item that omits the `%d` is legal Android but breaks this rule on purpose: Filipino's `one` covers more than 1.)

- [ ] **Step 2: Run it before any translation exists**

Run: `./gradlew checkTranslations`
Expected: FAIL listing every key as `missing` — the gate works. Do **not** add it to CI until Task 8 lands; record that in the commit message.

- [ ] **Step 3: Locale config + manifest**

```xml
<?xml version="1.0" encoding="utf-8"?>
<!-- The languages FitPulse speaks, for Android 13's per-app language picker. Hand-written rather
     than generated, so a library's own translations can't offer a language the app doesn't. -->
<locale-config xmlns:android="http://schemas.android.com/apk/res/android">
    <locale android:name="en" />
    <locale android:name="fil" />
</locale-config>
```
Manifest `<application … android:localeConfig="@xml/locales_config">`.

- [ ] **Step 4: Language row.** `AppIcons.kt`: `val Language: ImageVector = Icons.Outlined.Language`. Strings: `profile_settings_language_row` "Language", `profile_settings_language_row_sub` "English or Filipino — follows your phone until you choose". `SettingsNavRows.kt`:

```kotlin
/** The way into the app's language. Android owns the picker — the per-app setting on 13 and up,
 * the phone's own language below — so this row only opens it. */
@Composable
internal fun SettingsLanguageSection(onOpenLanguage: () -> Unit, modifier: Modifier = Modifier) {
    SettingsNavRow(
        label = stringResource(R.string.profile_settings_language_row),
        sublabel = stringResource(R.string.profile_settings_language_row_sub),
        icon = AppIcons.Language,
        onClick = onOpenLanguage,
        modifier = modifier,
    )
}
```
`SettingsScreen` adds `onOpenLanguage: () -> Unit` to `SettingsContent` (placed after `SettingsHomeLayoutSection`) and supplies it from the route body, which already holds `context`:

```kotlin
onOpenLanguage = {
    // Guarded for RemindersScreen's reason: a stripped OEM build may not resolve either page.
    runCatching {
        context.startActivity(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Intent(Settings.ACTION_APP_LOCALE_SETTINGS, Uri.fromParts("package", context.packageName, null))
            } else {
                Intent(Settings.ACTION_LOCALE_SETTINGS)
            },
        )
    }
},
```
Preview passes `onOpenLanguage = {}`.

- [ ] **Step 5: Settle the qualifier on the emulator.** Create `feature/profile/src/main/res/values-fil/strings.xml` holding only `profile_settings_language_row` → "Wika" (the probe). Then:

```bash
./gradlew :app:installDebug
adb shell cmd locale set-app-locales ph.mart.healthapp --locales fil
adb shell am start -n ph.mart.healthapp/.MainActivity
```
Open Profile → Settings; screenshot. If the row reads "Wika", the qualifier is `values-fil`. If it reads "Language", rename the folder to `values-tl`, set `translationQualifier = "values-tl"`, reinstall, and confirm "Wika". Record the answer in Task 11's DECISIONS entry.

- [ ] **Step 6: Commit** — `Localization: checkTranslations, the locale config and a Language row` (message notes `checkTranslations` is red until the translations land and is not in CI yet).

---

### Task 4: Glossary + the small modules (`:core:data`, `:core:designsystem`, `:app`, `:wear`)

**Files:**
- Create: `docs/glossary-fil.md`
- Create: `<module>/src/main/res/<qualifier>/strings.xml` for `core/data`, `core/designsystem`, `app`, `wear`

- [ ] **Step 1: Write `docs/glossary-fil.md`** — the term table every later task follows. Register: modern Filipino as Google's Android localizations write it; imperative actions take the `I-` prefix (I-save, I-log, I-edit, I-delete → Burahin), Cancel → Kanselahin, Done → Tapos na, Back → Bumalik, Settings → Mga Setting, Today → Ngayong araw, Yesterday → Kahapon, Breakfast/Lunch/Dinner/Snacks → Almusal/Tanghalian/Hapunan/Meryenda, Water → Tubig, Weight → Timbang, Protein → Protina, Carbs → Carbs, Fat → Taba, Calories → Calories, Goal → Layunin, Streak → Streak, Badge → Badge, Coach → Coach, Progress → Progreso, Food → Pagkain, Exercise → Ehersisyo, Sleep → Tulog, Mood → Mood, Fasting → Pag-aayuno, Supplements → Supplements, Blood pressure → Presyon ng dugo, Heart rate → Tibok ng puso, Steps → Hakbang, Cycle → Cycle, Recipe → Recipe, Barcode → Barcode. Never translated: FitPulse, Health Connect, Google Health, Gemini, Wear OS, the mascot names, unit symbols, `%`-placeholders. Tone: second person "mo/ka", warm, short; keep sentences as short as the English where a label is space-bound.

- [ ] **Step 2: Translate the four modules.** For each, copy `values/strings.xml` to `<qualifier>/strings.xml` and translate every `<string>` and `<plurals>` (both `one` and `other`, each keeping the English `other`'s placeholders), keeping comments only where they guide a translator. Escape `'` as `\'`. Keep XML comments out of the copy otherwise.

- [ ] **Step 3: Verify**

Run: `./gradlew checkTranslations 2>&1 | grep -E "core/data|core/designsystem|/app/|/wear/" ; ./gradlew assembleDebug`
Expected: no lines for these four modules; build passes.

- [ ] **Step 4: Commit** — `Filipino: glossary, core modules, app and watch` (note: not reviewed by a native speaker).

### Task 5: Translate `:feature:home`, `:feature:onboarding`, `:feature:training`, `:feature:coach`

- [ ] **Step 1:** Same procedure as Task 4 Step 2 for these four modules, following `docs/glossary-fil.md`.
- [ ] **Step 2:** `./gradlew checkTranslations 2>&1 | grep -E "feature/(home|onboarding|training|coach)"` → no lines; `./gradlew assembleDebug` passes.
- [ ] **Step 3: Commit** — `Filipino: home, onboarding, training, coach`.

### Task 6: Translate `:feature:food`

- [ ] **Step 1:** Same procedure for `feature/food`.
- [ ] **Step 2:** `./gradlew checkTranslations 2>&1 | grep feature/food` → no lines; build passes.
- [ ] **Step 3: Commit** — `Filipino: food`.

### Task 7: Translate `:feature:profile`

- [ ] **Step 1:** Same procedure for `feature/profile` (the Task 3 probe file is replaced by the full translation).
- [ ] **Step 2:** `./gradlew checkTranslations 2>&1 | grep feature/profile` → no lines; build passes.
- [ ] **Step 3: Commit** — `Filipino: profile`.

### Task 8: Translate `:feature:progress`; the gate joins CI

**Files:** `feature/progress/src/main/res/<qualifier>/strings.xml`, `.github/workflows/build.yml:32`

- [ ] **Step 1:** Same procedure for `feature/progress`.
- [ ] **Step 2:** Run: `./gradlew checkTranslations` → BUILD SUCCESSFUL (every module clean).
- [ ] **Step 3:** CI line becomes `./gradlew assembleDebug testDebugUnitTest checkUiLiterals checkTranslations -Dorg.gradle.jvmargs=…`.
- [ ] **Step 4: Commit** — `Filipino: progress; checkTranslations joins CI`.

---

### Task 9: AI replies follow the app language

**Files:**
- Modify: `core/data/src/main/java/ph/mart/healthapp/core/data/Ai.kt` (append)
- Create: `core/data/src/test/java/ph/mart/healthapp/core/data/ReplyLanguageTest.kt`
- Modify: `insight/InsightRepositoryImpl.kt:41`, `food/QuickLogRepositoryImpl.kt:60,62`, `food/MealIdeaRepositoryImpl.kt:53`, `food/FoodRecognitionRepositoryImpl.kt:82`, `food/MealParseRepositoryImpl.kt:40`, `coach/CoachRepositoryImpl.kt:158,253`

**Interfaces:**
- Produces: `internal fun replyLanguageLine(locale: Locale = Locale.getDefault()): String?`

- [ ] **Step 1: Failing test**

```kotlin
package ph.mart.healthapp.core.data

import java.util.Locale
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReplyLanguageTest {
    @Test
    fun `English needs no line`() {
        assertNull(replyLanguageLine(Locale.US))
        assertNull(replyLanguageLine(Locale.UK))
    }

    @Test
    fun `Filipino names the language and keeps units and saved names English`() {
        val line = replyLanguageLine(Locale.forLanguageTag("fil-PH"))!!
        assertTrue(line, "Filipino" in line)
        assertTrue(line, "portion unit" in line)
        assertTrue(line, "saved food" in line)
    }

    /** `tl` is how older devices and every Google library name the same language. */
    @Test
    fun `Tagalog reads as the same instruction`() {
        assertTrue("Tagalog" in replyLanguageLine(Locale.forLanguageTag("tl"))!!)
    }
}
```

Run: `./gradlew :core:data:testDebugUnitTest --tests '*ReplyLanguageTest*'` → FAIL (unresolved `replyLanguageLine`).

- [ ] **Step 2: Implement** (append to `Ai.kt`):

```kotlin
/**
 * The one line that tells the model which language the user reads, or null for English — the
 * prompts themselves stay English either way, since that is what the model reads best.
 *
 * Read per request, never at model construction: repositories are Koin singletons and outlive a
 * per-app language change, so a line baked into a `systemInstruction` would keep answering in the
 * language the app started in.
 *
 * Portion units and saved-food names are carved out by name because both are *compared*:
 * `portionStep` switches on "g"/"cup"/"serving", and `preferMyFoods` reprices a row only when its
 * name matches a saved food's exactly. A translated unit or name is a silently broken stepper or an
 * estimate where the label's own figures should have been.
 */
internal fun replyLanguageLine(locale: Locale = Locale.getDefault()): String? {
    if (locale.language == "en") return null
    val language = locale.getDisplayLanguage(Locale.ENGLISH)
    return "Write everything the user will read — sentences, questions, food and meal names — in " +
        "$language. Keep every portion unit and the name of any saved food exactly as given, in English."
}
```
(import `java.util.Locale`.)

Run the test → PASS.

- [ ] **Step 3: Wire it in, one line per site** — append when non-null, e.g.:
  - Insight: `content { text(promptFor(request)); replyLanguageLine()?.let { text(it) } }`
  - QuickLog (both): `content { text(prompt); replyLanguageLine()?.let { text(it) } }` / `content { image(…); text(prompt); replyLanguageLine()?.let { text(it) } }`
  - MealIdea, FoodRecognition, MealParse: same shape.
  - Coach `:158`: `content(role = "user") { text(context); replyLanguageLine()?.let { text(it) }; text(question) }`; `:253` (the follow-up turn after a tool read) gets the same line so a multi-round answer cannot switch back.
  - Not wired: `LabelScanRepositoryImpl`, `SupplementScanRepositoryImpl` (transcription), `ExerciseParseRepositoryImpl`, `RecipeParseRepositoryImpl` (names echo the user's words; exercise tokens are compared).

- [ ] **Step 4:** `./gradlew :core:data:testDebugUnitTest assembleDebug` → PASS.
- [ ] **Step 5: Commit** — `AI: replies follow the app language`.

---

### Task 10: Emulator pass

- [ ] **Step 1:** `./gradlew :app:installDebug :wear:assembleDebug`; `adb shell cmd locale set-app-locales ph.mart.healthapp --locales fil`; relaunch.
- [ ] **Step 2:** Screenshot and read (`adb exec-out screencap -p > scratchpad/<name>.png`): Home (ring, greeting, insight, cards), Food diary (date header "Ngayong araw", meal sections, add sheet), quick-log sheet, Progress overview (every subject card's unit + footnote), one subject page, Badges, Coach (send one question online — reply must be Filipino), Profile, Settings (Language row opens the system picker), Reminders, the widget on the home screen.
- [ ] **Step 3:** For each truncation/overflow found, fix at the component: prefer the shorter Filipino wording first (it is the cheaper fix and keeps layouts untouched), then `maxLines`/`overflow = TextOverflow.Ellipsis`, then a wrapping layout. No new spacing values.
- [ ] **Step 4:** `adb shell cmd locale set-app-locales ph.mart.healthapp --locales en` and spot-check Home + Progress overview: English unchanged.
- [ ] **Step 5:** `./gradlew assembleDebug testDebugUnitTest checkUiLiterals checkTranslations` → PASS.
- [ ] **Step 6: Commit** — `Filipino: fixes from the device pass` (list what was and wasn't checked; onboarding was not, as it needs a fresh install).

### Task 11: Docs

- [ ] **Step 1: CLAUDE.md → Localization:** "No translation ships" → Filipino ships in `<qualifier>` across all eleven modules; string count updated (`grep -c` it); a new bullet: **`./gradlew checkTranslations` is the second gate** — every new string needs its Filipino entry, following `docs/glossary-fil.md`; the "What stays in Kotlin" paragraph loses the pure-function exemption and gains `Phrase` (`:core:data/Phrase.kt`): "a pure function that chooses words returns a `Phrase`; its test asserts the phrase, the composable resolves it". Build & check gains `./gradlew checkTranslations`.
- [ ] **Step 2: FEATURES.md:** under Surfaces/Settings add the Language row; replace "No translation ships" line 714 with what ships.
- [ ] **Step 3: DECISIONS.md → Localization**, new entries: (a) the exemption retired and why `Phrase` over a case type per function; (b) Filipino first because `.` decimals keep the ASCII round trip; (c) the qualifier answer from Task 3 Step 5; (d) the locale config hand-written; (e) `checkTranslations`' placeholder rule and why it is a crash guard; (f) the AI language line, read per request, carving out units and saved names; (g) the template rule added to `checkUiLiterals`, retiring that half of its ponytail. Update the `Locale.US` entry's ponytail: Filipino uses `.`, so it still stands for this language.
- [ ] **Step 4: Commit** — `Docs: Filipino ships`.
