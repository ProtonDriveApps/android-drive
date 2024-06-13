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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.extension.filterSuccessOrError
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.usecase.FetchLinks
import me.proton.core.drive.link.domain.usecase.HasLinks
import me.proton.core.drive.link.domain.usecase.InsertOrUpdateLinks
import me.proton.core.drive.link.domain.usecase.SortLinksByParents
import me.proton.core.drive.share.domain.entity.Share
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.share.domain.usecase.GetShare
import me.proton.core.drive.share.domain.usecase.GetShares
import me.proton.core.drive.shareurl.base.data.api.ShareUrlApiDataSource
import me.proton.core.drive.shareurl.base.data.db.ShareUrlDatabase
import me.proton.core.drive.shareurl.base.data.db.entity.ShareUrlEntity
import me.proton.core.drive.shareurl.base.data.extension.toShareUrl
import me.proton.core.drive.shareurl.base.data.extension.toShareUrlEntity
import me.proton.core.drive.shareurl.base.domain.entity.ShareUrl
import me.proton.core.drive.shareurl.base.domain.entity.ShareUrlCustomPasswordInfo
import me.proton.core.drive.shareurl.base.domain.entity.ShareUrlExpirationDurationInfo
import me.proton.core.drive.shareurl.base.domain.entity.ShareUrlId
import me.proton.core.drive.shareurl.base.domain.entity.ShareUrlInfo
import me.proton.core.drive.shareurl.base.domain.extension.userId
import me.proton.core.drive.shareurl.base.domain.repository.ShareUrlRepository
import me.proton.core.drive.volume.data.api.VolumeApiDataSource
import me.proton.core.drive.volume.data.api.response.GetShareUrlsResponse
import me.proton.core.drive.volume.domain.entity.VolumeId
import javax.inject.Inject

