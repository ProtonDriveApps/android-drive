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

package me.proton.core.drive.backup.data.manager

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.backup.domain.manager.BackupManager
import me.proton.core.drive.backup.domain.repository.BackupFolderRepository
import me.proton.core.drive.link.domain.entity.FolderId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StubbedBackupManager @Inject constructor(
    private val repository: BackupFolderRepository,
) : BackupManager {
    var started = false
    var stopped = false

    var sync = emptyList<BackupFolder>()

    override suspend fun start(folderId: FolderId) {
        started = true
    }

    override suspend fun stop(folderId: FolderId) {
        stopped = true
    }

    override fun sync(backupFolder: BackupFolder, uploadPriority: Long) {
        sync = sync + backupFolder
    }

    override suspend fun cancelSync(backupFolder: BackupFolder) {
        throw NotImplementedError()
    }

    override fun syncAllFolders(folderId: FolderId, uploadPriority: Long) {
        runBlocking {
            repository.getAll(folderId).forEach { folder ->
                sync(folder, uploadPriority)
            }
        }
    }

    override suspend fun watchFolders(userId: UserId) {
        throw NotImplementedError()
    }

    override suspend fun unwatchFolders(userId: UserId) {
        throw NotImplementedError()
    }

    override fun isEnabled(folderId: FolderId): Flow<Boolean> =
        repository.hasFolders(folderId)

    override fun isUploading(folderId: FolderId): Flow<Boolean> = flowOf(true)

    override suspend fun updateNotification(folderId: FolderId) {
        // do nothing
    }
}
