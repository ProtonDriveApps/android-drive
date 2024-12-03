/*
 * Copyright (c) 2022-2023 Proton AG.
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
package me.proton.core.drive.drivelink.shared.domain.entity

import me.proton.core.drive.base.domain.entity.CryptoProperty
import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.drive.shareurl.base.domain.entity.ShareUrlId
import me.proton.core.drive.volume.domain.entity.VolumeId
import java.util.Date

data class SharedDriveLink(
    val volumeId: VolumeId,
    val shareUrlId: ShareUrlId,
    val publicUrl: CryptoProperty<String>,
    val customPassword: CryptoProperty<String>?,
    val expirationTime: Date?,
    val isLegacy: Boolean,
    val permissions: Permissions,
)
