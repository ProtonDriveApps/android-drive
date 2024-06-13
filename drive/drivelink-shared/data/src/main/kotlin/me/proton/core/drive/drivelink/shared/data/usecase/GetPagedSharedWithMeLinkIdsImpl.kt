/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.core.drive.drivelink.shared.data.usecase

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.drivelink.shared.data.db.DriveLinkSharedDatabase
import me.proton.core.drive.drivelink.shared.data.paging.SharedRemoteMediator
import me.proton.core.drive.drivelink.shared.domain.usecase.GetPagedSharedWithMeLinkIds
import me.proton.core.drive.share.user.data.db.ShareUserDatabase
import me.proton.core.drive.share.user.data.extension.toSharedLinkId
import me.proton.core.drive.share.user.domain.entity.SharedLinkId
import me.proton.core.drive.share.user.domain.usecase.DeleteAllLocalSharedWithMe
import me.proton.core.drive.share.user.domain.usecase.FetchSharedWithMe
import javax.inject.Inject

@OptIn(ExperimentalPagingApi::class)
class GetPagedSharedWithMeLinkIdsImpl @Inject constructor(
    private val shareUserDatabase: ShareUserDatabase,
    private val configurationProvider: ConfigurationProvider,
    private val fetchSharedWithMe: FetchSharedWithMe,
    private val deleteAllLocalSharedWithMe: DeleteAllLocalSharedWithMe,
    private val driveLinkSharedDatabase: DriveLinkSharedDatabase,
) : GetPagedSharedWithMeLinkIds {

    override operator fun invoke(userId: UserId): Flow<PagingData<SharedLinkId>> =
        Pager(
            config = PagingConfig(
                pageSize = configurationProvider.dbPageSize,
                initialLoadSize = configurationProvider.dbPageSize,
                enablePlaceholders = false,
            ),
            remoteMediator = SharedRemoteMediator(
                pagedListKey = SharedRemoteMediator.PagedListKey.SHARED_WITH_ME,
                fetchSharedListing = { anchorId -> fetchSharedWithMe(userId, anchorId) },
                deleteAllLocalSharedListing = { deleteAllLocalSharedWithMe(userId) },
                database = driveLinkSharedDatabase,
            ),
            pagingSourceFactory = {
                shareUserDatabase.sharedWithMeListingDao.getSharedWithMeListingPagingSource(userId)
            }
        )
            .flow
            .map { pagingData ->
                pagingData.map { sharedWithMeEntity -> sharedWithMeEntity.toSharedLinkId() }
            }
}
