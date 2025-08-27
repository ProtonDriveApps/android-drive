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
import me.proton.core.drive.drivelink.download.domain.entity.DownloadFileLink
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.volume.domain.entity.VolumeId

interface DownloadFileRepository {
    suspend fun getNextIdleAndUpdate(userId: UserId, state: DownloadFileLink.State): DownloadFileLink?
    suspend fun add(downloadFileLink: DownloadFileLink)
    suspend fun delete(id: Long)
    suspend fun delete(volumeId: VolumeId, fileId: FileId, revisionId: String)
    suspend fun deleteAll(userId: UserId)
    suspend fun resetAllState(userId: UserId, state: DownloadFileLink.State)
    fun getCountFlow(userId: UserId): Flow<Int>
    fun getCountFlow(userId: UserId, state: DownloadFileLink.State): Flow<Int>
    suspend fun hasChildrenOf(userId: UserId, volumeId: VolumeId, linkId: LinkId): Boolean
    suspend fun updateStateToFailed(id: Long, runAt: Long = System.currentTimeMillis())
    suspend fun getAllWithState(userId: UserId, state: DownloadFileLink.State): List<DownloadFileLink>
    suspend fun resetStateAndIncreaseRetries(id: Long, state: DownloadFileLink.State)
    suspend fun getNumberOfRetries(volumeId: VolumeId, fileId: FileId): Int?
}
