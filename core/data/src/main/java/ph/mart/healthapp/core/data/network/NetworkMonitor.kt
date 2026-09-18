package ph.mart.healthapp.core.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Whether the device can reach the network, asked two ways.
 *
 * [isOnline] is the original and the common one: a **recheck at the moment of a call**, which is
 * exactly what Home's one insight call and the coach's pre-send check want — they are about to
 * spend a request and the only answer that matters is the one true now. A listener would have been
 * a subscription held open for the life of a screen to answer a question asked once.
 *
 * [observe] is the second, and it exists because one thing on the coach is a **state** rather than
 * an event. Its offline notice says "the coach reads your diary on the server, so it needs a
 * connection", and a strip that says so has to stay for as long as that is true and go when it
 * stops — otherwise the user cannot tell a fixable state from a one-off failure, which is the whole
 * distinction that notice is built on. Derived from a recheck, it would either lie (staying up
 * after the connection came back) or need polling.
 *
 * So this stopped being a `fun interface`. Two members, one implementation, and no caller of
 * [isOnline] changed: a recheck is still a recheck.
 */
interface NetworkMonitor {
    fun isOnline(): Boolean

    /**
     * The same answer as a stream, seeded immediately and then updated as the platform reports
     * changes. `distinctUntilChanged` because the callbacks fire per *network* and a phone moving
     * between wifi and cellular reports both without the answer ever changing.
     */
    fun observe(): Flow<Boolean>
}

internal class NetworkMonitorImpl(private val context: Context) : NetworkMonitor {
    override fun isOnline(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override fun observe(): Flow<Boolean> = callbackFlow {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            // Every callback re-reads [isOnline] rather than trusting what it was handed. The
            // parameter describes *one* network and the question is about the device: a phone that
            // loses wifi while on cellular gets an `onLost` and is still online.
            override fun onAvailable(network: Network) {
                trySend(isOnline())
            }

            override fun onLost(network: Network) {
                trySend(isOnline())
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                trySend(isOnline())
            }
        }
        // Seeded before registering, so a collector has an answer on its first frame rather than
        // rendering as online until the platform happens to say otherwise.
        trySend(isOnline())
        connectivityManager.registerDefaultNetworkCallback(callback)
        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()
}
