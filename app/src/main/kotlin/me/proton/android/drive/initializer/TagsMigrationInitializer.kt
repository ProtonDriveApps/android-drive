/*
 * Copyright (c) 2023 Proton AG.
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

package me.proton.android.drive.initializer

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.startup.Initializer
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.transformLatest
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.accountmanager.presentation.observe
import me.proton.core.accountmanager.presentation.onAccountReady
import me.proton.core.accountmanager.presentation.onAccountRemoved
import me.proton.core.domain.arch.mapSuccessValueOrNull
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.log.LogTag.PHOTO
import me.proton.core.drive.base.domain.log.logId
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.drivelink.download.domain.usecase.Download
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.drivePhotosTagsMigration
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.drivePhotosTagsMigrationDisabled
import me.proton.core.drive.feature.flag.domain.extension.on
import me.proton.core.drive.feature.flag.domain.usecase.GetFeatureFlagFlow
import me.proton.core.drive.photo.domain.entity.TagsMigrationFile
import me.proton.core.drive.photo.domain.usecase.ContinueTagsMigrationAfterDownload
import me.proton.core.drive.photo.domain.usecase.GetLatestTagsMigrationFile
import me.proton.core.drive.photo.domain.usecase.GetTagsMigrationDownloadedFile
import me.proton.core.drive.photo.domain.usecase.StartTagsMigration
import me.proton.core.drive.photo.domain.usecase.StopTagsMigration
import me.proton.core.drive.volume.domain.entity.Volume
import me.proton.core.drive.volume.domain.usecase.GetOldestActiveVolume
import me.proton.core.presentation.app.AppLifecycleProvider
import me.proton.core.util.kotlin.CoreLogger

@Suppress("unused")
class TagsMigrationInitializer : Initializer<Unit> {

    private val scopes = mutableMapOf<UserId, CoroutineScope>()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun create(context: Context) {
        with(
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                TagsMigrationInitializerEntryPoint::class.java
            )
        ) {
            accountManager.observe(appLifecycleProvider.lifecycle, Lifecycle.State.STARTED)
                .onAccountReady { account ->
                    val userId = account.userId
                    val scope = scopes.getOrPut(userId) {
                        CoroutineScope(Dispatchers.IO + Job())
                    }
                    val enabledFlow = combine(
                        getFeatureFlagFlow(drivePhotosTagsMigrationDisabled(userId)),
                        getFeatureFlagFlow(drivePhotosTagsMigration(userId)),
                    ) { killSwitch, flag ->
                        if (killSwitch.on) {
                            false
                        } else if (flag.on) {
                            true
                        } else {
                            null
                        }
                    }
                    val volumeIdFlow = getOldestActiveVolume(userId, Volume.Type.PHOTO)
                        .mapSuccessValueOrNull()
                        .filterNotNull()
                        .map { volume -> volume.id }
                        .distinctUntilChanged()

                    combine(enabledFlow, volumeIdFlow) { enabled, volumeId ->
                        enabled to volumeId
                    }.onEach { (enabled, volumeId) ->
                        when (enabled) {
                            true -> startTagsMigration(userId, volumeId)
                                .getOrNull(
                                    PHOTO,
                                    "Failed to start migration for volume: ${volumeId.id.logId()}"
                                )

                            false -> stopTagsMigration(userId, volumeId).getOrNull(
                                PHOTO,
                                "Failed to stop migration for volume: ${volumeId.id.logId()}"
                            )

                            null -> {} // do nothing
                        }
                    }.launchIn(scope)
                    volumeIdFlow.transformLatest { volumeId ->
                        emitAll(
                            getLatestTagsMigrationFile(
                                userId,
                                volumeId,
                                TagsMigrationFile.State.PREPARED
                            ).map { tagsMigrationFile ->
                                tagsMigrationFile?.let { tagsMigrationFile.fileId}
                            }
                        )
                    }
                        .filterNotNull()
                        .distinctUntilChanged()
                        .onEach { fileId ->
                            coRunCatching {
                                CoreLogger.d(PHOTO, "Starting download for ${fileId.id.logId()}")
                                download(fileId)
                            }.getOrNull(
                                PHOTO,
                                "Failed to start download file: ${fileId.id.logId()}"
                            )
                        }.launchIn(scope)
                    volumeIdFlow.transformLatest { volumeId ->
                        emitAll(
                            getTagsMigrationDownloadedFile(userId, volumeId)
                                .map { tagsMigrationFile ->
                                    tagsMigrationFile?.let { tagsMigrationFile.fileId to volumeId }
                                }
                        )
                    }
                        .filterNotNull()
                        .distinctUntilChanged()
                        .onEach { (fileId, volumeId) ->
                            CoreLogger.d(PHOTO, "Continuing tags migration for ${fileId.id.logId()}")
                            continueTagsMigrationAfterDownload(
                                volumeId = volumeId,
                                fileId = fileId,
                            ).getOrNull(
                                PHOTO,
                                "Failed to start tagging file: ${fileId.id.logId()}"
                            )
                        }.launchIn(scope)
                }
                .onAccountRemoved { account ->
                    scopes.remove(account.userId)?.cancel()
                }
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = listOf(
        AccountStateHandlerInitializer::class.java,
        WorkManagerInitializer::class.java,
        LoggerInitializer::class.java,
    )

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface TagsMigrationInitializerEntryPoint {
        val accountManager: AccountManager
        val appLifecycleProvider: AppLifecycleProvider
        val getLatestTagsMigrationFile: GetLatestTagsMigrationFile
        val getTagsMigrationDownloadedFile: GetTagsMigrationDownloadedFile
        val getOldestActiveVolume: GetOldestActiveVolume
        val download: Download
        val continueTagsMigrationAfterDownload: ContinueTagsMigrationAfterDownload
        val startTagsMigration: StartTagsMigration
        val stopTagsMigration: StopTagsMigration
        val getFeatureFlagFlow: GetFeatureFlagFlow
    }
}
