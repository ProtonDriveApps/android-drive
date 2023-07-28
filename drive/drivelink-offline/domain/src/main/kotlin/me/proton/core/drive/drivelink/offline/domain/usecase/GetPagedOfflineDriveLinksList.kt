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

package me.proton.core.drive.drivelink.offline.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.SaveAction
import me.proton.core.drive.base.domain.extension.mapCatching
import me.proton.core.drive.drivelink.crypto.domain.usecase.DecryptDriveLinks
import me.proton.core.drive.drivelink.paged.domain.entity.LinksPage
import me.proton.core.drive.drivelink.paged.domain.usecase.GetPagedDriveLinks
import me.proton.core.drive.drivelink.sorting.domain.usecase.SortDriveLinks
import me.proton.core.drive.sorting.domain.entity.By
import me.proton.core.drive.sorting.domain.usecase.GetSorting
import javax.inject.Inject

class GetPagedOfflineDriveLinksList @Inject constructor(
    private val getSorting: GetSorting,
    private val getPagedDriveLinks: GetPagedDriveLinks,
    private val decryptDriveLinks: DecryptDriveLinks,
    private val sortDriveLinks: SortDriveLinks,
    private val getOfflineDriveLinksCount: GetOfflineDriveLinksCount,
    private val getDecryptedOfflineDriveLinks: GetDecryptedOfflineDriveLinks,
    private val getOfflineDriveLinks: GetOfflineDriveLinks,
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(userId: UserId) =
        getSorting(userId).flatMapLatest { sorting ->
            getPagedDriveLinks(
                userId = userId,
                pagedListKey = "OFFLINE-LIST-${userId.id}",
                remoteDriveLinks = { _, _ -> Result.success(LinksPage(emptyList(), SaveAction({}))) },
                localPagedDriveLinks = { fromIndex, count ->
                    if (sorting.by == By.NAME || sorting.by == By.LAST_MODIFIED) {
                        getDecryptedOfflineDriveLinks(userId, fromIndex, count)
                            .mapCatching { driveLinks ->
                                sortDriveLinks(sorting, driveLinks)
                            }
                    } else {
                        getOfflineDriveLinks(userId, fromIndex, count)
                            .mapCatching { driveLinks ->
                                sortDriveLinks(sorting, driveLinks)
                            }
                    }
                },
                localDriveLinksCount = { getOfflineDriveLinksCount(userId = userId) },
                processPage = takeIf { sorting.by != By.NAME && sorting.by != By.LAST_MODIFIED }?.let {
                    { page -> decryptDriveLinks(page) }
                },
            )
        }
}
