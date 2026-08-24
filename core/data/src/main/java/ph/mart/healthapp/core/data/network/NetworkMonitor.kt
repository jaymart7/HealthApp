package ph.mart.healthapp.core.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/** A manual recheck (the prototype's "Try again"), not a live listener — no [kotlinx.coroutines.flow.Flow]. */
fun interface NetworkMonitor {
    fun isOnline(): Boolean
}

internal class NetworkMonitorImpl(private val context: Context) : NetworkMonitor {
    override fun isOnline(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
