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

import androidx.paging.PagingData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.transform
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.extension.mapCatching
import me.proton.core.drive.drivelink.crypto.domain.usecase.DecryptDriveLinks
import me.proton.core.drive.drivelink.paged.domain.usecase.GetPagedDriveLinks
import me.proton.core.drive.drivelink.sorting.domain.usecase.SortDriveLinks
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.extension.rootFolderId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.share.domain.usecase.GetMainShare
import me.proton.core.drive.sorting.domain.entity.By
import me.proton.core.drive.sorting.domain.usecase.GetSorting
import me.proton.core.util.kotlin.exhaustive
import javax.inject.Inject

class GetPagedDriveLinksList @Inject constructor(
    private val getMainShare: GetMainShare,
    private val getPagedDriveLinks: GetPagedDriveLinks,
    private val getDecryptedDriveLinks: GetDecryptedDriveLinks,
    private val getFolderChildrenDriveLinks: GetFolderChildrenDriveLinks,
    private val fetchDriveLinksListPage: FetchDriveLinksListPage,
    private val getSorting: GetSorting,
    private val sortDriveLinks: SortDriveLinks,
    private val decryptDriveLinks: DecryptDriveLinks,
) {

    operator fun invoke(userId: UserId) = getMainShare(userId)
        .transform { result ->
            when (result) {
                is DataResult.Processing -> Unit
                is DataResult.Success -> emitAll(
                    invoke(result.value.rootFolderId)
                )
                is DataResult.Error -> emit(PagingData.empty())
            }.exhaustive
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(folderId: FolderId) =
        getSorting(folderId.userId).flatMapLatest { sorting ->
            getPagedDriveLinks(
                userId = folderId.userId,
                pagedListKey = "LIST-${folderId.shareId.id}-${folderId.id}",
                remoteDriveLinks = { page, pageSize ->
                    fetchDriveLinksListPage(
                        sorting,
                        folderId,
                        page,
                        pageSize
                    )
                },
                localDriveLinks = {
                    if (sorting.by == By.NAME || sorting.by == By.LAST_MODIFIED) {
                        getDecryptedDriveLinks(folderId)
                            .mapCatching { driveLinks ->
                                sortDriveLinks(sorting, driveLinks)
                            }
                    } else {
                        getFolderChildrenDriveLinks(folderId)
                            .mapCatching { driveLinks ->
                                sortDriveLinks(sorting, driveLinks)
                            }
                    }
                },
                processPage = takeIf { sorting.by != By.NAME && sorting.by != By.LAST_MODIFIED }?.let {
                    { page -> decryptDriveLinks(page) }
                }
            )
        }
}
