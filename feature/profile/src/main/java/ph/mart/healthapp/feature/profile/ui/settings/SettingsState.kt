package ph.mart.healthapp.feature.profile.ui.settings

import androidx.annotation.StringRes
import ph.mart.healthapp.core.data.profile.Profile
import ph.mart.healthapp.core.data.transfer.LocalBackup
import ph.mart.healthapp.feature.profile.R

/** Everything on Settings and Reminders derives from the one Room-backed profile. No second copy
 * of any value lives here, and there is no settings store separate from the profile row.
 *
 * [backups] is the exception, and it isn't a copy of anything: the weekly job's files are on disk,
 * not in Room, so they are re-listed on each profile emission rather than observed. */
data class SettingsUiState(
    val profile: Profile? = null,
    val backups: List<LocalBackup> = emptyList(),
)

/** The reminder switches, as one addressable thing so the rows and the ViewModel don't each
 * hand-roll a copy of the same when-block. Persisted to the profile row only — the actual schedule
 * is derived from that row by `ph.mart.healthapp.reminder` in `:app`, which is why nothing here
 * calls a scheduler. [FastingGoal] is the one one-shot in the set — it is derived off the running
 * fast rather than off a repeating clock, but the switch behind it is the same plain Room write.
 *
 * [group] is what the Reminders screen sorts on. Eight switches under one caption was a screen
 * wearing a card's clothes; the grouping lives on the enum rather than in the screen so a ninth
 * reminder lands in a group by declaring one, not by being remembered about. */
enum class ReminderKind(
    @StringRes val label: Int,
    @StringRes val sublabel: Int,
    val group: ReminderGroup,
) {
    Meals(R.string.profile_reminder_meals, R.string.profile_reminder_meals_sub, ReminderGroup.Logging),
    WeighIn(R.string.profile_reminder_weigh_in, R.string.profile_reminder_weigh_in_sub, ReminderGroup.Logging),
    Photo(R.string.profile_reminder_photo, R.string.profile_reminder_photo_sub, ReminderGroup.Logging),
    Supplements(R.string.profile_reminder_supplements, R.string.profile_reminder_supplements_sub, ReminderGroup.Logging),
    Water(R.string.profile_reminder_water, R.string.profile_reminder_water_sub, ReminderGroup.Targets),
    FastingGoal(R.string.profile_reminder_fasting, R.string.profile_reminder_fasting_sub, ReminderGroup.Targets),
    Workout(R.string.profile_reminder_workout, R.string.profile_reminder_workout_sub, ReminderGroup.Targets),
    WeeklyRecap(R.string.profile_reminder_recap, R.string.profile_reminder_recap_sub, ReminderGroup.Summary),
}

/** What the eight split into: a nudge to *log* something, a nudge about a *target*, and the one
 * that points at a report. Ordered as the screen draws them. */
enum class ReminderGroup(@StringRes val label: Int) {
    Logging(R.string.profile_reminders_group_logging),
    Targets(R.string.profile_reminders_group_targets),
    Summary(R.string.profile_reminders_group_summary),
}

internal fun Profile.reminderEnabled(kind: ReminderKind): Boolean = when (kind) {
    ReminderKind.Meals -> mealRemindersOn
    ReminderKind.WeighIn -> weighInReminderOn
    ReminderKind.Photo -> photoReminderOn
    ReminderKind.Water -> waterRemindersOn
    ReminderKind.FastingGoal -> fastingRemindersOn
    ReminderKind.Supplements -> supplementRemindersOn
    ReminderKind.Workout -> workoutRemindersOn
    ReminderKind.WeeklyRecap -> recapReminderOn
}

internal fun Profile.withReminder(kind: ReminderKind, enabled: Boolean): Profile = when (kind) {
    ReminderKind.Meals -> copy(mealRemindersOn = enabled)
    ReminderKind.WeighIn -> copy(weighInReminderOn = enabled)
    ReminderKind.Photo -> copy(photoReminderOn = enabled)
    ReminderKind.Water -> copy(waterRemindersOn = enabled)
    ReminderKind.FastingGoal -> copy(fastingRemindersOn = enabled)
    ReminderKind.Supplements -> copy(supplementRemindersOn = enabled)
    ReminderKind.Workout -> copy(workoutRemindersOn = enabled)
    ReminderKind.WeeklyRecap -> copy(recapReminderOn = enabled)
}

/** File IO stays in the composable, which owns the picker `Uri`; the ViewModel only ever produces
 * or consumes a JSON string. */
sealed interface SettingsSideEffect {
    data class ExportReady(val json: String) : SettingsSideEffect
    data class ImportFinished(val error: String?) : SettingsSideEffect
}
