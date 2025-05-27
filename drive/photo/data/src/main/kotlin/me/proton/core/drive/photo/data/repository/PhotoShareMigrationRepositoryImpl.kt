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

import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.datastore.GetUserDataStore
import me.proton.core.drive.base.data.extension.get
import me.proton.core.drive.base.domain.entity.TimestampMs
import me.proton.core.drive.photo.data.api.PhotoApiDataSource
import me.proton.core.drive.photo.domain.entity.PhotoShareMigrationState
import me.proton.core.drive.photo.domain.repository.PhotoShareMigrationRepository
import me.proton.core.drive.volume.domain.entity.VolumeId
import javax.inject.Inject

class PhotoShareMigrationRepositoryImpl @Inject constructor(
    private val getUserDataStore: GetUserDataStore,
    private val api: PhotoApiDataSource,
) : PhotoShareMigrationRepository {

    override suspend fun startMigration(userId: UserId) {
        api.startPhotoShareMigration(userId)
    }

    override suspend fun fetchMigrationStatus(userId: UserId): Pair<Long, VolumeId?> =
        api.getPhotoShareMigrationStatus(userId).let { response ->
            response.code to response.newVolumeId?.let { volumeId -> VolumeId(volumeId) }
        }

    override suspend fun getMigrationState(userId: UserId): PhotoShareMigrationState? =
        getUserDataStore(userId).get(GetUserDataStore.Keys.photoShareMigrationState)?.let { value ->
            PhotoShareMigrationState.valueOf(value)
        }

    override fun getMigrationStateFlow(userId: UserId): Flow<PhotoShareMigrationState?> = flow {
        val dataStore = getUserDataStore(userId)
        emitAll(
            dataStore.data.map { preferences ->
                preferences[GetUserDataStore.Keys.photoShareMigrationState]?.let {
                    PhotoShareMigrationState.valueOf(it)
                }
            }
        )
    }

    override suspend fun setMigrationState(userId: UserId, state: PhotoShareMigrationState) {
        getUserDataStore(userId).edit { preferences ->
            preferences[GetUserDataStore.Keys.photoShareMigrationState] = state.name
        }
    }

    override fun getPhotosImportantUpdatesLastShownFlow(
        userId: UserId
    ): Flow<TimestampMs?> = flow {
        val dataStore = getUserDataStore(userId)
        emitAll(
            dataStore.data.map { preferences ->
                preferences[GetUserDataStore.Keys.photosImportantUpdatesLastShown]?.let { lastShown ->
                    TimestampMs(lastShown)
                }
            }
        )
    }

    override suspend fun setPhotosImportantUpdatesLastShown(
        userId: UserId,
        lastShown: TimestampMs,
    ) {
        getUserDataStore(userId).edit { preferences ->
            preferences[GetUserDataStore.Keys.photosImportantUpdatesLastShown] = lastShown.value
        }
    }

    override suspend fun saveBucketIds(userId: UserId, bucketsIds: List<Int>) {
        getUserDataStore(userId).edit { preferences ->
            preferences[GetUserDataStore.Keys.photosMigrationBucketIds] =
                bucketsIds.map { bucketsId ->
                    bucketsId.toString()
                }.toSet()
        }
    }

    override suspend fun getBucketIds(userId: UserId): List<Int> =
        getUserDataStore(userId).get(GetUserDataStore.Keys.photosMigrationBucketIds)
            .orEmpty()
            .map { stringBucketId -> stringBucketId.toInt() }

    override suspend fun deleteBucketIds(userId: UserId) {
        getUserDataStore(userId).edit {  preferences ->
            preferences.remove(GetUserDataStore.Keys.photosMigrationBucketIds)
        }
    }
}
