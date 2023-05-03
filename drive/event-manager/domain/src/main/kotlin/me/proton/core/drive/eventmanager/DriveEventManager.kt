/*
 * Copyright (c) 2021-2023 Proton AG.
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

package me.proton.core.drive.eventmanager

import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import me.proton.core.account.domain.entity.Account
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.accountmanager.presentation.observe
import me.proton.core.accountmanager.presentation.onAccountDisabled
import me.proton.core.accountmanager.presentation.onAccountReady
import me.proton.core.domain.arch.mapSuccessValueOrNull
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.extension.filterSuccessOrError
import me.proton.core.drive.share.domain.usecase.GetShares
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.drive.volume.domain.entity.isActive
import me.proton.core.drive.volume.domain.usecase.GetVolumes
import me.proton.core.eventmanager.domain.EventManagerConfig
import me.proton.core.eventmanager.domain.EventManagerProvider
import me.proton.core.presentation.app.AppLifecycleProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DriveEventManager @Inject constructor(
    private val appLifecycleProvider: AppLifecycleProvider,
    private val eventManagerProvider: EventManagerProvider,
    private val accountManager: AccountManager,
    private val getShares: GetShares,
    private val getVolumes: GetVolumes,
) {
    private val scopes = mutableMapOf<UserId, CoroutineScope>()
    private val startedVolumeIds = mutableSetOf<VolumeId>()

    fun start() {
        accountManager.observe(appLifecycleProvider.lifecycle, minActiveState = Lifecycle.State.CREATED)
            .onAccountReady { account ->
                eventManagerProvider.get(EventManagerConfig.Core(account.userId)).start()
                account.startListeningToVolumesEvents()
            }
            .onAccountDisabled { account ->
                scopes.remove(account.userId)?.cancel()
                eventManagerProvider.get(EventManagerConfig.Core(account.userId)).stop()
                account.stopListeningToVolumesEvents()
            }
    }
    @Suppress("unused")
    private fun getAllShareIds(userId: UserId) =
        getShares(userId)
            .filterSuccessOrError()
            .mapSuccessValueOrNull()
            .filterNotNull()
            .map { shares ->
                shares
                    .filter { share -> share.isMain && share.isLocked.not() }
                    .map { share -> share.id }
            }

    private fun getAllVolumeIds(userId: UserId) =
        getVolumes(userId)
            .filterSuccessOrError()
            .mapSuccessValueOrNull()
            .filterNotNull()
            .map { volumes ->
                volumes
                    .filter { volume -> volume.isActive }
                    .map { volume -> volume.id }
            }

    private fun Account.startListeningToVolumesEvents() {
        getAllVolumeIds(userId).onEach { volumeIds ->
            val newVolumeIds = volumeIds.toSet().subtract(startedVolumeIds)
            newVolumeIds.forEach { volumeId ->
                eventManagerProvider.get(EventManagerConfig.Drive.Volume(userId, volumeId.id)).start()
                startedVolumeIds.add(volumeId)
            }
            val removedVolumeIds = startedVolumeIds.subtract(volumeIds.toSet())
            removedVolumeIds.forEach { volumeId ->
                eventManagerProvider.get(EventManagerConfig.Drive.Volume(userId, volumeId.id)).stop()
                startedVolumeIds.remove(volumeId)
            }
        }.launchIn(scopes.getOrPut(userId) {
            CoroutineScope(Dispatchers.IO + Job())
        })
    }

    private suspend fun Account.stopListeningToVolumesEvents() {
        startedVolumeIds.forEach { volumeId ->
            eventManagerProvider.get(EventManagerConfig.Drive.Volume(userId, volumeId.id)).stop()
        }
        startedVolumeIds.clear()
    }
}
