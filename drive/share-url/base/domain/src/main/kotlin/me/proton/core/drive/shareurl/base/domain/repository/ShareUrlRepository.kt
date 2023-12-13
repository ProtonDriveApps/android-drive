/*
 * Copyright (c) 2022-2023 Proton AG.
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

package me.proton.core.drive.shareurl.base.domain.repository

import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.shareurl.base.domain.entity.ShareUrl
import me.proton.core.drive.shareurl.base.domain.entity.ShareUrlCustomPasswordInfo
import me.proton.core.drive.shareurl.base.domain.entity.ShareUrlExpirationDurationInfo
import me.proton.core.drive.shareurl.base.domain.entity.ShareUrlId
import me.proton.core.drive.shareurl.base.domain.entity.ShareUrlInfo
import me.proton.core.drive.volume.domain.entity.VolumeId

interface ShareUrlRepository {

    /**
     * Returns true if the cache has at least one share url for the given share url
     */
    suspend fun hasShareUrl(shareUrlId: ShareUrlId): Boolean

    /**
     * Fetch the given page share url
     */
    suspend fun fetchShareUrl(volumeId: VolumeId, shareUrlId: ShareUrlId): Result<ShareUrl>

    /**
     * Get reactive share url
     */
    fun getShareUrl(shareUrlId: ShareUrlId): Flow<ShareUrl?>

    /**
     * Get share url
     */
    suspend fun getShareUrl(userId: UserId, shareUrlId: String): ShareUrl?

    /**
     * Returns true if the cache has at least one share url for the given volume
     */
    suspend fun hasShareUrls(userId: UserId, volumeId: VolumeId): Boolean

    /**
     * Fetch all share urls for a given volume
     */
    suspend fun fetchAllShareUrls(userId: UserId, volumeId: VolumeId, saveLinks: Boolean): Result<List<ShareUrl>>

    /**
     * Get reactive list of all share urls for a given volume
     */
    fun getAllShareUrls(userId: UserId, volumeId: VolumeId): Flow<List<ShareUrl>>

    /**
     * Create a share URL for a given share
     */
    suspend fun createShareUrl(
        volumeId: VolumeId,
        shareId: ShareId,
        shareUrlInfo: ShareUrlInfo,
    ): Result<ShareUrl>

    /**
     * Delete a given share URL
     */
    suspend fun deleteShareUrl(shareUrlId: ShareUrlId): Result<Unit>

    /**
     * Update share URL custom password and/or expiration duration
     */
    suspend fun updateShareUrl(
        volumeId: VolumeId,
        shareUrlId: ShareUrlId,
        shareUrlCustomPasswordInfo: ShareUrlCustomPasswordInfo?,
        shareUrlExpirationDurationInfo: ShareUrlExpirationDurationInfo?,
    ): Result<ShareUrl>
}
