package com.example.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

@Singleton
class NetworkConnectivityObserver @Inject constructor(
  @ApplicationContext private val context: Context,
) {
  private val connectivityManager =
    context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

  fun isCurrentlyOnline(): Boolean {
    val cm = connectivityManager ?: return false
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
      caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
  }

  val isOnlineFlow: Flow<Boolean> = callbackFlow {
    val cm = connectivityManager
    if (cm == null) {
      trySend(false)
      close()
      return@callbackFlow
    }

    val callback = object : ConnectivityManager.NetworkCallback() {
      override fun onAvailable(network: Network) {
        trySend(isCurrentlyOnline() || true)
      }

      override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
        val online = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        trySend(online)
      }

      override fun onLost(network: Network) {
        trySend(isCurrentlyOnline())
      }
    }

    val request = NetworkRequest.Builder()
      .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
      .build()

    try {
      cm.registerNetworkCallback(request, callback)
      trySend(isCurrentlyOnline())
    } catch (_: Exception) {
      trySend(false)
    }

    awaitClose {
      try {
        cm.unregisterNetworkCallback(callback)
      } catch (_: Exception) {
        // Ignore if already unregistered
      }
    }
  }.distinctUntilChanged()
}
