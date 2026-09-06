package ph.mart.healthapp.feature.profile.ui.shared

import androidx.annotation.StringRes
import ph.mart.healthapp.core.data.profile.ActivityLevel
import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.core.data.profile.Sex
import ph.mart.healthapp.feature.profile.R

/**
 * Display names for the three profile enums whose `name` is not the display name — each one is a
 * stored token (a `profile` column, an export field), so the words live here and the enum keeps
 * its identity. `:feature:profile` owns them because it is the only feature that prints them; the
 * near-identical set in `:feature:onboarding` is a deliberate duplicate rather than an import,
 * since a cross-feature type reference is exactly what the module boundary forbids.
 *
 * Two flows draw all three now — the Profile header's summary line and About you's pickers — which
 * is what moved them out of the section that used to hold the only copy.
 */
@StringRes
internal fun ActivityLevel.label(): Int = when (this) {
    ActivityLevel.Sedentary -> R.string.profile_activity_sedentary
    ActivityLevel.Light -> R.string.profile_activity_light
    ActivityLevel.Moderate -> R.string.profile_activity_moderate
    ActivityLevel.Very -> R.string.profile_activity_very
}

/** What each activity level actually looks like in a week. Only About you shows these — the header
 * has room for the name alone. */
@StringRes
internal fun ActivityLevel.sublabel(): Int = when (this) {
    ActivityLevel.Sedentary -> R.string.profile_activity_sedentary_sub
    ActivityLevel.Light -> R.string.profile_activity_light_sub
    ActivityLevel.Moderate -> R.string.profile_activity_moderate_sub
    ActivityLevel.Very -> R.string.profile_activity_very_sub
}

@StringRes
internal fun Goal.label(): Int = when (this) {
    Goal.Lose -> R.string.profile_goal_lose
    Goal.Maintain -> R.string.profile_goal_maintain
    Goal.Build -> R.string.profile_goal_build
}

/** The calorie adjustment each goal applies, which is the one thing that makes the three choices
 * concrete rather than three words. Mirrors `GOAL_ADJUSTMENT_KCAL` in `:core:data` — that map is
 * internal to the module, so this is copy about it rather than a second source of it. */
@StringRes
internal fun Goal.sublabel(): Int = when (this) {
    Goal.Lose -> R.string.profile_goal_lose_sub
    Goal.Maintain -> R.string.profile_goal_maintain_sub
    Goal.Build -> R.string.profile_goal_build_sub
}

/** The goal in the present continuous — what the header says you are *doing*, as opposed to what
 * About you asks you to *choose*. "Losing weight" is a state; "Lose weight" is a button. */
@StringRes
internal fun Goal.headline(): Int = when (this) {
    Goal.Lose -> R.string.profile_header_goal_lose
    Goal.Maintain -> R.string.profile_header_goal_maintain
    Goal.Build -> R.string.profile_header_goal_build
}

@StringRes
internal fun Sex.label(): Int = when (this) {
    Sex.Male -> R.string.profile_sex_male
    Sex.Female -> R.string.profile_sex_female
}
