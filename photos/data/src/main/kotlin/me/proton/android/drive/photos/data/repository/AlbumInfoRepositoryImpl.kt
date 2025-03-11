/*
 * Copyright (c) 2025 Proton AG.
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

import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.emptyFlow
import me.proton.android.drive.photos.domain.entity.NewAlbumInfo
import me.proton.android.drive.photos.domain.repository.AlbumInfoRepository
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.datastore.GetUserDataStore
import me.proton.core.drive.base.data.extension.get
import me.proton.core.drive.link.domain.entity.FileId
import javax.inject.Inject

class AlbumInfoRepositoryImpl @Inject constructor(
    private val getUserDataStore: GetUserDataStore,
) : AlbumInfoRepository {

    override suspend fun getInfo(userId: UserId): NewAlbumInfo = NewAlbumInfo(
        name = getUserDataStore(userId).get(GetUserDataStore.Keys.newAlbumName),
        items = emptyFlow(),
    )

    override suspend fun updateName(userId: UserId, name: String) {
        getUserDataStore(userId).edit { preferences ->
            preferences[GetUserDataStore.Keys.newAlbumName] = name
        }
    }

    override suspend fun addFileId(vararg fileId: FileId) {
        TODO("Not yet implemented")
    }

    override suspend fun removeFileId(vararg fileId: FileId) {
        TODO("Not yet implemented")
    }

    override suspend fun clear(userId: UserId) {
        getUserDataStore(userId).edit { preferences ->
            preferences.remove(GetUserDataStore.Keys.newAlbumName)
        }
    }
}
