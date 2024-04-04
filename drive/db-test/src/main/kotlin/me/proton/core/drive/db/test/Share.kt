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
@file:Suppress("MatchingDeclarationName")
package me.proton.core.drive.db.test

import me.proton.android.drive.db.DriveDatabase
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.share.data.api.ShareDto
import me.proton.core.drive.share.data.db.ShareEntity
import me.proton.core.drive.volume.data.db.VolumeEntity
import me.proton.core.user.data.entity.UserEntity
import me.proton.core.user.domain.entity.AddressId


data class ShareContext(
    val db: DriveDatabase,
    val user: UserEntity,
    val volume: VolumeEntity,
    val share: ShareEntity,
) : BaseContext()

suspend fun <T> VolumeContext.share(
    shareEntity: ShareEntity = NullableShareEntity(
        id = volume.shareId,
        userId = user.userId,
        volumeId = volume.id,
        linkId = "main-root-id",
        type = ShareDto.TYPE_MAIN,
    ),
    block: suspend ShareContext.() -> T,
): T {
    db.shareDao.insertOrUpdate(shareEntity)
    return ShareContext(db, user, volume, shareEntity).block()
}

@Suppress("FunctionName")
fun NullableShareEntity(
    id: String,
    userId: UserId,
    volumeId: String,
    linkId: String,
    type: Long = ShareDto.TYPE_STANDARD,
    addressId: AddressId? = AddressId("address-id-$id"),
    key : String = "key-$id",
    passphrase : String = "passphrase-$id",
    passphraseSignature : String = "passphrase-signature-$id",
): ShareEntity {
    return ShareEntity(
        id = id,
        userId = userId,
        volumeId = volumeId,
        addressId = addressId,
        flags = if (type == ShareDto.TYPE_MAIN) 1 else 0,
        linkId = linkId,
        isLocked = false,
        key = key,
        passphrase = passphrase,
        passphraseSignature = passphraseSignature,
        creationTime = null,
        type = type,
    )
}
