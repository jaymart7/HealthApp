package ph.mart.healthapp.wear

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ph.mart.healthapp.core.data.fasting.DEFAULT_FAST_GOAL_HOURS
import ph.mart.healthapp.core.data.fasting.FastingRepository
import ph.mart.healthapp.core.data.profile.ProfileRepository
import ph.mart.healthapp.core.data.water.WaterRepository
import ph.mart.healthapp.core.today.MSG_ADD_GLASS
import ph.mart.healthapp.core.today.MSG_TOGGLE_FAST
import ph.mart.healthapp.today.addGlass

/**
 * The watch's two writes, landing in Room.
 *
 * Room stays the single source of truth: the watch holds no database and sends an *intent to
 * log*, never a row. Which is why neither message carries a payload — the phone re-reads the
 * day's state here, exactly as the widget's button does, so a snapshot the watch has been
 * showing since breakfast can't add a glass to a stale count.
 *
 * A system surface like the widget and [ph.mart.healthapp.reminder.ReminderWorker], so it reaches
 * the repositories through Koin's global context rather than a ViewModel. `runBlocking` is
 * correct here and not a shortcut: `onMessageReceived` is already called on a background thread,
 * and the service may be torn down the moment it returns.
 */
class PhoneWearListenerService : WearableListenerService(), KoinComponent {

    private val waterRepository: WaterRepository by inject()
    private val profileRepository: ProfileRepository by inject()
    private val fastingRepository: FastingRepository by inject()

    override fun onMessageReceived(event: MessageEvent) {
        when (event.path) {
            MSG_ADD_GLASS -> runBlocking { addGlass(waterRepository, profileRepository) }
            MSG_TOGGLE_FAST -> runBlocking { toggleFast() }
            else -> super.onMessageReceived(event)
        }
    }

    /**
     * One message, both directions — the watch shows one button whose label follows the snapshot,
     * and asking "is a fast running" here rather than trusting the watch's answer is what stops a
     * stale wrist from ending a fast that was already ended on the phone.
     *
     * The goal is snapshotted onto the fast by `start()`, from the profile, exactly as the phone's
     * own control does: the watch has no say in it.
     */
    private suspend fun toggleFast() {
        if (fastingRepository.observeActive().first() != null) {
            fastingRepository.stop()
        } else {
            val goalHours = profileRepository.observeProfile().first()?.fastingGoalHours
                ?: DEFAULT_FAST_GOAL_HOURS
            fastingRepository.start(goalHours)
        }
    }
}
