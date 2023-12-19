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

package me.proton.core.drive.backup.data.manager

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.backup.domain.manager.BackupManager

class StubbedBackupManager : BackupManager {
    var stopped = false

    var sync = emptyMap<UserId, BackupFolder>()

    override suspend fun start(userId: UserId) {
        NotImplementedError()
    }

    override suspend fun stop(userId: UserId) {
        stopped = true
    }

    override fun sync(userId: UserId, backupFolder: BackupFolder, uploadPriority: Long) {
        sync = sync + (userId to backupFolder)
    }

    override suspend fun cancelSync(userId: UserId, backupFolder: BackupFolder) {
        throw NotImplementedError()
    }

    override fun syncAllFolders(userId: UserId, uploadPriority: Long) {
        throw NotImplementedError()
    }

    override fun watchFolders(userId: UserId) {
        throw NotImplementedError()
    }

    override fun unwatchFolders(userId: UserId) {
        throw NotImplementedError()
    }

    override fun isEnabled(userId: UserId): Flow<Boolean> = flow {
        throw NotImplementedError()
    }

    override fun isUploading(): Flow<Boolean> {
        throw NotImplementedError()
    }
}
