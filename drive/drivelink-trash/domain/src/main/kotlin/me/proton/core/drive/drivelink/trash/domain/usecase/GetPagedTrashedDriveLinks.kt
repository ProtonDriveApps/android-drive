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

package me.proton.core.drive.drivelink.trash.domain.usecase

import androidx.paging.PagingData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.transform
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.extension.mapCatching
import me.proton.core.drive.drivelink.paged.domain.entity.LinksPage
import me.proton.core.drive.drivelink.paged.domain.usecase.GetPagedDriveLinks
import me.proton.core.drive.drivelink.sorting.domain.usecase.SortDriveLinks
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.share.domain.usecase.GetMainShare
import me.proton.core.drive.sorting.domain.usecase.GetSorting
import me.proton.core.drive.trash.domain.repository.DriveTrashRepository
import me.proton.core.util.kotlin.exhaustive
import javax.inject.Inject

class GetPagedTrashedDriveLinks @Inject constructor(
    private val getMainShare: GetMainShare,
    private val getPagedDriveLinks: GetPagedDriveLinks,
    private val getDecryptedTrashedDriveLinks: GetDecryptedTrashedDriveLinks,
    private val trashRepository: DriveTrashRepository,
    private val getSorting: GetSorting,
    private val sortDriveLinks: SortDriveLinks,
) {

    operator fun invoke(userId: UserId) = getMainShare(userId)
        .transform { result ->
            when (result) {
                is DataResult.Processing -> Unit
                is DataResult.Success -> emitAll(invoke(result.value.id))
                is DataResult.Error -> emit(PagingData.empty())
            }.exhaustive
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(shareId: ShareId) =
        getSorting(shareId.userId).flatMapLatest { sorting ->
            getPagedDriveLinks(
                userId = shareId.userId,
                pagedListKey = "TRASH_${shareId.userId.id}_${shareId.id}",
                remoteDriveLinks = { page, pageSize ->
                    trashRepository.fetchTrashContent(shareId, page, pageSize).map { (links, onSaveAction) ->
                        LinksPage(links, onSaveAction)
                    }
                },
                localDriveLinks = {
                    getDecryptedTrashedDriveLinks(shareId)
                        .mapCatching { driveLinks ->
                            sortDriveLinks(sorting, driveLinks)
                        }
                }
            )
        }
}
