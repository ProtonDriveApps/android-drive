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

package me.proton.core.drive.drivelink.data.repository

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import me.proton.core.drive.drivelink.data.db.dao.DriveLinkDao
import me.proton.core.drive.drivelink.data.extension.toDriveLinks
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.repository.DriveLinkRepository
import me.proton.core.drive.link.data.extension.toLink
import me.proton.core.drive.link.data.extension.toLinkWithProperties
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.extension.userId
import javax.inject.Inject

@OptIn(FlowPreview::class)
class DriveLinkRepositoryImpl @Inject constructor(
    private val driveLinkDao: DriveLinkDao,
) : DriveLinkRepository {

    override fun getDriveLink(linkId: LinkId): Flow<DriveLink?> =
        driveLinkDao.getLink(linkId.userId, linkId.shareId.id, linkId.id)
            .distinctUntilChanged()
            .map { entities -> entities.toDriveLinks().firstOrNull() }

    override fun getDriveLinksCount(parentId: FolderId): Flow<Int> =
        driveLinkDao.getLinksCountFlow(parentId.userId, parentId.shareId.id, parentId.id)

    override fun getDriveLinks(parentId: FolderId, fromIndex: Int, count: Int): Flow<List<DriveLink>> =
        driveLinkDao.getLinks(parentId.userId, parentId.shareId.id, parentId.id, count, fromIndex)
            .map { entities -> entities.toDriveLinks() }

    override fun getDriveLinks(linkIds: List<LinkId>): Flow<List<DriveLink>> =
        linkIds
            .map { linkId -> linkId.id }
            .takeIf { list -> list.isNotEmpty() }
            ?.let { list ->
                val shareIdLinkIdList = linkIds.map { linkId -> linkId.shareId.id to linkId.id }
                driveLinkDao
                    .getLinks(linkIds.first().userId, list)
                    .map { entities ->
                        entities
                            .filter { entity ->
                                with (entity.linkWithPropertiesEntity.toLinkWithProperties().toLink().id) {
                                    (shareId.id to id) in shareIdLinkIdList
                                }
                            }
                            .distinctBy { entity -> entity.linkWithPropertiesEntity.link.id }.toDriveLinks()
                    }
            } ?: flowOf(emptyList())
}
