package ph.mart.healthapp.feature.home.ui

import ph.mart.healthapp.core.designsystem.component.HomeCard

/**
 * How Home turns the user's card order into rows, and which cards the Today strip mirrors.
 *
 * All pure, all JVM-testable, no Compose and no copy — the `homePhase()`/`daysSincePhoto()` shape,
 * in its own file rather than `HomeData.kt`'s so it stays outside `literalExceptions` and the
 * localization gate keeps watching it.
 */

/**
 * The width table. Half cards pair with an adjacent half; everything else takes its own row.
 *
 * Deliberately **not** a property on [HomeCard] itself: `:core:designsystem` knows nothing about a
 * running fast, and width is a renderer's concern that Profile's layout editor never asks about.
 * Fasting is the one card whose width moves — a running fast owns a timer, a goal bar and two
 * buttons, and an idle one is a label and a Start button.
 */
internal fun HomeCard.isHalf(fastRunning: Boolean): Boolean = when (this) {
    HomeCard.Streak,
    HomeCard.Weight,
    HomeCard.Steps,
    HomeCard.Sleep,
    HomeCard.Heart,
    HomeCard.BloodPressure,
    HomeCard.ProgressPhoto,
    -> true

    HomeCard.Fasting -> !fastRunning

    HomeCard.Calories,
    HomeCard.Water,
    HomeCard.Macros,
    HomeCard.Mood,
    HomeCard.Supplements,
    HomeCard.Cycle,
    HomeCard.Workout,
    -> false
}

/**
 * Walks the user's ordered, *already gated* list left to right: two adjacent halves make a paired
 * row, anything else takes a row of its own — so an unpaired half falls back to full width rather
 * than leaving a hole beside it.
 *
 * The gating has to happen before this, not inside the row loop: a card hidden for want of data
 * would otherwise still claim a slot and split a pair that should have closed up. That is what
 * makes an account with no watch and no profile re-pair its survivors instead of drawing gaps.
 */
internal fun homeRows(visible: List<HomeCard>, fastRunning: Boolean): List<List<HomeCard>> {
    val rows = mutableListOf<List<HomeCard>>()
    var i = 0
    while (i < visible.size) {
        val a = visible[i]
        val b = visible.getOrNull(i + 1)
        if (a.isHalf(fastRunning) && b != null && b.isHalf(fastRunning)) {
            rows += listOf(a, b)
            i += 2
        } else {
            rows += listOf(a)
            i += 1
        }
    }
    return rows
}

/**
 * Priority order for the Today strip.
 *
 * ponytail: a guess, not a measurement — the app records no per-card tap counts, so there is no
 * signal to rank by. Drive this off real counts if one ever exists; don't invent a ranking to
 * justify a different list.
 */
private val STRIP_PRIORITY = listOf(
    HomeCard.Calories,
    HomeCard.Water,
    HomeCard.Steps,
    HomeCard.Streak,
    HomeCard.Weight,
)

/** How many cells the strip needs before it is worth drawing at all. */
private const val MIN_STRIP_CELLS = 2

/**
 * The two or three cards the Today strip mirrors, in priority order.
 *
 * The strip carries **no new data** — every cell restates a card that is on the screen below it,
 * so hiding a card takes its cell with it and nothing can report a figure the user has switched
 * off. One survivor is not a summary, so the strip disappears rather than drawing a lone cell.
 */
internal fun todayStripCards(visible: List<HomeCard>): List<HomeCard> =
    STRIP_PRIORITY.filter { it in visible }.take(3).takeIf { it.size >= MIN_STRIP_CELLS }.orEmpty()
