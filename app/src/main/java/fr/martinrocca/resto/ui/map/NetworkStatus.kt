package fr.martinrocca.resto.ui.map

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

@Composable
internal fun rememberIsNetworkAvailable(): Boolean {
    val applicationContext = LocalContext.current.applicationContext
    val connectivityManager = remember(applicationContext) {
        applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
    var isAvailable by remember(connectivityManager) {
        mutableStateOf(connectivityManager.hasInternetNetwork())
    }

    DisposableEffect(connectivityManager) {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                isAvailable = connectivityManager.hasInternetNetwork()
            }

            override fun onLost(network: Network) {
                isAvailable = connectivityManager.hasInternetNetwork()
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                isAvailable = networkCapabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_INTERNET,
                )
            }
        }
        val registered = runCatching {
            connectivityManager.registerDefaultNetworkCallback(callback)
        }.isSuccess
        onDispose {
            if (registered) runCatching {
                connectivityManager.unregisterNetworkCallback(callback)
            }
        }
    }
    return isAvailable
}

private fun ConnectivityManager.hasInternetNetwork(): Boolean {
    val network = activeNetwork ?: return false
    return getNetworkCapabilities(network)
        ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
}
