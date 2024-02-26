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

package me.proton.core.drive.device.data.extension

import me.proton.core.crypto.common.pgp.VerificationStatus
import me.proton.core.drive.base.domain.entity.CryptoProperty
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.device.data.db.entity.DeviceEntity
import me.proton.core.drive.device.domain.entity.Device
import me.proton.core.drive.device.domain.entity.DeviceId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.volume.domain.entity.VolumeId

fun DeviceEntity.toDevice() = Device(
    id = DeviceId(id),
    volumeId = VolumeId(volumeId),
    rootLinkId = FolderId(ShareId(userId, shareId), linkId),
    type = type.toDeviceType(),
    syncState = syncState.toDeviceSyncState(),
    lastSynced = lastSynced?.let { TimestampS(lastSynced) },
    lastModified = lastModified?.let { TimestampS(lastModified) },
    creationTime = TimestampS(creationTime),
    cryptoName = takeIf { name.isNotBlank() }
        ?.let { CryptoProperty.Decrypted(name, VerificationStatus.Unknown) }
        ?: CryptoProperty.Encrypted(""),
)
