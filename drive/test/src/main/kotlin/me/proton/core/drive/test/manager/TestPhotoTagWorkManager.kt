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

package me.proton.core.drive.test.manager

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.photo.domain.manager.PhotoTagWorkManager
import me.proton.core.drive.volume.domain.entity.VolumeId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TestPhotoTagWorkManager @Inject constructor() : PhotoTagWorkManager {

    val enqueue = mutableListOf<VolumeId>()
    val cancel = mutableListOf<VolumeId>()
    val prepare = mutableListOf<FileId>()
    val tag = mutableListOf<FileId>()

    override suspend fun enqueue(userId: UserId, volumeId: VolumeId) {
        enqueue += volumeId
    }

    override suspend fun cancel(userId: UserId, volumeId: VolumeId) {
        cancel += volumeId
    }

    override suspend fun prepare(volumeId: VolumeId, fileId: FileId) {
        prepare += fileId
    }

    override suspend fun tag(volumeId: VolumeId, fileId: FileId, updateStatus: Boolean) {
        tag += fileId
    }
}
