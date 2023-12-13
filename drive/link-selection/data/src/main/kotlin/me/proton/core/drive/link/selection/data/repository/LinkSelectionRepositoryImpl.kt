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
package me.proton.core.drive.link.selection.data.repository

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.link.selection.data.db.LinkSelectionDatabase
import me.proton.core.drive.link.selection.data.db.entity.LinkSelectionEntity
import me.proton.core.drive.link.selection.domain.entity.SelectionId
import me.proton.core.drive.link.selection.domain.repository.LinkSelectionRepository
import javax.inject.Inject

class LinkSelectionRepositoryImpl @Inject constructor(
    private val db: LinkSelectionDatabase,
) : LinkSelectionRepository {

    override suspend fun hasSelectionWithId(selectionId: SelectionId): Boolean =
        db.linkSelectionDao.hasSelectionId(selectionId)

    override suspend fun insertOrIgnoreSelection(selectionId: SelectionId, linkIds: List<LinkId>) {
        db.linkSelectionDao.insertOrIgnore(
            *linkIds.map { linkId ->
                LinkSelectionEntity(
                    userId = linkId.userId,
                    shareId = linkId.shareId.id,
                    linkId = linkId.id,
                    selectionId = selectionId,
                )
            }.toTypedArray()
        )
    }

    override suspend fun insertSelection(linkIds: List<LinkId>, retries: Int): Result<SelectionId> {
        repeat(retries) {
            SelectionId().let { selectionId ->
                val insertedSelectionId = db.inTransaction {
                    if (db.linkSelectionDao.hasSelectionId(selectionId).not()) {
                        insertOrIgnoreSelection(selectionId, linkIds)
                        selectionId
                    } else {
                        null
                    }
                }
                if (insertedSelectionId != null) return Result.success(insertedSelectionId)
            }
        }
        return Result.failure(
            IllegalStateException(
                "Unable to find unique selection id in $retries attempt, try to increase number of retries"
            )
        )
    }

    override suspend fun remove(selectionId: SelectionId) {
        db.linkSelectionDao.deleteAll(selectionId)
    }

    override suspend fun remove(selectionId: SelectionId, linkIds: List<LinkId>) {
        db.linkSelectionDao.delete(
            *linkIds.map { linkId ->
                LinkSelectionEntity(
                    userId = linkId.userId,
                    shareId = linkId.shareId.id,
                    linkId = linkId.id,
                    selectionId = selectionId,
                )
            }.toTypedArray()
        )
    }

    override suspend fun removeAll(userId: UserId) {
        db.linkSelectionDao.deleteAll(userId)
    }

    override suspend fun duplicateSelection(selectionId: SelectionId, retries: Int): Result<SelectionId> {
        val entries = db.linkSelectionDao.getAll(selectionId)
        repeat(retries) {
            SelectionId().let { selectionId ->
                val insertedSelectionId = db.inTransaction {
                    if (db.linkSelectionDao.hasSelectionId(selectionId).not()) {
                        db.linkSelectionDao.insertOrIgnore(
                            *entries.map { entry ->
                                LinkSelectionEntity(
                                    userId = entry.userId,
                                    shareId = entry.shareId,
                                    linkId = entry.linkId,
                                    selectionId = selectionId,
                                )
                            }.toTypedArray()
                        )
                        selectionId
                    } else {
                        null
                    }
                }
                if (insertedSelectionId != null) return Result.success(insertedSelectionId)
            }
        }
        return Result.failure(
            IllegalStateException(
                "Unable to find unique selection id in $retries attempt, try to increase number of retries"
            )
        )
    }
}
