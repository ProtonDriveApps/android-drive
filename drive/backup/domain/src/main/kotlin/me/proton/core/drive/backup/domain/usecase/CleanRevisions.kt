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

import me.proton.core.drive.backup.domain.entity.BackupDuplicate
import me.proton.core.drive.backup.domain.repository.BackupDuplicateRepository
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.folder.domain.usecase.DeleteFolderChildren
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.extension.userId
import javax.inject.Inject

class CleanRevisions @Inject constructor(
    private val repository: BackupDuplicateRepository,
    private val configurationProvider: ConfigurationProvider,
    private val deleteFolderChildren: DeleteFolderChildren,
) {
    suspend operator fun invoke(folderId: FolderId) = coRunCatching {
        var backupDuplicates: List<BackupDuplicate>
        val count = minOf(configurationProvider.dbPageSize, configurationProvider.apiPageSize)
        do {
            backupDuplicates = repository.getAllWithState(
                userId = folderId.userId,
                parentId = folderId,
                state = Link.State.DRAFT,
                fromIndex = 0,
                count = count
            )
            if (backupDuplicates.isNotEmpty()) {
                deleteFolderChildren(
                    folderId = folderId,
                    linkIds = backupDuplicates.mapNotNull { duplicate -> duplicate.linkId },
                ).getOrThrow()
                repository.deleteDuplicates(backupDuplicates)
            }
        } while (backupDuplicates.size == count)
    }
}
