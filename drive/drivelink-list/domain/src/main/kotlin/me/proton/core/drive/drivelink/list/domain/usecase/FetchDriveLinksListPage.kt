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

package me.proton.core.drive.drivelink.list.domain.usecase

import me.proton.core.drive.base.domain.entity.SaveAction
import me.proton.core.drive.drivelink.list.domain.extension.toFolderSorting
import me.proton.core.drive.drivelink.paged.domain.entity.LinksPage
import me.proton.core.drive.folder.domain.repository.FolderRepository
import me.proton.core.drive.folder.domain.usecase.GetAllFolderChildren
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.sorting.domain.entity.By
import me.proton.core.drive.sorting.domain.entity.Sorting
import javax.inject.Inject

class FetchDriveLinksListPage @Inject constructor(
    private val folderRepository: FolderRepository,
    private val getAllFolderChildren: GetAllFolderChildren,
) {

    @Suppress("UseIfInsteadOfWhen")
    suspend operator fun invoke(
        sorting: Sorting,
        folderId: FolderId,
        page: Int,
        pageSize: Int,
    ): Result<LinksPage> =
        when (sorting.by) {
            By.NAME -> getAllFolderChildren(
                folderId = folderId,
                refresh = true,
            ).map { links -> LinksPage(links, EMPTY_SAVE_ACTION) }
            else -> folderRepository.fetchFolderChildren(folderId, page, pageSize, sorting.toFolderSorting())
                .map { (links, saveAction) ->
                    LinksPage(links, saveAction)
                }
        }

    companion object {
        private val EMPTY_SAVE_ACTION = SaveAction {}
    }
}
