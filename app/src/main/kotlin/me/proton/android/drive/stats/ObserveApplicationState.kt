/*
 * Copyright (c) 2024 Proton AG.
 * This file is part of Proton Drive.
 *
 * Proton Drive is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Drive is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Drive.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.android.drive.stats

import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.backup.domain.manager.BackupConnectivityManager
import me.proton.core.presentation.app.AppLifecycleProvider
import javax.inject.Inject

class ObserveApplicationState @Inject constructor(
    private val appLifecycleProvider: AppLifecycleProvider,
    private val connectivityManager: BackupConnectivityManager
) {
    operator fun invoke() = combine(
        appLifecycleProvider.state,
        connectivityManager.connectivity,
    ) { state, connectivity ->
        connectivityManager.getCurrentNetworkStatusInfo()
        Event.ApplicationState(
            inForeground = state == AppLifecycleProvider.State.Foreground,
            connectivity = when(connectivity){
                BackupConnectivityManager.Connectivity.NONE -> "disconnected"
                BackupConnectivityManager.Connectivity.UNMETERED -> "connected (unmetered)"
                BackupConnectivityManager.Connectivity.CONNECTED -> "connected (metered)"
            },
            currentNetworkStatus = connectivityManager.getCurrentNetworkStatusInfo()?.let { cnsi ->
                buildString {
                    appendLine("connected=${cnsi.isConnected}")
                    appendLine("validated=${cnsi.isValidated}")
                    appendLine("downstream bandwidth=${cnsi.downstreamBandwidthKbps} Kbps")
                    appendLine("upstream bandwidth=${cnsi.upstreamBandwidthKbps} Kbps")
                    appendLine("wifi=${cnsi.isWifi}")
                    appendLine("cellular=${cnsi.isCellular}")
                }
            }
        )
    }.distinctUntilChanged()
}
