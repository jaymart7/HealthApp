package ph.mart.healthapp.feature.progress.ui.shared

import androidx.annotation.StringRes
import ph.mart.healthapp.feature.progress.R

/**
 * The window a recap covers, and the words this tab puts on it.
 *
 * The *fold* lives in `:core:data/recap/` — `Recap` and `recap()` — because the coach draws a
 * report card of its own and `:feature:*` modules never import each other. What stays here is the
 * naming: three string resources and a coach question are a feature's business, and `recap()` only
 * ever needed [days].
 *
 * All three are rolling and end today — never a calendar week or month, which would report a
 * half-empty Monday or a half-empty first of the month.
 *
 * Deliberately *not* [ph.mart.healthapp.core.data.progress.ChartRange]: that has no week, and its
 * `inRange` helpers anchor a weight series to the latest *entry* rather than to today, which is
 * exactly the thing a card headed "Last 30 days" must not do.
 *
 * [days] stops at a year because that is how far back the inputs themselves reach —
 * `observeDailyNutrition()` is a dense year and the logged-day sets are windowed to
 * [ph.mart.healthapp.core.data.streak.STREAK_WINDOW_DAYS].
 *
 * [coachQuestion] is what this period would ask the coach, and **null means the page offers no
 * coach action for it at all** — the same column [ph.mart.healthapp.feature.progress.ui.progress.Subject]
 * carries, for the same reason. Two of the three have one: `get_history` reads a span of at most a
 * month (`MAX_HISTORY_DAYS`, which is `internal` to `:core:data` and so cannot be linked from
 * here), so a week and a month are spans the coach can actually answer while a year is one it
 * would answer from a month of data without saying so.
 * A confident answer over the wrong window is worse than no button, which is the rule the subject
 * pages are already written against.
 */
enum class RecapPeriod(
    @StringRes val short: Int,
    @StringRes val label: Int,
    val days: Int,
    @StringRes val coachQuestion: Int? = null,
) {
    Week(R.string.progress_period_week, R.string.progress_period_week_heading, 7, R.string.progress_ask_recap_week),
    Month(R.string.progress_period_month, R.string.progress_period_month_heading, 30, R.string.progress_ask_recap_month),
    Year(R.string.progress_period_year, R.string.progress_period_year_heading, 365),
}

/** The window the Progress screen's own card always shows — the recap as it shipped. */
val DEFAULT_RECAP_PERIOD = RecapPeriod.Week
