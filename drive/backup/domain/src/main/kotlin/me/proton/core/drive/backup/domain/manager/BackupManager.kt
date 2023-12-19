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

package me.proton.core.drive.backup.domain.manager

import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink

interface BackupManager {

    suspend fun start(userId: UserId)

    suspend fun stop(userId: UserId)

    fun sync(userId: UserId, backupFolder: BackupFolder, uploadPriority: Long)

    suspend fun cancelSync(userId: UserId, backupFolder: BackupFolder)

    fun syncAllFolders(userId: UserId, uploadPriority: Long = UploadFileLink.BACKUP_PRIORITY)

    fun watchFolders(userId: UserId)

    fun unwatchFolders(userId: UserId)

    fun isEnabled(userId: UserId): Flow<Boolean>

    fun isUploading(): Flow<Boolean>
}
