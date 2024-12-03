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

package me.proton.core.drive.shareurl.base.data.api

import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.shareurl.base.data.api.request.DeleteShareUrlsRequest
import me.proton.core.drive.shareurl.base.data.api.request.UpdateCustomPasswordShareUrlRequest
import me.proton.core.drive.shareurl.base.data.api.request.UpdateExpirationDurationShareUrlRequest
import me.proton.core.drive.shareurl.base.data.api.request.UpdatePermissionsShareUrlRequest
import me.proton.core.drive.shareurl.base.data.api.request.UpdateShareUrlRequest
import me.proton.core.drive.shareurl.base.data.extension.toRequest
import me.proton.core.drive.shareurl.base.domain.entity.ShareUrlCustomPasswordInfo
import me.proton.core.drive.shareurl.base.domain.entity.ShareUrlExpirationDurationInfo
import me.proton.core.drive.shareurl.base.domain.entity.ShareUrlId
import me.proton.core.drive.shareurl.base.domain.entity.ShareUrlInfo
import me.proton.core.drive.shareurl.base.domain.extension.userId
import me.proton.core.drive.volume.data.api.entity.ShareUrlDto
import me.proton.core.network.data.ApiProvider
import me.proton.core.network.domain.ApiException
import me.proton.core.network.domain.ApiResult
import javax.inject.Inject

class ShareUrlApiDataSource @Inject constructor(
    private val apiProvider: ApiProvider,
    private val configurationProvider: ConfigurationProvider,
) {

    @Throws(ApiException::class)
    suspend fun getShareUrl(
        shareUrlId: ShareUrlId,
    ): ShareUrlDto {
        var page = 0
        do {
            val results = apiProvider.get<ShareUrlApi>(shareUrlId.userId).invoke {
                getAllShareUrls(shareUrlId.shareId.id, page++, configurationProvider.uiPageSize, recursive = 0)
            }.valueOrThrow
            results.shareUrlDtos.firstOrNull { shareUrlDto -> shareUrlDto.shareUrlId == shareUrlId.id }
                ?.let { shareUrlDto ->
                    return shareUrlDto
                }
        } while (results.shareUrlDtos.size == configurationProvider.uiPageSize)
        throw ApiException(ApiResult.Error.Http(404, "ShareUrl[${shareUrlId.id}] not found"))
    }

    @Throws(ApiException::class)
    suspend fun getAllShareUrls(
        shareId: ShareId,
        page: Int,
        pageSize: Int,
        recursive: Boolean
    ) = apiProvider.get<ShareUrlApi>(shareId.userId).invoke {
        getAllShareUrls(shareId.id, page, pageSize, recursive = if (recursive) 1 else 0)
    }.valueOrThrow

    @Throws(ApiException::class)
    suspend fun createShareUrl(
        shareId: ShareId,
        shareUrlInfo: ShareUrlInfo,
    ) = apiProvider.get<ShareUrlApi>(shareId.userId).invoke {
        createShareUrl(shareId.id, shareUrlInfo.toRequest())
    }.valueOrThrow

    @Throws(ApiException::class)
    suspend fun updateShareUrl(
        shareUrlId: ShareUrlId,
        shareUrlCustomPasswordInfo: ShareUrlCustomPasswordInfo?,
        shareUrlExpirationDurationInfo: ShareUrlExpirationDurationInfo?,
    ) = apiProvider.get<ShareUrlApi>(shareUrlId.shareId.userId).invoke {
        when {
            shareUrlCustomPasswordInfo != null && shareUrlExpirationDurationInfo != null ->
                updateShareUrl(
                    shareId = shareUrlId.shareId.id,
                    urlId = shareUrlId.id,
                    request = UpdateShareUrlRequest(shareUrlCustomPasswordInfo, shareUrlExpirationDurationInfo),
                )
            shareUrlCustomPasswordInfo != null ->
                updateShareUrl(
                    shareId = shareUrlId.shareId.id,
                    urlId = shareUrlId.id,
                    request = UpdateCustomPasswordShareUrlRequest(shareUrlCustomPasswordInfo),
                )
            shareUrlExpirationDurationInfo != null ->
                updateShareUrl(
                    shareId = shareUrlId.shareId.id,
                    urlId = shareUrlId.id,
                    request = UpdateExpirationDurationShareUrlRequest(shareUrlExpirationDurationInfo),
                )
            else -> error("Either custom password info or expiration duration info must be non-null")
        }
    }.valueOrThrow

    @Throws(ApiException::class)
    suspend fun updateShareUrlPermissions(
        shareUrlId: ShareUrlId,
        permissions: Permissions,
    ) = apiProvider.get<ShareUrlApi>(shareUrlId.shareId.userId).invoke {
        updateShareUrl(
            shareId = shareUrlId.shareId.id,
            urlId = shareUrlId.id,
            request = UpdatePermissionsShareUrlRequest(permissions.value),
        )
    }.valueOrThrow

    @Throws(ApiException::class)
    suspend fun deleteShareUrl(
        shareUrlId: ShareUrlId,
    ) = apiProvider.get<ShareUrlApi>(shareUrlId.userId).invoke {
        deleteShareUrl(shareUrlId.shareId.id, shareUrlId.id)
    }.valueOrThrow

    @Throws(ApiException::class)
    suspend fun deleteShareUrls(
        shareId: ShareId,
        shareUrlIds: List<ShareUrlId>,
    ) = apiProvider.get<ShareUrlApi>(shareId.userId).invoke {
        deleteShareUrls(shareId.id, DeleteShareUrlsRequest(shareUrlIds.map { shareUrlId -> shareUrlId.id }))
    }.valueOrThrow
}
