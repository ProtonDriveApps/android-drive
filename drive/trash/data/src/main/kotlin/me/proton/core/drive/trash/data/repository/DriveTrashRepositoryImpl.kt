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

import me.proton.core.drive.base.domain.entity.SaveAction
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.link.data.api.extension.associateResults
import me.proton.core.drive.link.data.api.response.LinkResponses
import me.proton.core.drive.link.data.extension.toLink
import me.proton.core.drive.link.data.extension.toLinkWithProperties
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.usecase.InsertOrUpdateLinks
import me.proton.core.drive.link.domain.usecase.SortLinksByParents
import me.proton.core.drive.linktrash.data.db.LinkTrashDatabase
import me.proton.core.drive.linktrash.domain.entity.TrashState
import me.proton.core.drive.linktrash.domain.repository.LinkTrashRepository
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.trash.data.api.datasource.TrashApiDataSource
import me.proton.core.drive.trash.domain.repository.DriveTrashRepository
import javax.inject.Inject

class DriveTrashRepositoryImpl @Inject constructor(
    private val apiDataSource: TrashApiDataSource,
    private val linkTrashRepository: LinkTrashRepository,
    private val db: LinkTrashDatabase,
    private val configurationProvider: ConfigurationProvider,
    private val insertOrUpdateLinks: InsertOrUpdateLinks,
    private val sortLinksByParents: SortLinksByParents,
) : DriveTrashRepository {

    override suspend fun sendToTrash(
        folderId: FolderId,
        links: List<LinkId>,
    ) = associateResults(links) {
        links.batchBy(configurationProvider.apiPageSize) { batch ->
            apiDataSource.sendToTrash(folderId, batch.map { link -> link.id })
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

    override suspend fun emptyTrash(shareId: ShareId) {
        apiDataSource.emptyTrash(shareId)
        linkTrashRepository.markTrashedLinkAsDeleted(shareId)
    }

    override suspend fun deleteItemsFromTrash(
        shareId: ShareId,
        links: List<LinkId>,
    ) = associateResults(links) {
        links.batchBy(configurationProvider.apiPageSize) { batch ->
            apiDataSource.deleteItemsFromTrash(shareId, batch.map { link -> link.id })
        }
    }

    override suspend fun refreshTrashContent(shareId: ShareId) {
        var page = 0
        do {
            var size = 0
            fetchTrashContent(
                shareId = shareId,
                page = page++,
                pageSize = configurationProvider.uiPageSize,
            ).onSuccess { (items, saveAction) ->
                saveAction()
                size = items.size
            }.onFailure { result ->
                throw RuntimeException(result.message, result.cause)
            }
        } while (size == configurationProvider.uiPageSize)
    }

    override suspend fun fetchTrashContent(
        shareId: ShareId,
        page: Int,
        pageSize: Int,
    ): Result<Pair<List<Link>, SaveAction>> {
        val (parents, entities) = apiDataSource.fetchTrashContent(shareId, page, pageSize).run {
            parentsDtos.values.map { dto ->
                dto.toLinkWithProperties(shareId).toLink()
            } to linkDtos.map { dto ->
                dto.toLinkWithProperties(shareId).toLink()
            }
        }
        val saveAction = SaveAction {
            db.inTransaction {
                insertOrUpdateLinks(sortLinksByParents(parents))
                insertOrUpdateLinks(entities)
                linkTrashRepository.insertOrUpdateTrashState(
                    entities.map { entity -> entity.id },
                    TrashState.TRASHED,
                )
                linkTrashRepository.markTrashContentAsFetched(shareId)
            }
        }
        return Result.success(entities to saveAction)
    }

    private suspend fun List<LinkId>.batchBy(
        size: Int,
        action: suspend (List<LinkId>) -> LinkResponses
    ) = LinkResponses(
        code = 0,
        responses = chunked(size).map { chunk -> action(chunk) }.flatMap { it.responses }
    )
}
