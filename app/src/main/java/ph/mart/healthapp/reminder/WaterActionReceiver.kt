package ph.mart.healthapp.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ph.mart.healthapp.core.data.profile.ProfileRepository
import ph.mart.healthapp.core.data.water.WaterRepository
import ph.mart.healthapp.today.addGlass

/**
 * The water reminder's **+1 glass** button, landing in Room without the app coming to the
 * foreground.
 *
 * The count is re-read here rather than carried in the intent, the rule
 * [ph.mart.healthapp.wear.PhoneWearListenerService] and the widget's button already follow: a
 * notification posted at 11:00 and tapped at 14:00 must not add a glass against the count that was
 * true when it was posted. [addGlass] is that shared write, so all four surfaces cap at one goal.
 *
 * A system surface, so the repositories come from Koin's global context — the trick
 * [ReminderWorker] uses. Nothing pushes to the widget or the watch from here: the Application's
 * `todaySnapshotFlow` collector fires the moment Room emits.
 */
class WaterActionReceiver : BroadcastReceiver(), KoinComponent {

    private val waterRepository: WaterRepository by inject()
    private val profileRepository: ProfileRepository by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        // onReceive is on the main thread, so the write cannot be runBlocking'd the way the wear
        // service's is; goAsync is what keeps the process alive until it lands.
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                addGlass(waterRepository, profileRepository)
                // Answering the nudge cancels it rather than rewriting its text: a notification
                // left in the shade with an updated count would be a second surface reporting
                // today's water, and the widget is that.
                if (id >= 0) NotificationManagerCompat.from(context).cancel(id)
            } finally {
                pending.finish()
            }
        }
    }
}
