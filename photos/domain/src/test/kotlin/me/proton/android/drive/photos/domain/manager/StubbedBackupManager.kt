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

package me.proton.android.drive.photos.domain.manager

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.backup.domain.manager.BackupManager
import me.proton.core.drive.backup.domain.repository.BackupFolderRepository

class StubbedBackupManager(
    private val repository: BackupFolderRepository,
) : BackupManager {
    var started = false
    var stopped = false

    var sync = emptyMap<UserId, BackupFolder>()

    override suspend fun start(userId: UserId) {
        started = true
    }

    override suspend fun stop(userId: UserId) {
        stopped = true
    }

    override fun sync(userId: UserId, backupFolder: BackupFolder, uploadPriority: Long) {
        sync = sync + (userId to backupFolder)
    }

    override fun syncAllFolders(userId: UserId, uploadPriority: Long) {
        runBlocking {
            repository.getAll(userId).forEach { folder ->
                sync(userId, folder, uploadPriority)
            }
        }
    }

    override fun watchFolders(userId: UserId) {
        throw NotImplementedError()
    }

    override fun unwatchFolders(userId: UserId) {
        throw NotImplementedError()
    }

    override fun isEnabled(userId: UserId): Flow<Boolean> =
        repository.hasFolders(userId)

    override fun isUploading(): Flow<Boolean> = flowOf(true)
}