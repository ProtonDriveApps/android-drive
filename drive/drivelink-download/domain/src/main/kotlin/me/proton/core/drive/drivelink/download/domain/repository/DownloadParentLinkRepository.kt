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

package me.proton.core.drive.drivelink.download.domain.repository

import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.drivelink.download.domain.entity.DownloadParentLink
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.volume.domain.entity.VolumeId

interface DownloadParentLinkRepository {
    fun getCountFlow(userId: UserId): Flow<Int>
    suspend fun add(downloadParentLink: DownloadParentLink)
    suspend fun delete(id: Long)
    suspend fun delete(volumeId: VolumeId, linkId: LinkId)
    suspend fun deleteAll(userId: UserId)
    suspend fun getAllParentLinks(userId: UserId): List<DownloadParentLink>
}
