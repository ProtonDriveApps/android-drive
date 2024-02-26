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
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.CryptoProperty
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.device.data.api.entity.DeviceDto
import me.proton.core.drive.device.data.db.entity.DeviceEntity
import me.proton.core.drive.device.domain.entity.Device
import me.proton.core.drive.device.domain.entity.DeviceId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.volume.domain.entity.VolumeId

fun DeviceDto.toDeviceEntity(userId: UserId) = DeviceEntity(
    userId = userId,
    volumeId = device.volumeId,
    shareId = share.shareId,
    linkId = share.linkId,
    id = device.deviceId,
    type = device.type,
    syncState = device.syncState,
    creationTime = device.createTime,
    lastModified = device.modifyTime,
    lastSynced = device.lastSyncTime,
    name = share.name,
)

fun DeviceDto.toDevice(userId: UserId) = Device(
    id = DeviceId(device.deviceId),
    volumeId = VolumeId(device.volumeId),
    rootLinkId = FolderId(ShareId(userId, share.shareId), share.linkId),
    type = device.type.toDeviceType(),
    syncState = device.syncState.toDeviceSyncState(),
    lastSynced = device.lastSyncTime?.let { lastSyncTime -> TimestampS(lastSyncTime) },
    lastModified = device.modifyTime?.let { modifyTime -> TimestampS(modifyTime) },
    creationTime = TimestampS(device.createTime),
    cryptoName = takeIf { share.name.isNotBlank() }
        ?.let { CryptoProperty.Decrypted(share.name, VerificationStatus.Unknown) }
        ?: CryptoProperty.Encrypted(""),
)
