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

package me.proton.core.drive.backup.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest
import me.proton.core.drive.backup.domain.entity.BackupConfiguration
import me.proton.core.drive.backup.domain.entity.BackupError
import me.proton.core.drive.backup.domain.entity.BackupNetworkType
import me.proton.core.drive.backup.domain.entity.BackupPermissions
import me.proton.core.drive.backup.domain.entity.BackupState
import me.proton.core.drive.backup.domain.entity.BackupStatus
import me.proton.core.drive.backup.domain.manager.BackupConnectivityManager
import me.proton.core.drive.backup.domain.manager.BackupManager
import me.proton.core.drive.backup.domain.manager.BackupPermissionsManager
import me.proton.core.drive.base.domain.extension.combine
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.IsBackgroundRestricted
import me.proton.core.drive.link.domain.entity.FolderId
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class GetBackupState @Inject constructor(
    private val backupManager: BackupManager,
    private val getBackupStatus: GetBackupStatus,
    private val permissionsManager: BackupPermissionsManager,
    private val connectivityManager: BackupConnectivityManager,
    private val getErrors: GetErrors,
    private val getAllBuckets: GetAllBuckets,
    private val isBackgroundRestricted: IsBackgroundRestricted,
    private val configurationProvider: ConfigurationProvider,
    private val getConfiguration: GetConfiguration,
) {
    operator fun invoke(folderId: FolderId): Flow<BackupState> =
        backupManager.isEnabled(folderId).transformLatest { enable ->
            if (!enable) {
                emitAll(
                    getAllBuckets().map { bucketEntries ->
                        BackupState(
                            isBackupEnabled = false,
                            hasDefaultFolder = bucketEntries?.any { entry ->
                                entry.bucketName == configurationProvider.backupDefaultBucketName
                            },
                            backupStatus = null,
                        )
                    }
                )
            } else {
                emitAll(
                    combine(
                        getBackupStatus(folderId),
                        errors(folderId),
                    ) { backupStatus, errors ->
                        BackupState(
                            isBackupEnabled = true,
                            hasDefaultFolder = true,
                            backupStatus = when {
                                errors.isEmpty() -> backupStatus
                                else -> BackupStatus.Failed(
                                    errors = errors,
                                    totalBackupPhotos = backupStatus.totalBackupPhotos,
                                    pendingBackupPhotos = when (backupStatus) {
                                        is BackupStatus.Complete -> backupStatus.totalBackupPhotos
                                        is BackupStatus.Uncompleted -> backupStatus.totalBackupPhotos
                                        is BackupStatus.Failed -> backupStatus.pendingBackupPhotos
                                        is BackupStatus.InProgress -> backupStatus.pendingBackupPhotos
                                    }
                                )
                            },
                        )
                    })
            }
        }

    private fun errors(folderId: FolderId) = combine(
        permissionsManager.backupPermissions,
        getErrors(folderId),
        connectivityManager.connectivity,
        getConfiguration(folderId),
        backupManager.isUploading(folderId),
        isBackgroundRestricted(),
    ) { permissions, errors, connectivity, configuration, uploading, isBackgroundRestricted ->
        (errors + listOf(
            permissionError(permissions),
            connectivityError(connectivity, configuration, uploading),
            backgroundRestrictionsError(isBackgroundRestricted),
        )).filterNotNull().distinct()
    }

    private fun connectivityError(
        connectivity: BackupConnectivityManager.Connectivity,
        configuration: BackupConfiguration?,
        uploading: Boolean,
    ) = if (!uploading) {
        when (connectivity) {
            BackupConnectivityManager.Connectivity.NONE -> when (configuration?.networkType) {
                BackupNetworkType.UNMETERED -> BackupError.WifiConnectivity()
                else -> BackupError.Connectivity()
            }

            BackupConnectivityManager.Connectivity.UNMETERED -> null

            BackupConnectivityManager.Connectivity.CONNECTED -> when (configuration?.networkType) {
                BackupNetworkType.UNMETERED -> BackupError.WifiConnectivity()
                else -> null
            }
        }
    } else {
        null
    }

    private fun permissionError(
        permissions: BackupPermissions,
    ) = if (permissions is BackupPermissions.Denied) {
        BackupError.Permissions()
    } else {
        null
    }

    private fun backgroundRestrictionsError(
        isBackgroundRestricted: Boolean,
    ) = if (isBackgroundRestricted) {
        BackupError.BackgroundRestrictions()
    } else {
        null
    }
}
