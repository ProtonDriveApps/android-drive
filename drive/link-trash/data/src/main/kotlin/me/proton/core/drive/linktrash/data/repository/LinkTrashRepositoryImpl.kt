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
package me.proton.core.drive.linktrash.data.repository

import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.extension.asSuccess
import me.proton.core.drive.link.data.extension.toLink
import me.proton.core.drive.link.data.extension.toLinkWithProperties
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.linktrash.data.db.LinkTrashDatabase
import me.proton.core.drive.linktrash.data.db.entity.TrashMetadataEntity
import me.proton.core.drive.linktrash.data.db.entity.TrashWorkEntity
import me.proton.core.drive.linktrash.data.extension.toLinkTrashStateEntity
import me.proton.core.drive.linktrash.domain.entity.TrashState
import me.proton.core.drive.linktrash.domain.repository.LinkTrashRepository
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.volume.domain.entity.VolumeId
import java.util.UUID
import javax.inject.Inject

class LinkTrashRepositoryImpl @Inject constructor(
    private val db: LinkTrashDatabase,
) : LinkTrashRepository {

    override suspend fun insertOrUpdateTrashState(linkIds: List<LinkId>, trashState: TrashState) =
        db.linkTrashDao.insertOrUpdate(*linkIds.map { linkId ->
            trashState.toLinkTrashStateEntity(linkId.shareId, linkId.id)
        }.toTypedArray())

    override suspend fun removeTrashState(linkIds: List<LinkId>) = linkIds
        .groupBy({ linkId -> linkId.shareId }) { linkId -> linkId.id }
        .forEach { (shareId, ids) ->
            db.linkTrashDao.delete(shareId.userId, shareId.id, ids)
        }

    override suspend fun markTrashedLinkAsDeleted(shareId: ShareId) {
        db.linkTrashDao.delete(shareId.userId, shareId.id)
    }

    override fun hasTrashContent(userId: UserId, volumeId: VolumeId) =
        db.linkTrashDao.hasTrashContent(userId)

    override suspend fun hasWorkWithId(workId: String): Boolean =
        db.trashWorkDao.hasWorkId(workId)

    override suspend fun insertOrIgnoreWorkId(linkIds: List<LinkId>, workId: String) =
        db.trashWorkDao.insertOrIgnore(
            *linkIds.map { linkId ->
                TrashWorkEntity(
                    userId = linkId.userId,
                    shareId = linkId.shareId.id,
                    linkId = linkId.id,
                    workId = workId,
                )
            }.toTypedArray()
        )

    override suspend fun insertWork(linkIds: List<LinkId>, retries: Int): DataResult<String> {
        repeat(retries) {
            UUID.randomUUID().toString().let { workId ->
                val insertedWorkId = db.inTransaction {
                    if (db.trashWorkDao.hasWorkId(workId).not()) {
                        insertOrIgnoreWorkId(linkIds, workId)
                        workId
                    } else {
                        null
                    }
                }
                if (insertedWorkId != null) return insertedWorkId.asSuccess
            }
        }
        return DataResult.Error.Local(
            message = "Unable to find unique work id in $retries attempt, try to increase number of retries",
            cause = null
        )
    }

    override suspend fun getLinksAndRemoveWorkFromCache(workId: String): List<Link> = db.inTransaction {
        val links = db.trashWorkDao.getAllLinkWithProperties(workId)
        db.trashWorkDao.deleteAll(workId)
        links.map { link -> link.toLinkWithProperties().toLink() }
    }

    override suspend fun markTrashContentAsFetched(userId: UserId, volumeId: VolumeId) =
        db.trashMetadataDao.insertOrUpdate(userId, volumeId.id, System.currentTimeMillis())

    override suspend fun shouldInitiallyFetchTrashContent(userId: UserId, volumeId: VolumeId): Boolean =
        db.trashMetadataDao.get(userId, volumeId.id).hasNotFetchedTrashContent

    override suspend fun isTrashed(linkId: LinkId): Boolean =
        db.linkTrashDao.isTrashed(linkId.shareId.userId, linkId.shareId.id, linkId.id)

    override suspend fun isAnyTrashed(linkIds: Set<LinkId>): Boolean =
        db.linkTrashDao.isAnyTrashed(linkIds)

    private val TrashMetadataEntity?.hasNotFetchedTrashContent: Boolean
        get() = this?.lastFetchTrashTimestamp == null
}
