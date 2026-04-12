package com.skydown.android.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.core.content.getSystemService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AppNetworkMonitor {
    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    @Volatile
    private var initialized = false
    private var callback: ConnectivityManager.NetworkCallback? = null

    fun initialize(context: Context) {
        if (initialized) {
            return
        }

        val appContext = context.applicationContext
        val connectivityManager = appContext.getSystemService<ConnectivityManager>() ?: return
        _isOnline.value = connectivityManager.isCurrentlyOnline()

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _isOnline.value = true
            }

            override fun onLost(network: Network) {
                _isOnline.value = connectivityManager.isCurrentlyOnline()
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                _isOnline.value = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            }
        }

        runCatching {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(request, networkCallback)
            callback = networkCallback
            initialized = true
        }.onFailure {
            _isOnline.value = connectivityManager.isCurrentlyOnline()
        }
    }
}

private fun ConnectivityManager.isCurrentlyOnline(): Boolean {
    val active = activeNetwork ?: return false
    val capabilities = getNetworkCapabilities(active) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
