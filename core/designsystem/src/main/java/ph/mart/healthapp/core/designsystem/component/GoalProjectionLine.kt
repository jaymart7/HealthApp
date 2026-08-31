package ph.mart.healthapp.core.designsystem.component

/**
 * "When do I get there?", in one sentence — the words shared by Progress's goal projection card,
 * Progress's weekly recap and Home's weight card. Three surfaces, one wording: the projection is
 * derived once in `core.data.progress.goalProjection`, and this is what stops the three from
 * describing the same fact differently.
 *
 * Primitives rather than a `GoalProjection`, because `:core:designsystem` has no dependency on
 * `:core:data` (see this module's build file) — the caller converts and formats the weight, the
 * same division `MacroBar` and `WaterGlassRow` already make.
 *
 * [windowDays] is named in the sentence rather than left implicit: the recap card is headed
 * "Last 7 days" while the fit runs over thirty, and a line that doesn't say which is which is a
 * card contradicting its own heading. Callers pass `PROJECTION_WINDOW_DAYS`, so the words can't
 * drift from the constant.
 *
 * [goalWeightLabel] is already unit-converted and suffixed — "72 kg", "159 lb".
 */
fun goalProjectionLine(
    goalWeightLabel: String,
    targetEpochDay: Long?,
    reached: Boolean,
    windowDays: Long,
): String = when {
    reached -> "You're at your goal weight."
    targetEpochDay != null ->
        "On the last $windowDays days' trend, $goalWeightLabel around ${formatEpochDay(targetEpochDay)}."
    // Flat, pointing away from the goal, or past the horizon — the projection reports no date, and
    // saying so beats a date the next weigh-in would move by a year.
    else -> "No date to project at this pace."
}
