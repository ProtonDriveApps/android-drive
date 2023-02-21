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

package me.proton.core.drive.drivelink.paged.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.log.logId
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.paged.data.db.DriveLinkPagedDatabase
import me.proton.core.drive.drivelink.paged.data.db.entity.DriveLinkRemoteKeyEntity
import me.proton.core.drive.drivelink.paged.domain.entity.LinksPage
import me.proton.core.network.domain.ApiException
import me.proton.core.util.kotlin.CoreLogger

@OptIn(ExperimentalPagingApi::class)
class DriveLinkRemoteMediator(
    private val userId: UserId,
    private val pagedListKey: String,
    private val database: DriveLinkPagedDatabase,
    private val remoteLinks: suspend (page: Int, pageSize: Int) -> Result<LinksPage>,
) : RemoteMediator<Int, DriveLink>() {

    private val dao = database.driveLinkRemoteKeyDao

    override suspend fun initialize(): InitializeAction =
        InitializeAction.SKIP_INITIAL_REFRESH

    override suspend fun load(loadType: LoadType, state: PagingState<Int, DriveLink>): MediatorResult {
        return try {
            CoreLogger.d(LogTag.PAGING, loadType.name)
            val pageIndex = when (loadType) {
                LoadType.REFRESH -> getClosestRemoteKey(state)?.nextKey?.let { pageIndex ->
                    pageIndex - 1
                } ?: 0
                LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
                LoadType.APPEND -> getLastRemoteKey()?.let { remoteKey ->
                    remoteKey.nextKey ?: return MediatorResult.Success(endOfPaginationReached = true)
                } ?: state.pages
                    .lastOrNull { page -> page.data.size == state.config.pageSize }
                    ?.let { page ->
                        page.prevKey?.plus(2) ?: 1
                    } ?: 0
            }
            CoreLogger.d(LogTag.PAGING, "page index $pageIndex size ${state.config.pageSize}")
            val (links, saveAction) = remoteLinks(pageIndex, state.config.pageSize)
                .onFailure { throwable ->
                    CoreLogger.d(LogTag.PAGING, throwable, "Getting remote links failed")
                    return MediatorResult.Error(throwable)
                }
                .getOrThrow()
            val endOfPaginationReached = links.size < state.config.pageSize || links.size % state.config.pageSize > 0
            CoreLogger.d(LogTag.PAGING, "loaded links (${links.size})")
            val previousPageIndex = if (pageIndex == 0) null else pageIndex - 1
            val nextPageIndex = if (endOfPaginationReached) null else pageIndex + 1
            CoreLogger.d(LogTag.PAGING, "pageIndex ($pageIndex) nextPageIndex ($nextPageIndex)")
            val remoteKeys = links.map { encryptedLinkEntity ->
                DriveLinkRemoteKeyEntity(
                    key = pagedListKey,
                    shareId = encryptedLinkEntity.id.shareId.id,
                    linkId = encryptedLinkEntity.id.id,
                    userId = userId,
                    prevKey = previousPageIndex,
                    nextKey = nextPageIndex
                )
            }
            database.inTransaction {
                saveAction()
                if (loadType == LoadType.REFRESH) {
                    dao.deleteKeys(userId, pagedListKey)
                }
                dao.insertOrUpdate(*remoteKeys.toTypedArray())
            }
            MediatorResult.Success(endOfPaginationReached)
        } catch (e: ApiException) {
            CoreLogger.d(LogTag.PAGING, e, e.message.orEmpty())
            MediatorResult.Error(e)
        }
    }

    private suspend fun getClosestRemoteKey(state: PagingState<Int, DriveLink>): DriveLinkRemoteKeyEntity? {
        CoreLogger.d(LogTag.PAGING, "anchorPosition ${state.anchorPosition}")
        return state.anchorPosition?.let { position ->
            state.closestItemToPosition(position)?.let { linkId ->
                dao.getLinkRemoteKey(pagedListKey, linkId.id)
            }
        }
    }

    private suspend fun getLastRemoteKey(): DriveLinkRemoteKeyEntity? {
        val lastRemoteKey = dao.getLastRemoteKey(userId, pagedListKey)
        CoreLogger.d(
            LogTag.PAGING,
            "last db remote key ${lastRemoteKey?.linkId?.logId()} - ${lastRemoteKey?.shareId?.logId()}"
        )
        return lastRemoteKey
    }
}
