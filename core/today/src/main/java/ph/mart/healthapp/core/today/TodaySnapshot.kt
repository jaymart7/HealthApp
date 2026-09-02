package ph.mart.healthapp.core.today

import kotlinx.serialization.Serializable

/**
 * The day at a glance: everything a glanceable surface draws, and nothing it doesn't. Derived,
 * never stored — same rule as `HomeUiState`, for the same reason: a surface holding its own copy
 * of the calorie target is a surface that will one day disagree with Home.
 *
 * One type serves three surfaces — the home-screen widget, the watch app and the watch tile — so
 * none of them can drift apart on what "today" means. That is also why this sits in its own
 * module rather than in `:core:data`: the watch renders this and must never link Room.
 *
 * All the arithmetic lives in `todaySnapshot()` (in `:app`, where the `:core:data` types are)
 * rather than in a composable, because Glance and Wear composables can only be exercised on a
 * device and this can be unit-tested.
 */
@Serializable
data class TodaySnapshot(
    /**
     * The day this describes. The watch can outlive the push that produced it, so it compares
     * this against its own clock rather than trusting whatever arrived last.
     */
    val dateEpochDay: Long = 0,
    val consumedKcal: Int = 0,
    val budgetKcal: Int = 0,
    val glasses: Int = 0,
    val goalGlasses: Int = 0,
    /**
     * e.g. "1.5 L" or "48 fl oz", already converted. The phone owns the profile, so the phone
     * formats it — that keeps `UnitSystem` (and `:core:data` with it) off the watch.
     */
    val waterLabel: String = "",
    val streakDays: Int = 0,
    /** Today's steps from Google Health. Zero means none imported — the line is omitted. */
    val steps: Int = 0,
    /**
     * When the running fast hits its target, or null when none is running — the line is omitted.
     *
     * A *time*, not an elapsed duration, and that is the whole point: neither Glance nor a tile
     * can tick, and both refresh half-hourly, so "14h 20m" would be wrong for up to thirty
     * minutes after every redraw. A target time is computed once and stays true until the fast
     * ends. The watch *app* can tick, and does.
     */
    val fastingUntilMillis: Long? = null,
    val fastingGoalReached: Boolean = false,
    /** Null means follow the device, exactly as `Profile.darkThemeOn` does. Honoured by the
     * widget; the watch is pinned dark whatever this says. */
    val darkThemeOn: Boolean? = null,
    /** No profile row yet — the user hasn't finished onboarding, so there are no targets to show. */
    val onboarding: Boolean = false,
)

/** Signed: negative once the day is over budget, which the surfaces say out loud rather than
 * clamping to zero. */
val TodaySnapshot.remainingKcal: Int get() = budgetKcal - consumedKcal

/** The bar's fill, 0f..1f. Clamped — unlike [remainingKcal] — because a bar can't overflow, and
 * a zero budget would otherwise divide by zero. */
val TodaySnapshot.progress: Float
    get() = if (budgetKcal > 0) (consumedKcal.toFloat() / budgetKcal).coerceIn(0f, 1f) else 0f

/** The count the +1 button writes. Capped at the goal, which is also where the button stops
 * being offered — the control never silently no-ops. */
val TodaySnapshot.glassesAfterAdd: Int get() = (glasses + 1).coerceAtMost(goalGlasses)

/** The `goalGlasses > 0` half is load-bearing: an empty snapshot (nothing pushed yet) has a zero
 * goal, and a surface that flashed "Goal hit" before its first emission would be lying. */
val TodaySnapshot.waterGoalReached: Boolean get() = goalGlasses > 0 && glasses >= goalGlasses
