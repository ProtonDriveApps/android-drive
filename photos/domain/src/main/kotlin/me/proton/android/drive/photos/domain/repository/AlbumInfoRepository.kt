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

package me.proton.android.drive.photos.domain.repository

import me.proton.android.drive.photos.domain.entity.NewAlbumInfo
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.link.domain.entity.FileId

interface AlbumInfoRepository {

    suspend fun getInfo(userId: UserId): NewAlbumInfo
    suspend fun updateName(userId: UserId, name: String)
    suspend fun addFileId(vararg fileId: FileId)
    suspend fun removeFileId(vararg fileId: FileId)
    suspend fun clear(userId: UserId)
}
