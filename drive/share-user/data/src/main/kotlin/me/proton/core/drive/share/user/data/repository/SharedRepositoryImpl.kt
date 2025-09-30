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

package me.proton.core.drive.share.user.data.repository

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.SaveAction
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.share.user.data.api.SharedApiDataSource
import me.proton.core.drive.share.user.data.db.ShareUserDatabase
import me.proton.core.drive.share.user.data.db.entity.SharedWithMeListingEntity
import me.proton.core.drive.share.user.data.extension.toShareTargetTypeDtos
import me.proton.core.drive.share.user.data.extension.toSharedByMeListingEntity
import me.proton.core.drive.share.user.data.extension.toSharedLinkId
import me.proton.core.drive.share.user.data.extension.toSharedListing
import me.proton.core.drive.share.user.data.extension.toSharedWithMeListingEntity
import me.proton.core.drive.share.user.domain.entity.ShareTargetType
import me.proton.core.drive.share.user.domain.entity.SharedLinkId
import me.proton.core.drive.share.user.domain.entity.SharedListing
import me.proton.core.drive.share.user.domain.repository.SharedRepository
import me.proton.core.drive.volume.domain.entity.VolumeId
import javax.inject.Inject

class SharedRepositoryImpl @Inject constructor(
    private val api: SharedApiDataSource,
    private val db: ShareUserDatabase,
) : SharedRepository {

    override suspend fun fetchSharedWithMeListing(userId: UserId, anchorId: String?): Pair<SharedListing, SaveAction> =
        api.getSharedWithMeListings(userId, anchorId)
            .toSharedListing(userId)
            .let { sharedListing ->
                sharedListing to SaveAction {
                    db.sharedWithMeListingDao.insertOrIgnore(
                        *sharedListing.linkIds.map { sharedLinkId ->
                            sharedLinkId.toSharedWithMeListingEntity()
                        }.toTypedArray()
                    )
                }
            }

    override suspend fun fetchAndStoreSharedWithMeListing(userId: UserId, anchorId: String?): SharedListing =
        api.getSharedWithMeListings(userId, anchorId)
            .toSharedListing(userId).also { sharedListing ->
                db.sharedWithMeListingDao.insertOrIgnore(
                    *sharedListing.linkIds.map { sharedLinkId ->
                        sharedLinkId.toSharedWithMeListingEntity()
                    }.toTypedArray()
                )
            }

    override suspend fun getSharedByMeListing(
        userId: UserId,
        index: Int,
        count: Int
    ): List<SharedLinkId> = db.sharedByMeListingDao.getSharedByMeListing(
        userId = userId,
        limit = count,
        offset = index,
    ).map { sharedByMeListingEntity ->
        sharedByMeListingEntity.toSharedLinkId()
    }

    override suspend fun getSharedByMeListingCount(userId: UserId): Int =
        db.sharedByMeListingDao.getSharedByMeListingCount(userId)

    override suspend fun getSharedWithMeListing(
        userId: UserId,
        types: Set<ShareTargetType>,
        index: Int,
        count: Int
    ): List<SharedLinkId> = db.sharedWithMeListingDao.getSharedWithMeListing(
        userId = userId,
        types = types.toShareTargetTypeDtos(),
        includeNullType = types.contains(ShareTargetType.Album).not(),
        limit = count,
        offset = index,
    ).map { sharedWithMeListingEntity ->
        sharedWithMeListingEntity.toSharedLinkId()
    }

    override suspend fun getSharedWithMeListingCount(
        userId: UserId,
        types: Set<ShareTargetType>,
    ): Int = db.sharedWithMeListingDao.getSharedWithMeListingCount(
        userId = userId,
        types = types.toShareTargetTypeDtos(),
        includeNullType = types.contains(ShareTargetType.Album).not(),
    )

    override suspend fun deleteAllLocalSharedWithMe(userId: UserId) =
        db.sharedWithMeListingDao.deleteAll(userId)

    override suspend fun deleteLocalSharedWithMe(volumeId: VolumeId, linkId: LinkId) =
        db.sharedWithMeListingDao.delete(
            SharedWithMeListingEntity(
                userId = linkId.userId,
                volumeId = volumeId.id,
                shareId = linkId.shareId.id,
                linkId = linkId.id,
            )
        )

    override suspend fun fetchSharedByMeListing(
        userId: UserId,
        volumeId: VolumeId,
        anchorId: String?,
    ): Pair<SharedListing, SaveAction> =
        api.getSharedByMeListings(userId, volumeId, anchorId)
            .toSharedListing(userId, volumeId)
            .let { sharedListing ->
                sharedListing to SaveAction {
                    db.sharedByMeListingDao.insertOrIgnore(
                        *sharedListing.linkIds.map { sharedLinkId ->
                            sharedLinkId.toSharedByMeListingEntity()
                        }.toTypedArray()
                    )
                }
            }

    override suspend fun fetchAndStoreSharedByMeListing(
        userId: UserId,
        volumeId: VolumeId,
        anchorId: String?,
    ): SharedListing =
        api.getSharedByMeListings(userId, volumeId, anchorId)
            .toSharedListing(userId, volumeId).also { sharedListing ->
                db.sharedByMeListingDao.insertOrIgnore(
                    *sharedListing.linkIds.map { sharedLinkId ->
                        sharedLinkId.toSharedByMeListingEntity()
                    }.toTypedArray()
                )
            }

    override suspend fun deleteAllLocalSharedByMe(userId: UserId) =
        db.sharedByMeListingDao.deleteAll(userId)

    override suspend fun getSaveAction(sharedListing: SharedListing): SaveAction =
        SaveAction {
            db.sharedByMeListingDao.insertOrIgnore(
                *sharedListing.linkIds.map { sharedLinkId ->
                    sharedLinkId.toSharedByMeListingEntity()
                }.toTypedArray()
            )
        }
}
