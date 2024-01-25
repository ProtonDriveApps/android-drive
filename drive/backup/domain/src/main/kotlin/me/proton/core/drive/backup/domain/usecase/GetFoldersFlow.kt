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

import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import me.proton.core.drive.backup.domain.repository.BackupFolderRepository
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject

class GetFoldersFlow @Inject constructor(
    private val repository: BackupFolderRepository,
    private val configurationProvider: ConfigurationProvider,
) {
    operator fun invoke(folderId: FolderId) = flow {
        val count = configurationProvider.dbPageSize
        emitAll(repository.getAllFlow(
            folderId = folderId,
            count = count,
        ).onEach { backupFolders ->
            if (backupFolders.size == count) {
                val folderCount = repository.getCount(folderId)
                if (folderCount > count) {
                    CoreLogger.e(
                        LogTag.BACKUP,
                        IllegalStateException("Cannot get all backup folders: $folderCount (limit: $count)"),
                    )
                }
            }
        })
    }

}
