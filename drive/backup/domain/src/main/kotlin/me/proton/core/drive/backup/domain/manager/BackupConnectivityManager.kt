/*
 * Copyright (c) 2023-2024 Proton AG.
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

package me.proton.core.drive.backup.domain.manager

import kotlinx.coroutines.flow.Flow

interface BackupConnectivityManager {

    enum class Connectivity {
        NONE, UNMETERED, CONNECTED
    }

    data class NetworkStatusInfo(
        val isConnected: Boolean,
        val isValidated: Boolean,
        val downstreamBandwidthKbps: Int,
        val upstreamBandwidthKbps: Int,
        val isWifi: Boolean,
        val isCellular: Boolean,
    )

    val connectivity: Flow<Connectivity>

    fun getCurrentNetworkStatusInfo(): NetworkStatusInfo?
}
