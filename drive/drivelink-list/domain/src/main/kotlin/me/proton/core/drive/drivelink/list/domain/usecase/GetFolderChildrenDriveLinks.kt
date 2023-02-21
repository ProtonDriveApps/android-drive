/*
 * Copyright (c) 2022-2023 Proton AG.
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

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform
import me.proton.core.domain.arch.DataResult
import me.proton.core.drive.base.domain.extension.asSuccess
import me.proton.core.drive.base.domain.extension.flowOf
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.repository.fetcher
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.list.domain.extension.toFolderSorting
import me.proton.core.drive.folder.domain.repository.FolderRepository
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.sorting.domain.usecase.GetSorting
import javax.inject.Inject

class GetFolderChildrenDriveLinks @Inject constructor(
    private val getDriveLinks: GetDriveLinks,
    private val folderRepository: FolderRepository,
    private val configurationProvider: ConfigurationProvider,
    private val getSorting: GetSorting,
) {
    operator fun invoke(
        folderId: FolderId,
        refresh: Flow<Boolean> = flowOf { folderRepository.shouldInitiallyFetchFolderChildren(folderId) }
    ): Flow<DataResult<List<DriveLink>>> =
        refresh.transform { shouldRefresh ->
            if (shouldRefresh) {
                fetcher<List<DriveLink>> {
                    val (_, saveAction) = folderRepository.fetchFolderChildren(
                        folderId = folderId,
                        pageIndex = 0,
                        pageSize = configurationProvider.uiPageSize,
                        sorting = getSorting(folderId.userId).first().toFolderSorting()
                    ).getOrThrow()
                    saveAction()
                }
            }
            emitAll(getDriveLinks(folderId).map { driveLinks -> driveLinks.asSuccess })
        }
}
