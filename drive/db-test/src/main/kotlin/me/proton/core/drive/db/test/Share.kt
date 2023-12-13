/*
 * Copyright (c) 2023 Proton AG.
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
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.share.data.api.ShareDto
import me.proton.core.drive.share.data.db.ShareEntity
import me.proton.core.user.data.entity.UserEntity


data class ShareContext(
    val db: DriveDatabase,
    val user: UserEntity,
    val share: ShareEntity,
) : BaseContext()

const val shareId = "share-id"

suspend fun VolumeContext.share(
    shareEntity: ShareEntity = NullableShareEntity(
        id = shareId,
        userId = user.userId,
        volumeId = volume.id,
        linkId = "root-id",
        type = ShareDto.TYPE_MAIN,
    ),
    block: suspend ShareContext.() -> Unit
) {
    db.shareDao.insertOrUpdate(shareEntity)
    ShareContext(db, user, shareEntity).block()
}

@Suppress("FunctionName")
fun NullableShareEntity(
    id: String,
    userId: UserId,
    volumeId: String,
    linkId: String,
    type: Long = ShareDto.TYPE_STANDARD,
): ShareEntity {
    return ShareEntity(
        id = id,
        userId = userId,
        volumeId = volumeId,
        addressId = null,
        flags = 0,
        linkId = linkId,
        isLocked = false,
        key = "",
        passphrase = "",
        passphraseSignature = "",
        creationTime = null,
        type = type,
    )
}
