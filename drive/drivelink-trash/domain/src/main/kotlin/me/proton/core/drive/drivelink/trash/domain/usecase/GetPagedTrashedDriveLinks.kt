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
import me.proton.core.drive.drivelink.crypto.domain.usecase.DecryptDriveLinks
import me.proton.core.drive.drivelink.paged.domain.entity.LinksPage
import me.proton.core.drive.drivelink.paged.domain.usecase.GetPagedDriveLinks
import me.proton.core.drive.drivelink.sorting.domain.usecase.SortDriveLinks
import me.proton.core.drive.share.domain.usecase.GetMainShare
import me.proton.core.drive.sorting.domain.entity.By
import me.proton.core.drive.sorting.domain.usecase.GetSorting
import me.proton.core.drive.trash.domain.repository.DriveTrashRepository
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.util.kotlin.exhaustive
import javax.inject.Inject

class GetPagedTrashedDriveLinks @Inject constructor(
    private val getMainShare: GetMainShare,
    private val getPagedDriveLinks: GetPagedDriveLinks,
    private val getDecryptedTrashedDriveLinks: GetDecryptedTrashedDriveLinks,
    private val getTrashedDriveLinks: GetTrashedDriveLinks,
    private val getTrashedDriveLinksCount: GetTrashedDriveLinksCount,
    private val decryptDriveLinks: DecryptDriveLinks,
    private val trashRepository: DriveTrashRepository,
    private val getSorting: GetSorting,
    private val sortDriveLinks: SortDriveLinks,
) {

    operator fun invoke(userId: UserId) = getMainShare(userId)
        .transform { result ->
            when (result) {
                is DataResult.Processing -> Unit
                is DataResult.Success -> emitAll(invoke(userId, result.value.volumeId))
                is DataResult.Error -> emit(PagingData.empty())
            }.exhaustive
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(userId: UserId, volumeId: VolumeId) =
        getSorting(userId).flatMapLatest { sorting ->
            getPagedDriveLinks(
                userId = userId,
                pagedListKey = "TRASH_${userId.id}_${volumeId.id}",
                remoteDriveLinks = { pageIndex, pageSize ->
                    trashRepository.fetchTrashContent(userId, volumeId, pageIndex, pageSize)
                        .map { (links, onSaveAction) ->
                            LinksPage(links, onSaveAction)
                        }
                },
                localPagedDriveLinks = { fromIndex, count ->
                    if (sorting.by == By.NAME || sorting.by == By.LAST_MODIFIED) {
                        getDecryptedTrashedDriveLinks(userId, volumeId, fromIndex, count)
                            .mapCatching { driveLinks ->
                                sortDriveLinks(sorting, driveLinks)
                            }
                    } else {
                        getTrashedDriveLinks(userId, volumeId, fromIndex, count)
                            .mapCatching { driveLinks ->
                                sortDriveLinks(sorting, driveLinks)
                            }
                    }
                },
                localDriveLinksCount = { getTrashedDriveLinksCount(userId, volumeId) },
                processPage = takeIf { sorting.by != By.NAME && sorting.by != By.LAST_MODIFIED }?.let {
                    { page -> decryptDriveLinks(page) }
                },
            )
        }
}
