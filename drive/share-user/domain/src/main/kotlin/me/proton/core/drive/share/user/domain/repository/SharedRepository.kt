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

package me.proton.core.drive.share.user.domain.repository

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.SaveAction
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.share.user.domain.entity.ShareTargetType
import me.proton.core.drive.share.user.domain.entity.SharedLinkId
import me.proton.core.drive.share.user.domain.entity.SharedListing
import me.proton.core.drive.volume.domain.entity.VolumeId

interface SharedRepository {

    suspend fun fetchSharedWithMeListing(
        userId: UserId,
        anchorId: String? = null,
    ): Pair<SharedListing, SaveAction>

    suspend fun fetchAndStoreSharedWithMeListing(
        userId: UserId,
        anchorId: String? = null,
    ): SharedListing

    suspend fun getSharedByMeListing(userId: UserId, index: Int, count: Int): List<SharedLinkId>

    suspend fun getSharedWithMeListing(
        userId: UserId,
        types: Set<ShareTargetType>,
        index: Int,
        count: Int,
    ): List<SharedLinkId>

    suspend fun deleteAllLocalSharedWithMe(userId: UserId)

    suspend fun deleteLocalSharedWithMe(volumeId: VolumeId, linkId: LinkId)

    suspend fun fetchSharedByMeListing(
        userId: UserId,
        volumeId: VolumeId,
        anchorId: String? = null,
    ): Pair<SharedListing, SaveAction>

    suspend fun fetchAndStoreSharedByMeListing(
        userId: UserId,
        volumeId: VolumeId,
        anchorId: String? = null,
    ): SharedListing

    suspend fun deleteAllLocalSharedByMe(userId: UserId)

    suspend fun getSaveAction(sharedListing: SharedListing): SaveAction
}
