/*
 * Copyright (c) 2025 Proton AG.
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

package me.proton.core.drive.drivelink.download.data.repository

import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.function.pagedList
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.drivelink.download.data.db.DriveLinkDownloadDatabase
import me.proton.core.drive.drivelink.download.data.extension.toDownloadParentLink
import me.proton.core.drive.drivelink.download.data.extension.toParentLinkDownloadEntity
import me.proton.core.drive.drivelink.download.domain.entity.DownloadParentLink
import me.proton.core.drive.drivelink.download.domain.repository.DownloadParentLinkRepository
import me.proton.core.drive.link.data.api.entity.LinkDto
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.volume.domain.entity.VolumeId
import javax.inject.Inject

class DownloadParentLinkRepositoryImpl @Inject constructor(
    db: DriveLinkDownloadDatabase,
    private val configurationProvider: ConfigurationProvider,
) : DownloadParentLinkRepository {
    private val dao = db.parentLinkDownloadDao

    override fun getCountFlow(userId: UserId): Flow<Int> =
        dao.getCountFlow(userId)

    override suspend fun add(downloadParentLink: DownloadParentLink) =
        dao.insertOrIgnore(downloadParentLink.toParentLinkDownloadEntity())

    override suspend fun delete(id: Long) =
        dao.delete(id)

    override suspend fun delete(volumeId: VolumeId, linkId: LinkId) =
        dao.delete(linkId.userId, volumeId.id, linkId.shareId.id, linkId.id)

    override suspend fun deleteAll(userId: UserId) =
        dao.deleteAll(userId)

    override suspend fun getAllParentLinks(userId: UserId): List<DownloadParentLink> =
        pagedList(configurationProvider.dbPageSize) { fromIndex, count ->
            dao.getAll(
                userId = userId,
                types = setOf(LinkDto.TYPE_FOLDER, LinkDto.TYPE_ALBUM),
                limit = count,
                offset = fromIndex,
            )
        }.map { entity -> entity.toDownloadParentLink() }
}
