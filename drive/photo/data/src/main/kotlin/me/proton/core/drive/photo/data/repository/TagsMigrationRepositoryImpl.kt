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

package me.proton.core.drive.photo.data.repository

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.transformLatest
import me.proton.core.domain.arch.mapSuccessValueOrNull
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.datastore.GetUserDataStore
import me.proton.core.drive.base.data.datastore.GetUserDataStore.Keys.photosMigrationTagsLastFinishedUpdate
import me.proton.core.drive.base.data.extension.get
import me.proton.core.drive.base.domain.extension.filterSuccessOrError
import me.proton.core.drive.base.domain.extension.orNow
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.PhotoTag
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.photo.data.api.PhotoApiDataSource
import me.proton.core.drive.photo.data.db.PhotoDatabase
import me.proton.core.drive.photo.data.db.entity.TagsMigrationFileTagEntity
import me.proton.core.drive.photo.data.extension.toEntity
import me.proton.core.drive.photo.data.extension.toRequest
import me.proton.core.drive.photo.domain.entity.TagsMigrationFile
import me.proton.core.drive.photo.domain.entity.TagsMigrationStatistics
import me.proton.core.drive.photo.domain.entity.TagsMigrationStatus
import me.proton.core.drive.photo.domain.repository.TagsMigrationRepository
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.drive.volume.domain.usecase.GetVolume
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class TagsMigrationRepositoryImpl @Inject constructor(
    private val database: PhotoDatabase,
    private val api: PhotoApiDataSource,
    private val configurationProvider: ConfigurationProvider,
    private val getVolume: GetVolume,
    private val getUserDataStore: GetUserDataStore,
) : TagsMigrationRepository {
    override suspend fun getStatus(
        userId: UserId,
        volumeId: VolumeId,
    ): TagsMigrationStatus = getVolume(userId, volumeId).toResult().getOrThrow().let { volume ->
        val lastFinishedUpdate = getUserDataStore(userId).get(photosMigrationTagsLastFinishedUpdate)
        if (lastFinishedUpdate == null) {
            api.getTagsMigrationStatus(userId, volumeId)
                .toEntity(ShareId(userId, volume.shareId))
                .apply {
                    if (finished) {
                        updatePhotosMigrationTags(userId)
                    }
                }
        } else {
            TagsMigrationStatus(finished = true, anchor = null)
        }
    }

    override fun getStatusFlow(
        userId: UserId,
        volumeId: VolumeId,
    ): Flow<TagsMigrationStatus> = getVolume(userId, volumeId)
        .filterSuccessOrError()
        .mapSuccessValueOrNull()
        .filterNotNull()
        .transformLatest { volume ->
            runCatching { getStatus(userId, volume.id) }.getOrNull()?.let { status ->
                emit(status)
            }
            emitAll(getUserDataStore(userId).data.mapLatest { preferences: Preferences ->
                val lastFinishedUpdate = preferences[photosMigrationTagsLastFinishedUpdate]
                TagsMigrationStatus(lastFinishedUpdate != null, null)
            })
        }
        .distinctUntilChanged()

    override suspend fun updateStatus(
        userId: UserId,
        volumeId: VolumeId,
        status: TagsMigrationStatus
    ) {
        api.postTagsMigrationStatus(userId, volumeId, status.toRequest())
        if (status.finished) {
            status.updatePhotosMigrationTags(userId)
        }
    }

    override suspend fun insertFiles(files: List<TagsMigrationFile>) {
        database.inTransaction {
            files.chunked(configurationProvider.dbPageSize).onEach { chunkedFiles ->
                database.tagsMigrationFileDao.insertOrIgnore(
                    *chunkedFiles.map { it.toEntity() }.toTypedArray()
                )
            }
        }
    }

    override suspend fun getFile(
        volumeId: VolumeId,
        fileId: FileId,
    ): TagsMigrationFile? = database.tagsMigrationFileDao.getFile(
        userId = fileId.userId,
        shareId = fileId.shareId.id,
        fileId = fileId.id,
    )?.toEntity()

    override suspend fun getFilesByState(
        userId: UserId,
        volumeId: VolumeId,
        state: TagsMigrationFile.State,
        count: Int,
    ): List<TagsMigrationFile> = database.tagsMigrationFileDao.getFilesByState(
        userId = userId,
        volumeId = volumeId.id,
        state = state,
        count = count,
    ).map { it.toEntity() }

    override suspend fun getBatchFilesByState(
        userId: UserId,
        volumeId: VolumeId,
        state: TagsMigrationFile.State,
        count: Int,
    ): List<TagsMigrationFile> = database.tagsMigrationFileDao.getBatchFilesByState(
        userId = userId,
        volumeId = volumeId.id,
        state = state,
        count = count,
    ).map { it.toEntity() }

    override suspend fun getLatestDownloadedFile(
        userId: UserId,
        volumeId: VolumeId,
    ): Flow<TagsMigrationFile?> = database.tagsMigrationFileDao.getLatestDownloadedFile(
        userId = userId,
        volumeId = volumeId.id,
    ).map { entity ->
        entity?.toEntity()
    }.distinctUntilChanged()

    override suspend fun getLatestFileByState(
        userId: UserId,
        volumeId: VolumeId,
        state: TagsMigrationFile.State,
    ): Flow<TagsMigrationFile?> = database.tagsMigrationFileDao.getLatestFileWithState(
        userId = userId,
        volumeId = volumeId.id,
        state = state,
    ).map { entity ->
        entity?.toEntity()
    }.distinctUntilChanged()

    override suspend fun getOldestFileWithState(
        userId: UserId,
        volumeId: VolumeId,
        state: TagsMigrationFile.State,
    ): Flow<TagsMigrationFile?> = database.tagsMigrationFileDao.getOldestFileWithState(
        userId = userId,
        volumeId = volumeId.id,
        state = state,
    ).map { entity ->
        entity?.toEntity()
    }.distinctUntilChanged()

    override suspend fun updateState(
        volumeId: VolumeId,
        fileId: FileId,
        state: TagsMigrationFile.State
    ) {
        database.tagsMigrationFileDao.updateState(
            userId = fileId.userId,
            shareId = fileId.shareId.id,
            fileId = fileId.id,
            state = state,
        )
    }

    override suspend fun updateUri(volumeId: VolumeId, fileId: FileId, uriString: String?) {
        database.tagsMigrationFileDao.updateUri(
            userId = fileId.userId,
            shareId = fileId.shareId.id,
            fileId = fileId.id,
            uriString = uriString,
        )
    }

    override suspend fun updateMimeType(volumeId: VolumeId, fileId: FileId, mimeType: String?) {
        database.tagsMigrationFileDao.updateMimeType(
            userId = fileId.userId,
            shareId = fileId.shareId.id,
            fileId = fileId.id,
            mimeType = mimeType,
        )
    }

    override suspend fun removeAll(userId: UserId, volumeId: VolumeId) {
        database.tagsMigrationFileDao.deleteAll(
            userId = userId,
            volumeId = volumeId.id,
        )
    }

    override suspend fun insertTags(
        volumeId: VolumeId,
        fileId: FileId,
        tags: Set<PhotoTag>,
    ) {
        database.tagsMigrationFileTagDao.insertOrIgnore(
            *tags.map { tag ->
                TagsMigrationFileTagEntity(
                    userId = fileId.userId,
                    volumeId = volumeId.id,
                    shareId = fileId.shareId.id,
                    linkId = fileId.id,
                    tag = tag.value,
                )
            }.toTypedArray()
        )
    }

    override suspend fun getTags(
        volumeId: VolumeId,
        fileId: FileId,
    ): Set<PhotoTag> = database.tagsMigrationFileTagDao.getTags(
        userId = fileId.userId,
        volumeId = volumeId.id,
        shareId = fileId.shareId.id,
        fileId = fileId.id,
    ).mapNotNull { entity -> PhotoTag.fromLong(entity.tag) }.toSet()

    override fun getStatistics(
        userId: UserId,
        volumeId: VolumeId
    ): Flow<TagsMigrationStatistics> = database.tagsMigrationFileDao.getStatistics(
        userId = userId,
        volumeId = volumeId.id,
    ).map { data -> TagsMigrationStatistics(data.map { it.state to it.count }.toMap()) }

    private suspend fun TagsMigrationStatus.updatePhotosMigrationTags(userId: UserId) {
        getUserDataStore(userId).edit { preferences ->
            preferences[photosMigrationTagsLastFinishedUpdate] = (anchor?.currentTimestamp.orNow).value
        }
    }
}
