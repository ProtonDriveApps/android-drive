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

package me.proton.core.drive.photo.domain.repository

import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.PhotoTag
import me.proton.core.drive.photo.domain.entity.TagsMigrationFile
import me.proton.core.drive.photo.domain.entity.TagsMigrationStatistics
import me.proton.core.drive.photo.domain.entity.TagsMigrationStatus
import me.proton.core.drive.volume.domain.entity.VolumeId

interface TagsMigrationRepository {
    suspend fun getStatus(userId: UserId, volumeId: VolumeId): TagsMigrationStatus
    fun getStatusFlow(userId: UserId, volumeId: VolumeId): Flow<TagsMigrationStatus>
    suspend fun updateStatus(userId: UserId, volumeId: VolumeId, status: TagsMigrationStatus)
    suspend fun insertFiles(files: List<TagsMigrationFile>)
    suspend fun getFile(volumeId: VolumeId, fileId: FileId): TagsMigrationFile?
    suspend fun getFilesByState(
        userId: UserId,
        volumeId: VolumeId,
        state: TagsMigrationFile.State,
        count: Int,
    ): List<TagsMigrationFile>
    suspend fun getBatchFilesByState(
        userId: UserId,
        volumeId: VolumeId,
        state: TagsMigrationFile.State,
        count: Int,
    ): List<TagsMigrationFile>

    suspend fun updateState(volumeId: VolumeId, fileId: FileId, state: TagsMigrationFile.State)
    suspend fun updateUri(volumeId: VolumeId, fileId: FileId, uriString: String?)
    suspend fun updateMimeType(volumeId: VolumeId, fileId: FileId, mimeType: String?)

    suspend fun insertTags(volumeId: VolumeId, fileId: FileId, tags: Set<PhotoTag>)
    suspend fun getTags(volumeId: VolumeId, fileId: FileId): Set<PhotoTag>

    suspend fun getLatestDownloadedFile(
        userId: UserId,
        volumeId: VolumeId
    ): Flow<TagsMigrationFile?>

    suspend fun getLatestFileByState(
        userId: UserId,
        volumeId: VolumeId,
        state: TagsMigrationFile.State
    ): Flow<TagsMigrationFile?>

    suspend fun removeAll(userId: UserId, volumeId: VolumeId)
    suspend fun getOldestFileWithState(
        userId: UserId,
        volumeId: VolumeId,
        state: TagsMigrationFile.State
    ): Flow<TagsMigrationFile?>

    fun getStatistics(userId: UserId, volumeId: VolumeId): Flow<TagsMigrationStatistics>
}
