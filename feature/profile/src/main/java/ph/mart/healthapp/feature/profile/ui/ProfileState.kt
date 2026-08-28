package ph.mart.healthapp.feature.profile.ui

import ph.mart.healthapp.core.data.profile.Profile

/** Everything on this screen derives from the one Room-backed profile — targets included, via
 * [ph.mart.healthapp.core.data.profile.dailyTargets]. No second copy of any value lives here. */
data class ProfileUiState(val profile: Profile? = null)

/** The three reminder switches, as one addressable thing so the rows and the ViewModel don't each
 * hand-roll a copy of the same when-block. Persisted to the profile row only — the actual schedule
 * is derived from that row by `ph.mart.healthapp.reminder` in `:app`, which is why nothing here
 * calls a scheduler. */
enum class ReminderKind(val label: String, val sublabel: String) {
    Meals("Meal logging", "Reminds you 3x daily"),
    WeighIn("Weigh-in day", "Every Monday, 8:00 AM"),
    Photo("Photo cadence", "Every 2 weeks"),
    Water("Water", "Twice a day, 11:00 AM & 4:00 PM"),
}

internal fun Profile.reminderEnabled(kind: ReminderKind): Boolean = when (kind) {
    ReminderKind.Meals -> mealRemindersOn
    ReminderKind.WeighIn -> weighInReminderOn
    ReminderKind.Photo -> photoReminderOn
    ReminderKind.Water -> waterRemindersOn
}

internal fun Profile.withReminder(kind: ReminderKind, enabled: Boolean): Profile = when (kind) {
    ReminderKind.Meals -> copy(mealRemindersOn = enabled)
    ReminderKind.WeighIn -> copy(weighInReminderOn = enabled)
    ReminderKind.Photo -> copy(photoReminderOn = enabled)
    ReminderKind.Water -> copy(waterRemindersOn = enabled)
}

/** File IO stays in the composable, which owns the picker `Uri`; the ViewModel only ever produces
 * or consumes a JSON string. */
sealed interface ProfileSideEffect {
    data class ExportReady(val json: String) : ProfileSideEffect
    data class ImportFinished(val error: String?) : ProfileSideEffect
}
