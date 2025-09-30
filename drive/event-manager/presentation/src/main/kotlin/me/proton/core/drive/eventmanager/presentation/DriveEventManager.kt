/*
 * Copyright (c) 2021-2024 Proton AG.
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

package me.proton.core.drive.eventmanager.presentation

import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.transform
import me.proton.core.account.domain.entity.Account
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.accountmanager.presentation.observe
import me.proton.core.accountmanager.presentation.onAccountDisabled
import me.proton.core.accountmanager.presentation.onAccountReady
import me.proton.core.domain.arch.mapSuccessValueOrNull
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.extension.filterSuccessOrError
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.eventmanager.entity.VolumeConfig
import me.proton.core.drive.eventmanager.repository.VolumeConfigRepository
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId
import me.proton.core.drive.feature.flag.domain.extension.on
import me.proton.core.drive.feature.flag.domain.usecase.GetFeatureFlagFlow
import me.proton.core.drive.share.domain.entity.Share
import me.proton.core.drive.share.domain.usecase.GetShares
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.drive.volume.domain.entity.isActive
import me.proton.core.drive.volume.domain.usecase.GetVolumes
import me.proton.core.eventmanager.domain.EventManagerConfig
import me.proton.core.eventmanager.domain.EventManagerProvider
import me.proton.core.presentation.app.AppLifecycleProvider
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DriveEventManager @Inject constructor(
    private val appLifecycleProvider: AppLifecycleProvider,
    private val eventManagerProvider: EventManagerProvider,
    private val accountManager: AccountManager,
    private val getShares: GetShares,
    private val getVolumes: GetVolumes,
    private val configurationProvider: ConfigurationProvider,
    private val getFeatureFlag: GetFeatureFlagFlow,
    private val repository: VolumeConfigRepository
) {
    private val scopes = mutableMapOf<UserId, CoroutineScope>()
    private val startedConfigs: MutableSet<VolumeConfig> =
        Collections.newSetFromMap(ConcurrentHashMap())

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

    suspend fun stop(config: EventManagerConfig.Drive) = eventManagerProvider.get(config).stop()

    private fun getAllStandardShareVolumeIds(userId: UserId): Flow<Set<VolumeId>> =
        getShares(
            userId = userId,
            shareType = Share.Type.STANDARD,
            refresh = flowOf(false),
        )
            .filterSuccessOrError()
            .mapSuccessValueOrNull()
            .transform { shares ->
                emit(
                    shares
                        ?.filter { share -> share.isLocked.not() }
                        ?.map { share -> share.volumeId }
                        ?.toSet()
                        ?: emptySet()
                )
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
                    .toSet()
            }

    private fun getAllConfigs(userId: UserId) = combine(
        getAllVolumeIds(userId),
        getAllStandardShareVolumeIds(userId),
        getFeatureFlag(FeatureFlagId.driveSharingDisabled(userId)),
    ) { volumeIds, shareVolumeIds, sharingDisabled ->
        repository.removeAll(userId)
        listOfNotNull(
            volumeIds.map { volumeId ->
                VolumeConfig(volumeId).also { volumeConfig ->
                    repository.add(userId, volumeId, volumeConfig)
                }
            },
            takeUnless { sharingDisabled.on }
                ?.let {
                    shareVolumeIds.subtract(volumeIds).map { volumeId ->
                        VolumeConfig(volumeId, configurationProvider.minimumSharedVolumeEventFetchInterval).also { volumeConfig ->
                            repository.add(userId, volumeId, volumeConfig)
                        }
                    }
                },
        ).flatten().toSet()
    }

    private fun Account.startListeningToVolumesEvents() {
        getAllConfigs(userId).onEach { configs ->
            val newConfigs = configs.subtract(startedConfigs)
            newConfigs.forEach { config ->
                eventManagerProvider.get(
                    EventManagerConfig.Drive.Volume(userId, config.volumeId.id, config.minimumFetchInterval)
                ).start()
                startedConfigs.add(config)
            }
            val removedConfigs = startedConfigs.subtract(configs)
            removedConfigs.forEach { config ->
                eventManagerProvider.get(
                    EventManagerConfig.Drive.Volume(userId, config.volumeId.id, config.minimumFetchInterval)
                ).stop()
                startedConfigs.remove(config)
            }
        }.launchIn(scopes.getOrPut(userId) {
            CoroutineScope(Dispatchers.IO + Job())
        })
    }

    private suspend fun Account.stopListeningToVolumesEvents() {
        val configs = startedConfigs.toList()
        startedConfigs.clear()
        configs.forEach { config ->
            eventManagerProvider.get(
                EventManagerConfig.Drive.Volume(userId, config.volumeId.id, config.minimumFetchInterval)
            ).stop()
        }
    }
}
