/*
 * Copyright (c) 2023 Proton AG.
 * This file is part of Proton Drive.
 *
 * Proton Drive is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Drive is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Drive.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.android.drive.photos.data.repository

import android.content.Context
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import me.proton.android.drive.photos.data.db.PhotosDatabase
import me.proton.android.drive.photos.data.db.entity.MediaStoreVersionEntity
import me.proton.android.drive.photos.domain.repository.MediaStoreVersionRepository
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class MediaStoreVersionRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: PhotosDatabase,
) : MediaStoreVersionRepository {
    override suspend fun getLastVersion(userId: UserId): String? =
        database.mediaStoreVersionDao.get(userId, MediaStore.VOLUME_EXTERNAL)?.version

    override suspend fun setLastVersion(userId: UserId, version: String?) {
        database.mediaStoreVersionDao.insertOrUpdate(
            MediaStoreVersionEntity(
                userId = userId,
                volumeName = MediaStore.VOLUME_EXTERNAL,
                version = version
            )
        )
    }

    override suspend fun getCurrentVersion(): String =
        MediaStore.getVersion(context, MediaStore.VOLUME_EXTERNAL)

}
