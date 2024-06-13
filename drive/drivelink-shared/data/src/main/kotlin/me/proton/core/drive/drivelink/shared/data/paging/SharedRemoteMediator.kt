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

package me.proton.core.drive.drivelink.shared.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import me.proton.core.drive.base.domain.entity.SaveAction
import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.drivelink.shared.data.db.DriveLinkSharedDatabase
import me.proton.core.drive.drivelink.shared.data.db.entity.SharedRemoteKeyEntity
import me.proton.core.drive.share.user.data.db.entity.SharedByMeListingEntity
import me.proton.core.drive.share.user.data.db.entity.SharedWithMeListingEntity
import me.proton.core.drive.share.user.domain.entity.SharedListing
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject

@OptIn(ExperimentalPagingApi::class)
class SharedRemoteMediator<T : Any> @Inject constructor(
    private val pagedListKey: PagedListKey,
    private val fetchSharedListing: suspend (String?) -> Result<Pair<SharedListing, SaveAction>>,
    private val deleteAllLocalSharedListing: suspend () -> Result<Unit>,
    private val database: DriveLinkSharedDatabase,
) : RemoteMediator<Int, T>() {
    private val remoteKeyDao = database.sharedRemoteKeyDao

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, T>,
    ): MediatorResult {
        return try {
            CoreLogger.i(
                tag = LogTag.PAGING,
                message = "Shared ${pagedListKey.log} remote mediator starts loading (type = ${loadType.name})",
            )
            val pageKey = when (loadType) {
                LoadType.REFRESH -> null
                LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
                LoadType.APPEND -> getLastRemoteKey()?.let { remoteKey ->
                    if (!remoteKey.hasMore) {
                        return MediatorResult.Success(endOfPaginationReached = true)
                    } else {
                        remoteKey.anchorId
                    }
                }
            }
            val (sharedListing, saveAction) = fetchSharedListing(pageKey)
                .onFailure { throwable ->
                    CoreLogger.d(LogTag.PAGING, throwable, "Fetching shared ${pagedListKey.log} failed")
                    return MediatorResult.Error(throwable)
                }
                .getOrThrow()
            CoreLogger.i(
                tag = LogTag.PAGING,
                message = "Shared ${pagedListKey.log} remote mediator loaded ${sharedListing.linkIds.size} items",
            )
            val endOfPaginationReached = sharedListing.hasMore.not()
            database.inTransaction {
                if (loadType == LoadType.REFRESH) {
                    remoteKeyDao.deleteKeys(pagedListKey.id)
                    deleteAllLocalSharedListing().getOrNull(
                        tag = LogTag.PAGING,
                        message = "Shared ${pagedListKey.log} remote mediator failed deleting shared with me from database",
                    )
                }
                saveAction()
                remoteKeyDao.insertOrIgnore(
                    SharedRemoteKeyEntity(
                        id = 0,
                        key = pagedListKey.id,
                        anchorId = sharedListing.requireAnchorId(),
                        hasMore = sharedListing.hasMore,
                    )
                )
            }
            MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
        } catch (error: Exception) {
            CoreLogger.d(LogTag.PAGING, error, "Shared ${pagedListKey.log} remote mediator loading failed")
            MediatorResult.Error(error)
        }
    }

    private suspend fun getLastRemoteKey(): SharedRemoteKeyEntity? =
        remoteKeyDao.getLastRemoteKey(pagedListKey.id).also { lastRemoteKey ->
            CoreLogger.d(
                LogTag.PAGING,
                "last db remote key anchorId=${lastRemoteKey?.anchorId} hasMore=${lastRemoteKey?.hasMore}"
            )
        }

    private fun SharedListing.requireAnchorId(): String = if (hasMore) {
        requireNotNull(anchorId)
    } else {
        anchorId.orEmpty()
    }

    enum class PagedListKey {
        SHARED_WITH_ME,
        SHARED_BY_ME,
    }

    private val PagedListKey.log: String get() = when (this) {
        PagedListKey.SHARED_WITH_ME -> LOG_SHARED_WITH_ME
        PagedListKey.SHARED_BY_ME -> LOG_SHARED_BY_ME
    }

    private val PagedListKey.id: String get() = when (this) {
        PagedListKey.SHARED_WITH_ME -> PAGED_LIST_KEY_SHARED_WITH_ME
        PagedListKey.SHARED_BY_ME -> PAGED_LIST_KEY_SHARED_BY_ME
    }

    companion object {
        private const val LOG_SHARED_WITH_ME = "with me"
        private const val LOG_SHARED_BY_ME = "by me"
        private const val PAGED_LIST_KEY_SHARED_WITH_ME = "SHARED_WITH_ME_LISTING"
        private const val PAGED_LIST_KEY_SHARED_BY_ME = "SHARED_BY_ME_LISTING"
    }
}
