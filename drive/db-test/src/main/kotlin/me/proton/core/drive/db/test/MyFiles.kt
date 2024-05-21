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

suspend fun DriveDatabaseRule.myFiles(
    block: suspend FolderContext.() -> Unit,
): FolderId = db.myFiles(block)

val mainRootId = FolderId(mainShareId, "root-id")

suspend fun DriveDatabase.myFiles(
    block: suspend FolderContext.() -> Unit,
): FolderId = user {
    withKey()
    volume {
        mainShare(block)
    }
}

suspend fun VolumeContext.mainShare(block: suspend FolderContext.() -> Unit): FolderId {
    share(
        shareEntity = NullableShareEntity(
            id = mainShareId.id,
            userId = user.userId,
            volumeId = volumeId.id,
            linkId = mainRootId.id,
            type = ShareDto.TYPE_MAIN,
        )
    ) {
        withKey()
        folder(id = share.linkId, block = block)
    }
    return mainRootId
}
