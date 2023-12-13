/*
 * Copyright (c) 2023 Proton AG.
 * This file is part of Proton Drive.
 *
 * Proton Drive is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Drive is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Drive.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.android.drive.photos.domain.usecase

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.transform
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.extension.mapCatching
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.usecase.GetDriveLinksCount
import me.proton.core.drive.drivelink.list.domain.usecase.FetchDriveLinksListPage
import me.proton.core.drive.drivelink.list.domain.usecase.GetDecryptedDriveLinks
import me.proton.core.drive.drivelink.paged.domain.usecase.GetPagedDriveLinks
import me.proton.core.drive.drivelink.sorting.domain.usecase.SortDriveLinks
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.extension.rootFolderId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.share.crypto.domain.usecase.GetOrCreatePhotoShare
import me.proton.core.drive.sorting.domain.entity.By
import me.proton.core.drive.sorting.domain.entity.Direction
import me.proton.core.drive.sorting.domain.entity.Sorting
import me.proton.core.util.kotlin.exhaustive
import javax.inject.Inject

class GetPagedDriveLinksList @Inject constructor(
    private val getPhotoShare: GetOrCreatePhotoShare,
    private val getPagedDriveLinks: GetPagedDriveLinks,
    private val getDecryptedDriveLinks: GetDecryptedDriveLinks,
    private val getDriveLinksCount: GetDriveLinksCount,
    private val fetchDriveLinksListPage: FetchDriveLinksListPage,
    private val sortDriveLinks: SortDriveLinks,
) {

    operator fun invoke(userId: UserId) = getPhotoShare(userId)
        .transform { result ->
            when (result) {
                is DataResult.Processing -> Unit
                is DataResult.Success -> emitAll(
                    invoke(result.value.rootFolderId)
                )
                is DataResult.Error -> emit(PagingData.empty())
            }.exhaustive
        }

    operator fun invoke(folderId: FolderId): Flow<PagingData<DriveLink>> {
        val sorting = Sorting(By.LAST_MODIFIED, Direction.DESCENDING)
        return getPagedDriveLinks(
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
            localPagedDriveLinks = { fromIndex, count ->
                getDecryptedDriveLinks(folderId, fromIndex, count)
                    .mapCatching { driveLinks ->
                        sortDriveLinks(sorting, driveLinks)
                    }
            },
            localDriveLinksCount = { getDriveLinksCount(parentId = folderId) },
            processPage = null,
        )
    }
}
