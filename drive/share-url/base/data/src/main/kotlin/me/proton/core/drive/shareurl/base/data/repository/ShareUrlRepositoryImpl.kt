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

package me.proton.core.drive.shareurl.base.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.link.data.api.entity.LinkDto
import me.proton.core.drive.link.data.extension.toLink
import me.proton.core.drive.link.data.extension.toLinkWithProperties
import me.proton.core.drive.link.domain.usecase.InsertOrUpdateLinks
import me.proton.core.drive.link.domain.usecase.SortLinksByParents
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.share.domain.usecase.DeleteShare
import me.proton.core.drive.share.domain.usecase.GetShare
import me.proton.core.drive.shareurl.base.data.api.ShareUrlApiDataSource
import me.proton.core.drive.shareurl.base.data.api.entity.ShareUrlDto
import me.proton.core.drive.shareurl.base.data.db.ShareUrlDatabase
import me.proton.core.drive.shareurl.base.data.extension.toShareUrl
import me.proton.core.drive.shareurl.base.data.extension.toShareUrlEntity
import me.proton.core.drive.shareurl.base.domain.entity.ShareUrl
import me.proton.core.drive.shareurl.base.domain.entity.ShareUrlCustomPasswordInfo
import me.proton.core.drive.shareurl.base.domain.entity.ShareUrlExpirationDurationInfo
import me.proton.core.drive.shareurl.base.domain.entity.ShareUrlId
import me.proton.core.drive.shareurl.base.domain.entity.ShareUrlInfo
import me.proton.core.drive.shareurl.base.domain.extension.userId
import me.proton.core.drive.shareurl.base.domain.repository.ShareUrlRepository
import javax.inject.Inject

class ShareUrlRepositoryImpl @Inject constructor(
    private val api: ShareUrlApiDataSource,
    private val configurationProvider: ConfigurationProvider,
    private val db: ShareUrlDatabase,
    private val insertOrUpdateLinks: InsertOrUpdateLinks,
    private val getShare: GetShare,
    private val deleteShare: DeleteShare,
    private val sortLinksByParents: SortLinksByParents,
) : ShareUrlRepository {

    private inline val dao get() = db.shareUrlDao

    override suspend fun hasShareUrl(shareUrlId: ShareUrlId): Boolean =
        dao.hasShareUrlEntity(shareUrlId.userId, shareUrlId.id)

    override suspend fun fetchShareUrl(shareUrlId: ShareUrlId): Result<ShareUrl> = coRunCatching {
        val shareUrlDto = api.getShareUrl(shareUrlId)
        getShare(shareUrlId.shareId.copy(id = shareUrlDto.shareId)).toResult().getOrThrow()
        val userId = shareUrlId.userId
        dao.insertOrUpdate(shareUrlDto.toShareUrlEntity(userId))
        shareUrlDto.toShareUrl(userId)
    }

    override fun getShareUrl(shareUrlId: ShareUrlId): Flow<ShareUrl?> =
        dao.getFlow(shareUrlId.userId, shareUrlId.id).map { entity ->
            entity?.toShareUrl()
        }

    override suspend fun getShareUrl(userId: UserId, shareUrlId: String): ShareUrl? =
        dao.get(userId, shareUrlId)?.toShareUrl()

    override suspend fun hasShareUrls(shareId: ShareId) =
        dao.hasShareUrlEntities(shareId.userId, shareId.id)

    override fun getAllShareUrls(shareId: ShareId): Flow<List<ShareUrl>> =
        dao.getAllFlow(shareId.userId, shareId.id).map { shareUrlEntities ->
            shareUrlEntities.map { shareUrlEntity -> shareUrlEntity.toShareUrl() }
        }

    override suspend fun fetchAllShareUrls(
        shareId: ShareId,
        saveLinks: Boolean,
    ): Result<List<ShareUrl>> = coRunCatching {
        val shareUrlDtos = mutableListOf<ShareUrlDto>()
        val linkDtos = mutableListOf<LinkDto>()
        var page = 0
        do {
            val results = api.getAllShareUrls(
                shareId = shareId,
                page = page++,
                pageSize = configurationProvider.uiPageSize,
                recursive = saveLinks,
            )
            shareUrlDtos.addAll(results.shareUrlDtos)
            linkDtos.addAll(results.linkDtos.values)
        } while (results.shareUrlDtos.size == configurationProvider.uiPageSize)
        saveShareUrlsAndLinks(shareId, saveLinks, shareUrlDtos, linkDtos)
        shareUrlDtos.map { shareUrlDto -> shareUrlDto.toShareUrl(shareId.userId) }
    }

    private suspend fun saveShareUrlsAndLinks(
        shareId: ShareId,
        saveLinks: Boolean,
        shareUrlDtos: Collection<ShareUrlDto>,
        linkDtos: Collection<LinkDto>,
    ) {
        // We need to make sure we have the shares before we save the share urls
        val userId = shareId.userId
        shareUrlDtos
            .distinctBy { shareUrlDto -> shareUrlDto.shareId }
            .forEach { shareUrlDto ->
                getShare(ShareId(userId, shareUrlDto.shareId)).toResult().getOrThrow()
            }
        db.inTransaction {
            dao.deleteAllForLinksInShare(shareId.userId, shareId.id)
            dao.deleteAll(shareId.userId, shareId.id)
            dao.insertOrUpdate(
                *shareUrlDtos.map { shareUrlDto ->
                    shareUrlDto.toShareUrlEntity(userId)
                }.toTypedArray()
            )
            if (saveLinks) {
                insertOrUpdateLinks(
                    links = sortLinksByParents(
                        linkDtos
                            .distinctBy { linkDto -> linkDto.id }
                            .map { linkDto ->
                                linkDto.toLinkWithProperties(shareId).toLink()
                            }
                    )
                )
            }
        }
    }

    override suspend fun createShareUrl(
        shareId: ShareId,
        shareUrlInfo: ShareUrlInfo,
    ): Result<ShareUrl> = coRunCatching {
        val shareUrlEntity = api.createShareUrl(
            shareId = shareId,
            shareUrlInfo = shareUrlInfo,
        ).shareUrl.toShareUrlEntity(shareId.userId)
        dao.insertOrUpdate(shareUrlEntity)
        shareUrlEntity.toShareUrl()
    }

    override suspend fun deleteShareUrl(shareUrlId: ShareUrlId): Result<Unit> = coRunCatching {
        api.deleteShareUrl(shareUrlId)
        dao.delete(shareUrlId.id)
        // We ignore the result of the share deletion as it is possible there are still some urls attached to it
        deleteShare(shareUrlId.shareId).getOrDefault(Unit)
    }

    override suspend fun updateShareUrl(
        shareUrlId: ShareUrlId,
        shareUrlCustomPasswordInfo: ShareUrlCustomPasswordInfo?,
        shareUrlExpirationDurationInfo: ShareUrlExpirationDurationInfo?,
    ): Result<ShareUrl> = coRunCatching {
        val shareUrlEntity = api.updateShareUrl(
            shareUrlId = shareUrlId,
            shareUrlCustomPasswordInfo = shareUrlCustomPasswordInfo,
            shareUrlExpirationDurationInfo = shareUrlExpirationDurationInfo,
        ).shareUrl.toShareUrlEntity(shareUrlId.shareId.userId)
        dao.insertOrUpdate(shareUrlEntity)
        shareUrlEntity.toShareUrl()
    }
}
