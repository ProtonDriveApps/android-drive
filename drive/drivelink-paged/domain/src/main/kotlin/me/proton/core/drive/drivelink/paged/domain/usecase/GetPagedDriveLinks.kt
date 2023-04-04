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

package me.proton.core.drive.drivelink.paged.domain.usecase

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.db.paging.asPagingSource
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.paged.domain.entity.LinksPage
import me.proton.core.drive.drivelink.paged.domain.paging.DriveLinkRemoteMediatorFactory
import javax.inject.Inject

class GetPagedDriveLinks @Inject constructor(
    private val factory: DriveLinkRemoteMediatorFactory,
    private val configurationProvider: ConfigurationProvider,
    private val getObservablePageSize: GetObservablePageSize,
) {

    @OptIn(ExperimentalPagingApi::class)
    operator fun invoke(
        userId: UserId,
        pagedListKey: String,
        remoteDriveLinks: suspend (page: Int, pageSize: Int) -> Result<LinksPage>,
        localDriveLinks: () -> Flow<Result<List<DriveLink>>>,
        pageSize: Int = configurationProvider.uiPageSize,
        processPage: (suspend (List<DriveLink>) -> List<DriveLink>)? = null,
    ): Flow<PagingData<DriveLink>> {
        return Pager(
            PagingConfig(
                pageSize = pageSize,
                initialLoadSize = pageSize,
                enablePlaceholders = false,
            ),
            remoteMediator = factory.create(userId, pagedListKey, remoteDriveLinks),
            pagingSourceFactory = { localDriveLinks().asPagingSource(processPage = processPage) }
        ).flow
    }

    @OptIn(ExperimentalPagingApi::class)
    operator fun invoke(
        userId: UserId,
        pagedListKey: String,
        remoteDriveLinks: suspend (page: Int, pageSize: Int) -> Result<LinksPage>,
        localPagedDriveLinks: (Int, Int) -> Flow<Result<List<DriveLink>>>,
        localDriveLinksCount: () -> Flow<Int>,
        pageSize: Int = configurationProvider.uiPageSize,
        processPage: (suspend (List<DriveLink>) -> List<DriveLink>)? = null,
    ): Flow<PagingData<DriveLink>> {
        return Pager(
            PagingConfig(
                pageSize = pageSize,
                initialLoadSize = pageSize,
                enablePlaceholders = false,
            ),
            remoteMediator = factory.create(userId, pagedListKey, remoteDriveLinks),
            pagingSourceFactory = {
                { fromIndex: Int, count: Int ->
                    localPagedDriveLinks(fromIndex, count)
                }.asPagingSource(
                    sourceSize = localDriveLinksCount(),
                    observablePageSize = getObservablePageSize(),
                    processPage = processPage,
                )
            }
        ).flow
    }
}
