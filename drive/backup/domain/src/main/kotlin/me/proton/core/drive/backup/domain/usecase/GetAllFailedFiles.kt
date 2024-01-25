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

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import me.proton.core.drive.backup.domain.entity.BackupFile
import me.proton.core.drive.backup.domain.entity.BackupFileState
import me.proton.core.drive.backup.domain.repository.BackupFileRepository
import me.proton.core.drive.base.domain.log.LogTag.BACKUP
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject

class GetAllFailedFiles @Inject constructor(
    private val configurationProvider: ConfigurationProvider,
    private val repository: BackupFileRepository,
) {
    operator fun invoke(folderId: FolderId): Flow<List<BackupFile>> = flow {
        val count = configurationProvider.dbPageSize
        emitAll(repository.getAllInFolderIdWithState(
            folderId = folderId,
            state = BackupFileState.FAILED,
            fromIndex = 0,
            count = count,
        ).onEach { backupFiles ->
            if (backupFiles.size == count) {
                val countByState = repository.getCountByState(folderId, BackupFileState.FAILED)
                if (countByState > count) {
                    CoreLogger.e(
                        BACKUP,
                        IllegalStateException("Cannot get all failed backup files: $countByState (limit: $count)"),
                    )
                }
            }
        })
    }
}
