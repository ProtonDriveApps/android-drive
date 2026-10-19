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

package me.proton.core.drive.backup.data.provider

import androidx.work.NetworkType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.backup.domain.entity.BackupNetworkType
import me.proton.core.drive.backup.domain.manager.BackupConnectivityManager
import me.proton.core.drive.backup.domain.usecase.GetConfiguration
import me.proton.core.drive.base.domain.entity.TimestampMs
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.telemetry.domain.event.PhotosEvent
import me.proton.core.drive.telemetry.domain.manager.DriveTelemetryManager
import me.proton.core.drive.upload.data.provider.NetworkTypeProvider
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class BackupNetworkTypeProvider(
    private val getConfiguration: GetConfiguration,
    private val connectivityManager: BackupConnectivityManager,
    private val driveTelemetryManager: DriveTelemetryManager,
    private val clock: () -> TimestampMs,
) : NetworkTypeProvider {

    @Inject
    constructor(
        getConfiguration: GetConfiguration,
        connectivityManager: BackupConnectivityManager,
        driveTelemetryManager: DriveTelemetryManager,
    ) : this(getConfiguration, connectivityManager, driveTelemetryManager, ::TimestampMs)

    // In-memory only: one singleton instance for the app's lifetime is enough to rate-limit
    // this per user, no need to persist it. Resets on process death, which just means the
    // next report after a restart isn't suppressed - an acceptable, minor imprecision.
    private val lastReportedAt = ConcurrentHashMap<UserId, TimestampMs>()

    override suspend fun get(folderId: FolderId): NetworkType =
        when (getConfiguration(folderId).firstOrNull()?.networkType) {
            BackupNetworkType.ANY, BackupNetworkType.UNCONFIRMED -> NetworkType.CONNECTED
            BackupNetworkType.UNMETERED, null -> NetworkType.UNMETERED
        }

    override fun isSatisfied(folderId: FolderId): Flow<Boolean> =
        combine(
            getConfiguration(folderId).map { configuration -> configuration?.networkType },
            connectivityManager.connectivity,
        ) { networkType, connectivity -> isSatisfied(networkType, connectivity) }
            .distinctUntilChanged()

    private fun isSatisfied(
        networkType: BackupNetworkType?,
        connectivity: BackupConnectivityManager.Connectivity,
    ): Boolean = when (networkType) {
        BackupNetworkType.ANY -> true
        BackupNetworkType.UNCONFIRMED ->
            connectivity == BackupConnectivityManager.Connectivity.CONNECTED_UNMETERED ||
                connectivity == BackupConnectivityManager.Connectivity.CONNECTED_UNCONFIRMED
        BackupNetworkType.UNMETERED, null ->
            connectivity == BackupConnectivityManager.Connectivity.CONNECTED_UNMETERED
    }

    override suspend fun reportUsage(folderId: FolderId) {
        val networkType = getConfiguration(folderId).firstOrNull()?.networkType
        val connectivity = connectivityManager.getCurrentConnectivity()
        if (networkType == BackupNetworkType.UNCONFIRMED &&
            connectivity == BackupConnectivityManager.Connectivity.CONNECTED_UNCONFIRMED &&
            shouldReport(folderId.userId)
        ) {
            driveTelemetryManager.enqueue(folderId.userId, PhotosEvent.BackupOverUnconfirmedNetwork())
        }
    }

    // At most once per REPORT_WINDOW_MS per user, so one backup of many files or many
    // retries reports roughly once per occurrence instead of once per file.
    private fun shouldReport(userId: UserId): Boolean {
        val now = clock()
        val last = lastReportedAt[userId]
        return if (last == null || now.value - last.value > REPORT_WINDOW_MS) {
            lastReportedAt[userId] = now
            true
        } else {
            false
        }
    }

    companion object {
        private val REPORT_WINDOW_MS = TimeUnit.HOURS.toMillis(1)
    }
}