class ShareUrlRepositoryImpl @Inject constructor(
    private val api: ShareUrlApiDataSource,
    private val configurationProvider: ConfigurationProvider,
    private val db: ShareUrlDatabase,
    private val insertOrUpdateLinks: InsertOrUpdateLinks,
    private val getShare: GetShare,
    private val getShares: GetShares,
    private val sortLinksByParents: SortLinksByParents,
    private val volumeApi: VolumeApiDataSource,
    private val hasLinks: HasLinks,
    private val fetchLinks: FetchLinks,
) : ShareUrlRepository {

    private inline val dao get() = db.shareUrlDao

    override suspend fun hasShareUrl(shareUrlId: ShareUrlId): Boolean =
        dao.hasShareUrlEntity(shareUrlId.userId, shareUrlId.id)

    override suspend fun fetchShareUrl(volumeId: VolumeId, shareUrlId: ShareUrlId): Result<ShareUrl> = coRunCatching {
        val shareUrlDto = api.getShareUrl(shareUrlId)
        getShare(shareUrlId.shareId.copy(id = shareUrlDto.shareId)).toResult().getOrThrow()
        val userId = shareUrlId.userId
        dao.insertOrUpdate(shareUrlDto.toShareUrlEntity(userId, volumeId))
        shareUrlDto.toShareUrl(userId, volumeId)
    }

    override fun getShareUrl(shareUrlId: ShareUrlId): Flow<ShareUrl?> =
        dao.getFlow(shareUrlId.userId, shareUrlId.id).map { entity ->
            entity?.toShareUrl()
        }

    override suspend fun getShareUrl(userId: UserId, shareUrlId: String): ShareUrl? =
        dao.get(userId, shareUrlId)?.toShareUrl()

    override suspend fun hasShareUrls(userId: UserId, volumeId: VolumeId) =
        dao.hasShareUrlEntities(userId, volumeId.id)

    override fun getAllShareUrls(userId: UserId, volumeId: VolumeId): Flow<List<ShareUrl>> =
        dao.getAllFlow(userId, volumeId.id).map { shareUrlEntities ->
            shareUrlEntities.map { shareUrlEntity -> shareUrlEntity.toShareUrl() }
        }

    override suspend fun fetchAllShareUrls(
        userId: UserId,
        volumeId: VolumeId,
        saveLinks: Boolean,
    ): Result<List<ShareUrl>> = coRunCatching {
        val contextShareIds = mutableSetOf<ShareId>()
        val links = mutableListOf<Link>()
        val shareUrlEntities = mutableListOf<ShareUrlEntity>()
        var page = 0
        var shareUrlsResponse: GetShareUrlsResponse
        do {
            shareUrlsResponse = volumeApi.getShareUrls(
                userId = userId,
                volumeId = volumeId,
                pageIndex = page++,
                pageSize = configurationProvider.uiPageSize,
            )
            shareUrlsResponse.shareUrlContexts.forEach { shareUrlContext ->
                val contextShareId = ShareId(userId, shareUrlContext.contextShareId)
                contextShareIds.add(contextShareId)
                links.addAll(
                    shareUrlContext
                        .linkIds
                        .toSet()
                        .getNonCachedLinks(contextShareId)
                )
                shareUrlEntities.addAll(
                    shareUrlContext.shareUrls.map { shareUrlDto ->
                        shareUrlDto.toShareUrlEntity(userId, volumeId)
                    }
                )
            }
        } while (shareUrlsResponse.more)
        if (saveLinks) {
            saveShareUrlsAndLinks(userId, volumeId, contextShareIds, links, shareUrlEntities)
        }
        shareUrlEntities.map { shareUrlEntity -> shareUrlEntity.toShareUrl() }
    }

    private suspend fun Set<String>.getNonCachedLinks(shareId: ShareId): List<Link> {
        val nonCachedLinks = mutableListOf<Link>()
        val cachedLinkIds = hasLinks(shareId, this.toSet())
        val nonCachedLinkIds = this - cachedLinkIds.map { linkId -> linkId.id }.toSet()
        if (nonCachedLinkIds.isNotEmpty()) {
            val fetchedLinks = fetchLinks(shareId, nonCachedLinkIds).getOrThrow()
            nonCachedLinks.addAll(
                sortLinksByParents((fetchedLinks.first.toSet() + fetchedLinks.second.toSet()).toList())
                    .filter { link -> link.id.id in nonCachedLinkIds }
            )
        }
        return nonCachedLinks
    }

    private suspend fun saveShareUrlsAndLinks(
        userId: UserId,
        volumeId: VolumeId,
        contextShareIds: Set<ShareId>,
        links: List<Link>,
        shareUrlEntities: Collection<ShareUrlEntity>,
    ) {
        // We need to make sure we have the shares before we save the share urls
        getShares(
            userId,
            Share.Type.STANDARD,
            flowOf(true),
        ).filterSuccessOrError().first().toResult().getOrThrow() // Why always? We can check if we have shares
        contextShareIds.forEach { shareId ->
            getShare(shareId).toResult().getOrThrow()
        }
        db.inTransaction {
            dao.deleteAll(userId, volumeId.id)
            insertOrUpdateLinks(
                links = links
            )
            dao.insertOrUpdate(
                *shareUrlEntities.toTypedArray()
            )
        }
    }

    override suspend fun createShareUrl(
        volumeId: VolumeId,
        shareId: ShareId,
        shareUrlInfo: ShareUrlInfo,
    ): Result<ShareUrl> = coRunCatching {
        val shareUrlEntity = api.createShareUrl(
            shareId = shareId,
            shareUrlInfo = shareUrlInfo,
        ).shareUrl.toShareUrlEntity(shareId.userId, volumeId)
        dao.insertOrUpdate(shareUrlEntity)
        shareUrlEntity.toShareUrl()
    }

    override suspend fun deleteShareUrl(shareUrlId: ShareUrlId): Result<Unit> = coRunCatching {
        api.deleteShareUrl(shareUrlId)
        dao.delete(shareUrlId.id)
    }

    override suspend fun updateShareUrl(
        volumeId: VolumeId,
        shareUrlId: ShareUrlId,
        shareUrlCustomPasswordInfo: ShareUrlCustomPasswordInfo?,
        shareUrlExpirationDurationInfo: ShareUrlExpirationDurationInfo?,
    ): Result<ShareUrl> = coRunCatching {
        val shareUrlEntity = api.updateShareUrl(
            shareUrlId = shareUrlId,
            shareUrlCustomPasswordInfo = shareUrlCustomPasswordInfo,
            shareUrlExpirationDurationInfo = shareUrlExpirationDurationInfo,
        ).shareUrl.toShareUrlEntity(shareUrlId.shareId.userId, volumeId)
        dao.insertOrUpdate(shareUrlEntity)
        shareUrlEntity.toShareUrl()
    }
}
