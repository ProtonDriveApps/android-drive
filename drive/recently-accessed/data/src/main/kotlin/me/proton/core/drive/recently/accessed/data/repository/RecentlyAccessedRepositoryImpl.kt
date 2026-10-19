/*
 * Copyright (c) 2026 Proton AG.
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

package me.proton.core.drive.recently.accessed.data.repository

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.recently.accessed.data.db.RecentlyAccessedDatabase
import me.proton.core.drive.recently.accessed.data.extension.toRecentlyAccessed
import me.proton.core.drive.recently.accessed.domain.entity.RecentlyAccessed
import me.proton.core.drive.recently.accessed.domain.repository.RecentlyAccessedRepository
import javax.inject.Inject

class RecentlyAccessedRepositoryImpl @Inject constructor(
    private val db: RecentlyAccessedDatabase,
) : RecentlyAccessedRepository {

    override suspend fun add(item: RecentlyAccessed) =
        db.recentlyAccessedDao.upsert(
            userId = item.linkId.userId,
            shareId = item.linkId.shareId.id,
            linkId = item.linkId.id,
            volumeId = item.volumeId.id,
            accessTime = item.accessTime.value,
        )

    override suspend fun getAll(userId: UserId, limit: Int): List<RecentlyAccessed> =
        db.recentlyAccessedDao.getAll(userId, limit).map { row -> row.toRecentlyAccessed() }

    override suspend fun delete(items: List<RecentlyAccessed>) {
        val accessTime = items.maxOfOrNull { item -> item.accessTime.value } ?: return
        db.inTransaction {
            items.groupBy { item -> item.linkId.shareId }.forEach { (shareId, shareItems) ->
                db.recentlyAccessedDao.delete(
                    userId = shareId.userId,
                    shareId = shareId.id,
                    linkIds = shareItems.map { item -> item.linkId.id },
                    accessTime = accessTime,
                )
            }
        }
    }
}
