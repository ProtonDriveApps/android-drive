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
package me.proton.core.drive.link.data.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.extension.asSuccessOrNullAsError
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.link.data.api.LinkApiDataSource
import me.proton.core.drive.link.data.db.LinkDatabase
import me.proton.core.drive.link.data.db.entity.LinkWithProperties
import me.proton.core.drive.link.data.extension.toCheckAvailableHashes
import me.proton.core.drive.link.data.extension.toLink
import me.proton.core.drive.link.data.extension.toLinkWithProperties
import me.proton.core.drive.link.domain.entity.CheckAvailableHashes
import me.proton.core.drive.link.domain.entity.CheckAvailableHashesInfo
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.entity.MoveInfo
import me.proton.core.drive.link.domain.entity.RenameInfo
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.link.domain.repository.LinkRepository
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.volume.domain.entity.VolumeId
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class LinkRepositoryImpl @Inject constructor(
    private val api: LinkApiDataSource,
    private val db: LinkDatabase,
) : LinkRepository {

    private val dao = db.linkDao

    override fun getLinkFlow(linkId: LinkId): Flow<DataResult<Link>> =
        dao.getLinkWithPropertiesFlow(linkId.userId, linkId.shareId.id, linkId.id)
            .mapLatest { linkWithProperties: LinkWithProperties? ->
                linkWithProperties?.toLink().asSuccessOrNullAsError()
            }

    override fun hasLink(linkId: LinkId): Flow<Boolean> =
        dao.hasLinkEntity(linkId.userId, linkId.shareId.id, linkId.id)

    override suspend fun hasAnyFileLink(shareId: ShareId): Boolean =
        dao.hasAnyFileEntity(shareId.userId, shareId.id)

    override suspend fun fetchLink(linkId: LinkId) {
        dao.insertOrUpdate(
            api.getLink(linkId)
                .toLinkWithProperties(linkId.shareId)
        )
    }

    override suspend fun checkAvailableHashes(
        linkId: LinkId,
        checkAvailableHashesInfo: CheckAvailableHashesInfo,
    ): Result<CheckAvailableHashes> = coRunCatching {
        api.checkAvailableHashes(linkId, checkAvailableHashesInfo)
            .toCheckAvailableHashes(linkId)
    }

    override suspend fun moveLink(
        linkId: LinkId,
        moveInfo: MoveInfo,
    ): Result<Unit> = coRunCatching {
        api.moveLink(linkId, moveInfo)
    }

    override suspend fun renameLink(
        linkId: LinkId,
        renameInfo: RenameInfo,
    ): Result<Unit> = coRunCatching {
        api.renameLink(linkId, renameInfo)
    }

    override suspend fun insertOrUpdate(links: List<Link>) {
        db.linkDao.insertOrUpdate(*links.map { link -> link.toLinkWithProperties() }.toTypedArray())
    }

    override suspend fun delete(linkIds: List<LinkId>) {
        db.inTransaction {
            linkIds
                .groupBy({ link -> link.shareId }) { link -> link.id }
                .forEach { (shareId, linkIds) ->
                    dao.delete(shareId.userId, shareId.id, linkIds)
                }
        }
    }

    override suspend fun fetchLinks(shareId: ShareId, linkIds: Set<String>) = coRunCatching {
        api
            .getLinks(shareId, linkIds)
            .let { response ->
                response.parents.map { linkDto -> linkDto.toLinkWithProperties(shareId).toLink() } to
                        response.links.map { linkDto -> linkDto.toLinkWithProperties(shareId).toLink() }
            }
    }

    override suspend fun fetchAndStoreLinks(shareId: ShareId, linkIds: Set<String>) {
        db.linkDao.insertOrUpdate(
            *api
                .getLinks(shareId, linkIds)
                .links
                .map { linkDto -> linkDto.toLinkWithProperties(shareId) }
                .toTypedArray()
        )
    }

    override suspend fun fetchAndStoreLinks(linkIds: Set<LinkId>) {
        linkIds
            .groupBy { linkId -> linkId.shareId }
            .forEach { (shareId, links) ->
                fetchAndStoreLinks(shareId, links.map { link -> link.id }.toSet())
            }
    }

    override suspend fun getCachedLinks(userId: UserId, shareId: String, linkIds: Set<String>): Set<Link> =
        db.linkDao.getFlow(userId, shareId, linkIds.toList()).first().map { linkWithProperties ->
            linkWithProperties.toLinkWithProperties().toLink()
        }.toSet()

    override suspend fun findLinkIds(
        userId: UserId,
        volumeId: VolumeId,
        linkId: String,
    ): List<LinkId> =
        db.linkDao.getLinks(userId, volumeId.id, linkId).map { linkFilePropertiesEntity ->
            linkFilePropertiesEntity.toLinkWithProperties().linkId
        }
}
