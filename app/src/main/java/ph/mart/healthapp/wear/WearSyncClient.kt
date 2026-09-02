package ph.mart.healthapp.wear

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ph.mart.healthapp.core.today.KEY_SNAPSHOT
import ph.mart.healthapp.core.today.TODAY_PATH
import ph.mart.healthapp.core.today.TodaySnapshot
import ph.mart.healthapp.core.today.encodeSnapshot

private const val TAG = "WearSync"

/**
 * Pushes today to the watch. One data item at one path, replaced in full — the Data Layer keeps
 * the last value on both devices, so the watch needs no store of its own and reads whatever
 * arrived last even with the phone out of range. That persisted item *is* the watch's cache.
 *
 * `setUrgent()` because these pushes follow a tap the user just made; without it the system may
 * sit on the item for up to half an hour, which is the whole latency budget of a wrist glance.
 *
 * Identical bytes are not re-delivered by the Data Layer, so pushing an unchanged snapshot costs
 * nothing — the caller does not need to dedupe beyond its own `distinctUntilChanged`.
 *
 * `Tasks.await` on [Dispatchers.IO] rather than `kotlinx-coroutines-play-services`, the call
 * `GoogleHealthAuth` already made: one blocking call is not worth a dependency, and the
 * `withContext` is what keeps it off the main thread.
 */
suspend fun pushTodayToWear(context: Context, snapshot: TodaySnapshot) = withContext(Dispatchers.IO) {
    val request = PutDataMapRequest.create(TODAY_PATH).apply {
        dataMap.putString(KEY_SNAPSHOT, encodeSnapshot(snapshot))
    }.asPutDataRequest().setUrgent()
    runCatching { Tasks.await(Wearable.getDataClient(context).putDataItem(request)) }
        // No watch paired, Play Services missing, or the API disabled — all of them ordinary on a
        // phone with no watch, and none of them worth crashing an app that never mentions one.
        .onFailure { Log.d(TAG, "Today not pushed to the watch: ${it.message}") }
    Unit
}
