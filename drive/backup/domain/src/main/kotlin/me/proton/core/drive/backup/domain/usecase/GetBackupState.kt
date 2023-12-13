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

package me.proton.core.drive.backup.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.transformLatest
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.backup.domain.entity.BackupError
import me.proton.core.drive.backup.domain.entity.BackupPermissions
import me.proton.core.drive.backup.domain.entity.BackupState
import me.proton.core.drive.backup.domain.entity.BackupStatus
import me.proton.core.drive.backup.domain.manager.BackupConnectivityManager
import me.proton.core.drive.backup.domain.manager.BackupManager
import me.proton.core.drive.backup.domain.manager.BackupPermissionsManager
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class GetBackupState @Inject constructor(
    private val backupManager: BackupManager,
    private val getBackupStatus: GetBackupStatus,
    private val permissionsManager: BackupPermissionsManager,
    private val connectivityManager: BackupConnectivityManager,
    private val getErrors: GetErrors,
) {
    operator fun invoke(userId: UserId): Flow<BackupState> =
        backupManager.isEnabled(userId).transformLatest { enable ->
            if (!enable) {
                emit(
                    BackupState(
                        isBackupEnabled = false,
                        backupStatus = null,
                    )
                )
            } else {
                emitAll(
                    combine(
                        getBackupStatus(userId),
                        errors(userId),
                    ) { backupStatus, errors ->
                        BackupState(
                            isBackupEnabled = true,
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

    private fun errors(userId: UserId) = combine(
        permissionsManager.backupPermissions,
        getErrors(userId),
        connectivityManager.connectivity,
        backupManager.isUploading(),
    ) { permissions, errors, connectivity, uploading ->
        (errors + permissionError(permissions) + connectivityError(connectivity, uploading))
            .filterNotNull().distinct()
    }

    private fun connectivityError(
        connectivity: BackupConnectivityManager.Connectivity,
        uploading: Boolean,
    ) = if (connectivity != BackupConnectivityManager.Connectivity.UNMETERED && !uploading) {
        BackupError.Connectivity()
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
}
