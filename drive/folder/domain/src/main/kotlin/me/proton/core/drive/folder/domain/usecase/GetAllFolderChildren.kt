/*
 * Copyright (c) 2021-2023 Proton AG.
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
package me.proton.core.drive.folder.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.transform
import me.proton.core.drive.base.domain.extension.flowOf
import me.proton.core.drive.base.domain.repository.listFetcherEmitOnEmpty
import me.proton.core.drive.folder.domain.repository.FolderRepository
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.Link
import javax.inject.Inject

class GetAllFolderChildren @Inject constructor(
    private val folderRepository: FolderRepository,
) {
    operator fun invoke(
        folderId: FolderId,
        refresh: Flow<Boolean>? = null,
    ) =
        (refresh ?: defaultRefresh(folderId)).transform { shouldRefresh ->
            if (shouldRefresh) {
                listFetcherEmitOnEmpty<Link> { folderRepository.fetchAllFolderChildren(folderId).getOrThrow() }
            }
            emitAll(folderRepository.getAllFolderChildrenFlow(folderId))
        }

    private fun defaultRefresh(folderId: FolderId): Flow<Boolean> = flowOf {
        !folderRepository.hasFolderChildren(folderId)
    }
}
