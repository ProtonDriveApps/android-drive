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
package me.proton.core.drive.linkoffline.data.repository

import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.linkoffline.data.db.LinkOfflineDao
import me.proton.core.drive.linkoffline.data.db.LinkOfflineEntity
import me.proton.core.drive.linkoffline.domain.repository.LinkOfflineRepository
import javax.inject.Inject

class LinkOfflineRepositoryImpl @Inject constructor(
    private val db: LinkOfflineDao,
) : LinkOfflineRepository {

    override suspend fun isMarkedOffline(linkId: LinkId): Boolean =
        db.hasLinkOfflineEntity(linkId.userId, linkId.shareId.id, linkId.id)

    override suspend fun isAnyMarkedOffline(linkIds: Set<LinkId>): Boolean =
        db.hasAnyLinkOfflineEntity(linkIds)

    override suspend fun addOffline(linkId: LinkId) =
        db.insertOrIgnore(LinkOfflineEntity(linkId.userId, linkId.shareId.id, linkId.id))

    override suspend fun removeOffline(linkId: LinkId) =
        db.delete(LinkOfflineEntity(linkId.userId, linkId.shareId.id, linkId.id))
}
