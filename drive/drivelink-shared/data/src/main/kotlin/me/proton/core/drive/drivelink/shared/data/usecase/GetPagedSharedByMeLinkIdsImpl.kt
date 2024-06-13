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
import me.proton.core.drive.drivelink.shared.domain.usecase.GetPagedSharedByMeLinkIds
import me.proton.core.drive.share.user.data.db.ShareUserDatabase
import me.proton.core.drive.share.user.data.extension.toSharedLinkId
import me.proton.core.drive.share.user.domain.entity.SharedLinkId
import me.proton.core.drive.share.user.domain.usecase.DeleteAllLocalSharedByMe
import me.proton.core.drive.share.user.domain.usecase.FetchSharedByMe
import me.proton.core.drive.volume.domain.entity.VolumeId
import javax.inject.Inject

@OptIn(ExperimentalPagingApi::class)
class GetPagedSharedByMeLinkIdsImpl @Inject constructor(
    private val configurationProvider: ConfigurationProvider,
    private val deleteAllLocalSharedByMe: DeleteAllLocalSharedByMe,
    private val driveLinkSharedDatabase: DriveLinkSharedDatabase,
    private val shareUserDatabase: ShareUserDatabase,
    private val fetchSharedByMe: FetchSharedByMe,
) : GetPagedSharedByMeLinkIds {

    override fun invoke(userId: UserId, volumeId: VolumeId): Flow<PagingData<SharedLinkId>> =
        Pager(
            config = PagingConfig(
                pageSize = configurationProvider.dbPageSize,
                initialLoadSize = configurationProvider.dbPageSize,
                enablePlaceholders = false,
            ),
            remoteMediator = SharedRemoteMediator(
                pagedListKey = SharedRemoteMediator.PagedListKey.SHARED_BY_ME,
                fetchSharedListing = { anchorId -> fetchSharedByMe(userId, volumeId, anchorId) },
                deleteAllLocalSharedListing = { deleteAllLocalSharedByMe(userId) },
                database = driveLinkSharedDatabase,
            ),
            pagingSourceFactory = {
                shareUserDatabase.sharedByMeListingDao.getSharedByMeListingPagingSource(userId)
            }
        )
            .flow
            .map { pagingData ->
                pagingData.map { sharedByMeEntity -> sharedByMeEntity.toSharedLinkId() }
            }
}
