/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.core.drive.db.test

import me.proton.android.drive.db.DriveDatabase
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.share.data.api.ShareDto
import me.proton.core.drive.share.domain.entity.ShareId

suspend fun DriveDatabaseRule.device(index: Int = defaultIndex, block: suspend FolderContext.() -> Unit): FolderId {
    return db.device(index, block)
}

fun deviceShareId(index: Int = defaultIndex) = ShareId(userId, "device-share-id-$index")
fun deviceRootId(index: Int = defaultIndex) = FolderId(photoShareId, "device-${index}-root-id")

suspend fun DriveDatabase.device(index: Int = defaultIndex, block: suspend FolderContext.() -> Unit): FolderId {
    user {
        volume {
            deviceShare(index, block)
        }
    }
    return deviceRootId(index)
}

suspend fun VolumeContext.deviceShare(index: Int = defaultIndex, block: suspend FolderContext.() -> Unit) {
    share(
        shareEntity = NullableShareEntity(
            id = deviceShareId(index).id,
            userId = user.userId,
            volumeId = volumeId.id,
            linkId = deviceRootId(index).id,
            type = ShareDto.TYPE_DEVICE,
        )
    ) {
        folder(id = share.linkId, block = block)
    }
}

private val defaultIndex = 1
