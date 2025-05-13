/*
 * Copyright (c) 2025 Proton AG.
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

package me.proton.core.drive.drivelink.photo.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.log.logId
import me.proton.core.drive.drivelink.photo.data.db.DriveLinkPhotoDatabase
import me.proton.core.drive.drivelink.photo.data.db.entity.TaggedPhotoListingRemoteKeyEntity
import me.proton.core.drive.drivelink.photo.domain.entity.PhotoListingsPage
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.photo.domain.entity.PhotoListing
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.network.domain.ApiException
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject

@OptIn(ExperimentalPagingApi::class)
class TaggedPhotoListingRemoteMediator @Inject constructor(
    private val volumeId: VolumeId,
    private val pagedListKey: String,
    private val database: DriveLinkPhotoDatabase,
    private val remotePhotoListings: suspend (pageKey: String?, pageSize: Int) -> Result<PhotoListingsPage>,
    private val deleteAllLocalPhotoListings: suspend () -> Result<Unit>,
) : RemoteMediator<Int, PhotoListing>() {
    private val remoteKeyDao = database.taggedPhotoListingRemoteKeyDao

    override suspend fun initialize(): InitializeAction =
        InitializeAction.SKIP_INITIAL_REFRESH

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, PhotoListing>,
    ): MediatorResult {
        return try {
            CoreLogger.d(LogTag.PAGING, "Remote ${loadType.name}")
            val pageKey = when (loadType) {
                LoadType.REFRESH -> null
                LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
                LoadType.APPEND -> getLastRemoteKey()?.let { remoteKey ->
                    remoteKey.nextKey ?: return MediatorResult.Success(endOfPaginationReached = true)
                } ?: state.pages
                    .lastOrNull { page -> page.data.size == state.config.pageSize }
                    ?.data?.lastOrNull()?.linkId?.id
            }
            val (photoListings, saveAction) = remotePhotoListings(pageKey, state.config.pageSize)
                .onFailure { throwable ->
                    CoreLogger.d(LogTag.PAGING, throwable, "Getting remote photo listings failed")
                    return MediatorResult.Error(throwable)
                }
                .getOrThrow()
            val endOfPaginationReached = photoListings.size < state.config.pageSize ||
                    photoListings.size % state.config.pageSize > 0
            CoreLogger.d(LogTag.PAGING, "loaded photo listings (${photoListings.size})")
            val nextPageKey = if (endOfPaginationReached) {
                null
            } else {
                photoListings.lastOrNull()?.linkId?.id
            }
            CoreLogger.d(LogTag.PAGING, "pageKey ($pageKey) nextPageKey ($nextPageKey)")
            val remoteKeys = photoListings.map { photoListing: PhotoListing ->
                TaggedPhotoListingRemoteKeyEntity(
                    key = pagedListKey,
                    userId = photoListing.linkId.userId,
                    volumeId = volumeId.id,
                    shareId = photoListing.linkId.shareId.id,
                    tag = requireNotNull(photoListing.tag).value,
                    linkId = photoListing.linkId.id,
                    captureTime = photoListing.captureTime.value,
                    prevKey = pageKey,
                    nextKey = nextPageKey,
                )
            }
            database.inTransaction {
                if (loadType == LoadType.REFRESH) {
                    remoteKeyDao.deleteKeys(pagedListKey, volumeId.id)
                    deleteAllLocalPhotoListings()
                }

                saveAction()
                remoteKeyDao.insertOrUpdate(*remoteKeys.toTypedArray())
            }
            MediatorResult.Success(endOfPaginationReached)
        } catch (e: ApiException) {
            CoreLogger.d(LogTag.PAGING, e, e.message.orEmpty())
            MediatorResult.Error(e)
        }
    }

    private suspend fun getLastRemoteKey(): TaggedPhotoListingRemoteKeyEntity? {
        val lastRemoteKey = remoteKeyDao.getLastRemoteKey(pagedListKey, volumeId.id)
        CoreLogger.d(
            LogTag.PAGING,
            "last db remote key ${lastRemoteKey?.linkId?.logId()} - ${lastRemoteKey?.shareId?.logId()}"
        )
        return lastRemoteKey
    }
}
