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

package me.proton.core.drive.device.domain.entity

import me.proton.core.drive.base.domain.entity.CryptoProperty
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.volume.domain.entity.VolumeId

data class DeviceId(val id: String)

data class Device(
    val id: DeviceId,
    val volumeId: VolumeId,
    val rootLinkId: FolderId,
    val type: Type,
    val syncState: SyncState,
    val cryptoName: CryptoProperty<String>,
    val creationTime: TimestampS,
    val lastModified: TimestampS? = null,
    val lastSynced: TimestampS? = null,
) {
    val name: String get() = cryptoName.value

    enum class Type {
        UNKNOWN,
        WINDOWS,
        MAC_OS,
        LINUX,
    }

    enum class SyncState {
        UNKNOWN,
        OFF,
        ON,
    }
}
