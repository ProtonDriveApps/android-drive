/*
 * Copyright (c) 2026 Proton AG.
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

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.device.data.db.entity.DeviceEntity
import me.proton.drive.sdk.entity.Device
import me.proton.drive.sdk.entity.DeviceType
import me.proton.drive.sdk.entity.LegacyDeviceUid
import me.proton.drive.sdk.entity.LegacyNodeUid

@Suppress("DEPRECATION")
fun Device.toDeviceEntity(userId: UserId): DeviceEntity {
    val deviceUid = LegacyDeviceUid(uid.value)
    val rootFolderNodeUid = LegacyNodeUid(rootFolderUid.value)
    return DeviceEntity(
        userId = userId,
        volumeId = rootFolderNodeUid.volumeId,
        shareId = shareId,
        linkId = rootFolderNodeUid.linkId,
        id = deviceUid.deviceId,
        type = type.toLong(),
        // SDK does not expose the device's share name
        name = "",
        // SDK does not expose the sync state
        syncState = SYNC_STATE_UNKNOWN,
        creationTime = creationTime.epochSecond,
        lastModified = null,
        lastSynced = lastSyncTime?.epochSecond,
    )
}

private fun DeviceType.toLong(): Long = when (this) {
    DeviceType.WINDOWS -> 1L
    DeviceType.MACOS -> 2L
    DeviceType.LINUX -> 3L
}

private const val SYNC_STATE_UNKNOWN = -1L
