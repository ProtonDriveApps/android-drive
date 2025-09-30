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
import me.proton.core.drive.drivelink.download.data.extension.toDownloadFileLink
import me.proton.core.drive.drivelink.download.data.extension.toFileDownloadEntity
import me.proton.core.drive.drivelink.download.domain.entity.DownloadFileLink
import me.proton.core.drive.drivelink.download.domain.entity.NetworkType
import me.proton.core.drive.drivelink.download.domain.repository.DownloadFileRepository
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.volume.domain.entity.VolumeId
import javax.inject.Inject

class DownloadFileRepositoryImpl @Inject constructor(
    private val db: DriveLinkDownloadDatabase,
    private val configurationProvider: ConfigurationProvider,
) : DownloadFileRepository {
    private val dao = db.fileDownloadDao

    override fun getCountFlow(userId: UserId): Flow<Int> =
        dao.getCountFlow(userId)

    override fun getCountFlow(userId: UserId, state: DownloadFileLink.State): Flow<Int> =
        dao.getCountFlow(userId, state)

    override suspend fun getNextIdleAndUpdate(
        userId: UserId,
        networkTypes: Set<NetworkType>,
        state: DownloadFileLink.State
    ): DownloadFileLink? = db.inTransaction {
        dao.getNext(userId, networkTypes, DownloadFileLink.State.IDLE)?.let { entity ->
            entity.copy(state = state).also {
                dao.update(it)
            }
        }?.toDownloadFileLink()
    }

    override suspend fun add(downloadFileLink: DownloadFileLink) {
        db.inTransaction {
            dao.get(
                userId = downloadFileLink.fileId.userId,
                volumeId = downloadFileLink.volumeId.id,
                shareId = downloadFileLink.fileId.shareId.id,
                fileId = downloadFileLink.fileId.id,
                revisionId = downloadFileLink.revisionId,
            )?.let { entity ->
                dao.update(
                    downloadFileLink.toFileDownloadEntity().copy(
                        id = entity.id,
                        state = entity.state,
                    )
                )
            } ?: dao.insertOrIgnore(downloadFileLink.toFileDownloadEntity())
        }
    }

    override suspend fun delete(id: Long) =
        dao.delete(id)

    override suspend fun delete(volumeId: VolumeId, fileId: FileId, revisionId: String) =
        dao.delete(
            userId = fileId.userId,
            volumeId = volumeId.id,
            shareId = fileId.shareId.id,
            fileId = fileId.id,
            revisionId = revisionId,
        )

    override suspend fun deleteAll(userId: UserId) =
        dao.deleteAll(userId)

    override suspend fun resetAllState(userId: UserId, state: DownloadFileLink.State) =
        dao.resetAllState(userId, state)

    override suspend fun hasChildrenOf(
        userId: UserId,
        volumeId: VolumeId,
        linkId: LinkId,
    ): Boolean =
        dao.getCount(userId, volumeId.id, linkId.id) > 0

    override suspend fun updateStateToFailed(id: Long, runAt: Long) = db.inTransaction {
        dao.get(id)?.let { entity ->
            dao.update(
                entity.copy(
                    state = DownloadFileLink.State.FAILED,
                    lastRunTimestamp = runAt,
                )
            )
        }
        Unit
    }

    override suspend fun getAllWithState(
        userId: UserId,
        state: DownloadFileLink.State
    ): List<DownloadFileLink> =
        pagedList(configurationProvider.dbPageSize) { fromIndex, count ->
            dao.getAll(userId, state, fromIndex, count).map { entity -> entity.toDownloadFileLink() }
        }

    override suspend fun resetStateAndIncreaseRetries(
        id: Long,
        state: DownloadFileLink.State,
    ) = db.inTransaction {
        dao.get(id)?.let { entity ->
            dao.update(
                entity.copy(
                    state = DownloadFileLink.State.IDLE,
                    numberOfRetries = entity.numberOfRetries + 1,
                )
            )
        }
        Unit
    }

    override suspend fun getNumberOfRetries(
        volumeId: VolumeId,
        fileId: FileId,
    ): Int? =
        dao
            .get(
                userId = fileId.userId,
                volumeId = volumeId.id,
                shareId = fileId.shareId.id,
                fileId = fileId.id,
            )
            ?.numberOfRetries
}
