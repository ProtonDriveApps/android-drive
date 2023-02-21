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
package me.proton.core.drive.share.data.api

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.share.data.api.response.shareDto
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.share.domain.entity.ShareInfo
import me.proton.core.drive.share.data.extension.toCreateShareRequest
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.network.data.ApiProvider
import me.proton.core.network.domain.ApiException

class ShareApiDataSource(private val apiProvider: ApiProvider) {
    @Throws(ApiException::class)
    suspend fun getShares(userId: UserId): List<ShareDto> =
        apiProvider.get<ShareApi>(userId).invoke { getShares() }.valueOrThrow.shareDtos

    @Throws(ApiException::class)
    suspend fun getShareBootstrap(shareId: ShareId): ShareDto =
        apiProvider.get<ShareApi>(shareId.userId).invoke {
            getShareBootstrap(shareId.id)
        }.valueOrThrow.shareDto

    @Throws(ApiException::class)
    suspend fun deleteShare(shareId: ShareId) =
        apiProvider.get<ShareApi>(shareId.userId).invoke { deleteShare(shareId.id) }.valueOrThrow

    @Throws(ApiException::class)
    suspend fun createShare(userId: UserId, volumeId: VolumeId, shareInfo: ShareInfo) =
        apiProvider.get<ShareApi>(userId).invoke {
            createShare(volumeId.id, shareInfo.toCreateShareRequest())
        }.valueOrThrow.shareId.id
}
