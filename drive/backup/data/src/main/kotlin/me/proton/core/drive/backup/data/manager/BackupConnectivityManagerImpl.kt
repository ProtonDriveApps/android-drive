/*
 * Copyright (c) 2023 Proton AG.
 * This file is part of Proton Core.
 *
 * Proton Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Core.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.core.drive.backup.data.manager

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.shareIn
import me.proton.core.drive.backup.domain.manager.BackupConnectivityManager
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class BackupConnectivityManagerImpl @Inject constructor(
    @ApplicationContext private val appContext: Context,
    coroutineContext: CoroutineContext,
) : BackupConnectivityManager {
    private val coroutineScope = CoroutineScope(coroutineContext)
    private val connectivityManager by lazy {
        appContext.getSystemService(ConnectivityManager::class.java) as ConnectivityManager
    }

    override val connectivity: Flow<BackupConnectivityManager.Connectivity> = callbackFlow {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()
        val networkCallback = object : ConnectivityManager.NetworkCallback() {

            private var networks: List<Network> = emptyList()

            override fun onAvailable(network: Network) {
                networks = networks + network
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                trySend(networkCapabilities.getConnectivity())
            }

            override fun onLost(network: Network) {
                networks = networks - network
                trySend(
                    networks.lastOrNull()
                        ?.let(connectivityManager::getNetworkCapabilities)
                        ?.getConnectivity()
                        ?: BackupConnectivityManager.Connectivity.NONE
                )
            }

            private fun NetworkCapabilities.getConnectivity(): BackupConnectivityManager.Connectivity =
                if (hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)) {
                    BackupConnectivityManager.Connectivity.UNMETERED
                } else {
                    BackupConnectivityManager.Connectivity.METERED
                }

        }
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        awaitClose {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }.shareIn(coroutineScope, SharingStarted.WhileSubscribed(), 1)
}
