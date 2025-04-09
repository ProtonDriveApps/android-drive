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

package me.proton.core.drive.trash.data.repository

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.SaveAction
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.link.data.api.extension.associateResults
import me.proton.core.drive.link.data.api.response.LinkResponses
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.entity.ParentId
import me.proton.core.drive.link.domain.repository.LinkRepository
import me.proton.core.drive.link.domain.usecase.InsertOrUpdateLinks
import me.proton.core.drive.link.domain.usecase.SortLinksByParents
import me.proton.core.drive.linktrash.data.db.LinkTrashDatabase
import me.proton.core.drive.linktrash.domain.entity.TrashState
import me.proton.core.drive.linktrash.domain.repository.LinkTrashRepository
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.share.domain.usecase.GetShare
import me.proton.core.drive.trash.data.api.datasource.TrashApiDataSource
import me.proton.core.drive.trash.domain.repository.DriveTrashRepository
import me.proton.core.drive.volume.data.api.VolumeApiDataSource
import me.proton.core.drive.volume.domain.entity.VolumeId
import javax.inject.Inject

class DriveTrashRepositoryImpl @Inject constructor(
    private val apiDataSource: TrashApiDataSource,
    private val volumeApi: VolumeApiDataSource,
    private val linkRepository: LinkRepository,
    private val linkTrashRepository: LinkTrashRepository,
    private val db: LinkTrashDatabase,
    private val configurationProvider: ConfigurationProvider,
    private val insertOrUpdateLinks: InsertOrUpdateLinks,
    private val sortLinksByParents: SortLinksByParents,
    private val getShare: GetShare,
) : DriveTrashRepository {

    override suspend fun sendToTrash(
        parentId: ParentId,
        links: List<LinkId>,
    ) = associateResults(links) {
        links.batchBy(configurationProvider.apiPageSize) { batch ->
            apiDataSource.sendToTrash(parentId, batch.map { link -> link.id })
        }
    }

    override suspend fun restoreFromTrash(
        shareId: ShareId,
        links: List<LinkId>,
    ) = associateResults(links) {
        links.batchBy(configurationProvider.apiPageSize) { batch ->
            apiDataSource.restoreFromTrash(shareId, batch.map { link -> link.id })
        }
    }

    override suspend fun emptyTrash(userId: UserId, volumeId: VolumeId) {
        volumeApi.emptyTrash(userId, volumeId)
        linkTrashRepository.markTrashedLinkAsDeleted(userId, volumeId)
    }

    override suspend fun deleteItemsFromTrash(
        shareId: ShareId,
        links: List<LinkId>,
    ) = associateResults(links) {
        links.batchBy(configurationProvider.apiPageSize) { batch ->
            apiDataSource.deleteItemsFromTrash(shareId, batch.map { link -> link.id })
        }
    }

    override suspend fun fetchTrashContent(
        userId: UserId,
        volumeId: VolumeId,
        pageIndex: Int,
        pageSize: Int,
    ): Result<Pair<List<Link>, SaveAction>> = coRunCatching {
        val allParents = mutableSetOf<Link>()
        val allLinks = mutableSetOf<Link>()
        val fetchedLinks = mutableSetOf<Link>()
        volumeApi
            .getShareTrashes(userId, volumeId.id, pageIndex, pageSize)
            .forEach { shareTrash ->
                val cachedLinks = linkRepository.getCachedLinks(userId, shareTrash.shareId, shareTrash.linkIds.toSet())
                allLinks.addAll(cachedLinks)
                val notCachedLinkIds = shareTrash.linkIds.toSet() - cachedLinks.map { link -> link.id.id }.toSet()
                if (notCachedLinkIds.isNotEmpty()) {
                    val shareId = getShare(ShareId(userId, shareTrash.shareId)).toResult().getOrThrow().id
                    val (parents, links) = linkRepository.fetchLinks(shareId, notCachedLinkIds).getOrThrow()
                    allParents.addAll(parents)
                    allLinks.addAll(links)
                    fetchedLinks.addAll(links)
                }
            }
        Pair(
            allLinks.toList(),
            SaveAction {
                db.inTransaction {
                    insertOrUpdateLinks(sortLinksByParents(allParents.toList()))
                    insertOrUpdateLinks(fetchedLinks.toList())
                    linkTrashRepository.insertOrUpdateTrashState(
                        volumeId = volumeId,
                        linkIds = allLinks.map { link -> link.id },
                        trashState = TrashState.TRASHED,
                    )
                }
            },
        )
    }

    private suspend fun List<LinkId>.batchBy(
        size: Int,
        action: suspend (List<LinkId>) -> LinkResponses
    ) = LinkResponses(
        code = 0,
        responses = chunked(size).map { chunk -> action(chunk) }.flatMap { it.responses }
    )
}
