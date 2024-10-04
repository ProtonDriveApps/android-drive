/*
 * Copyright (c) 2023-2024 Proton AG.
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

suspend fun DriveDatabaseRule.photo(block: suspend FolderContext.() -> Unit): FolderId {
    return db.photo(block)
}

val photoShareId = ShareId(userId, "photo-share-id")
val photoRootId = FolderId(photoShareId, "photo-root-id")

suspend fun DriveDatabase.photo(
    block: suspend FolderContext.() -> Unit,
): FolderId = user {
    withKey()
    volume {
        photoShare(block)
    }
}

suspend fun VolumeContext.photoShare(
    block: suspend FolderContext.() -> Unit,
) : FolderId = share(
    shareEntity = NullableShareEntity(
        id = photoShareId.id,
        linkId = photoRootId.id,
        type = ShareDto.TYPE_PHOTO,
    )
) {
    withKey()
    folder(id = share.linkId, block = block)
}
