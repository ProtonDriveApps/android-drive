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

package me.proton.core.drive.shareurl.base.domain.entity

import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.volume.domain.entity.VolumeId

data class ShareUrlId(val shareId: ShareId, val id: String)

data class ShareUrl(
    val id: ShareUrlId,
    val volumeId: VolumeId,
    val name: String?,
    val token: String,
    val creatorEmail: String,
    val permissions: Permissions,
    val creationTime: TimestampS,
    val expirationTime: TimestampS?,
    val lastAccessTime: TimestampS?,
    val maxAccesses: Long?,
    val numberOfAccesses: Long,
    val flags: ShareUrlPasswordFlags,
    val urlPasswordSalt: String,
    val sharePasswordSalt: String,
    val srpVerifier: String,
    val srpModulusId: String,
    val encryptedUrlPassword: String,
    val sharePassphraseKeyPacket: String,
    val publicUrl: String,
)
