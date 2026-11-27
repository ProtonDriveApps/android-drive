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

package me.proton.core.drive.drivelink.download.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.drivelink.download.domain.entity.DownloadFileLink
import me.proton.core.drive.drivelink.download.domain.entity.NetworkType
import me.proton.core.drive.drivelink.download.domain.repository.DownloadFileRepository
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.volume.domain.entity.VolumeId

class TestDownloadFileRepository : DownloadFileRepository {

    val links = mutableListOf<DownloadFileLink>()
    val failedIds = mutableListOf<Long>()

    var nextIdle: DownloadFileLink? = null

    override suspend fun getNextIdleAndUpdate(
        userId: UserId,
        networkTypes: Set<NetworkType>,
        state: DownloadFileLink.State
    ): DownloadFileLink? = nextIdle.also { nextIdle = null }

    override suspend fun add(downloadFileLink: DownloadFileLink) {
        links.add(downloadFileLink)
    }

    override suspend fun delete(id: Long): Boolean =
        links.removeAll { link -> link.id == id }

    override suspend fun delete(
        volumeId: VolumeId,
        fileId: FileId,
        revisionId: String
    ): Boolean = links.removeAll { link ->
        link.volumeId == volumeId && link.fileId == fileId && link.revisionId == revisionId
    }

    override suspend fun deleteAll(userId: UserId) {
    }

    override suspend fun resetAllState(
        userId: UserId,
        state: DownloadFileLink.State
    ) {
    }

    override fun getCountFlow(userId: UserId): Flow<Int> = emptyFlow()

    override fun getCountFlow(
        userId: UserId,
        state: DownloadFileLink.State
    ): Flow<Int> = emptyFlow()

    override suspend fun hasChildrenOf(
        userId: UserId,
        volumeId: VolumeId,
        linkId: LinkId
    ): Boolean = false

    override suspend fun updateStateToFailed(id: Long, runAt: Long) {
        failedIds.add(id)
    }

    override suspend fun getAllWithState(
        userId: UserId,
        state: DownloadFileLink.State
    ): List<DownloadFileLink> =
        links.filter { link -> link.state == state }

    override suspend fun resetStateAndIncreaseRetries(
        id: Long,
        state: DownloadFileLink.State
    ) {
    }

    override suspend fun getNumberOfRetries(
        volumeId: VolumeId,
        fileId: FileId
    ): Int? = 0
}