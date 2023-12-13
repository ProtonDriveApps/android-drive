/*
 * Copyright (c) 2021-2023 Proton AG.
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
package me.proton.core.drive.share.data.extension

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.share.data.api.ShareDto
import me.proton.core.drive.share.data.db.ShareEntity
import me.proton.core.drive.share.data.db.ShareEntity.Companion.PRIMARY_BIT
import me.proton.core.drive.share.domain.entity.Share
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.volume.domain.entity.VolumeId

fun ShareEntity.toShare(userId: UserId) =
    Share(
        id = ShareId(userId, id),
        volumeId = VolumeId(volumeId),
        rootLinkId = linkId,
        addressId = addressId,
        isMain = (flags and PRIMARY_BIT) == 1L,
        isLocked = isLocked,
        key = key,
        passphrase = passphrase,
        passphraseSignature = passphraseSignature,
        creationTime = creationTime?.let { TimestampS(creationTime) },
        type = type.toShareType(),
    )

fun Long?.toShareType() = when (this) {
    ShareDto.TYPE_MAIN -> Share.Type.MAIN
    ShareDto.TYPE_STANDARD -> Share.Type.STANDARD
    ShareDto.TYPE_DEVICE -> Share.Type.DEVICE
    ShareDto.TYPE_PHOTO -> Share.Type.PHOTO
    else -> Share.Type.UNKNOWN
}
