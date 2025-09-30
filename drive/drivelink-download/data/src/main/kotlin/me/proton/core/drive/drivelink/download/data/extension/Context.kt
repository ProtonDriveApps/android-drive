/*
 * Copyright (c) 2025 Proton AG.
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

package me.proton.core.drive.drivelink.download.data.extension

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import me.proton.core.drive.drivelink.download.domain.entity.NetworkType

val Context.observeNetworkTypes: Flow<Set<NetworkType>> get() = callbackFlow {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun NetworkCapabilities?.toNetworkTypes(): Set<NetworkType> = this?.let { caps ->
        val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        val isMetered = !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        when {
            !hasInternet -> emptySet()
            isMetered -> setOf(NetworkType.METERED, NetworkType.ANY)
            else -> setOf(NetworkType.UNMETERED, NetworkType.ANY)
        }
    } ?: emptySet()

    trySend(
        connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            .toNetworkTypes()
    )

    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            if (connectivityManager.activeNetwork == network) {
                trySend(networkCapabilities.toNetworkTypes())
            }
        }

        override fun onLost(network: Network) {
            if (connectivityManager.activeNetwork == network) {
                trySend(emptySet())
            }
        }
    }

    connectivityManager.registerDefaultNetworkCallback(callback)

    awaitClose {
        connectivityManager.unregisterNetworkCallback(callback)
    }
}
