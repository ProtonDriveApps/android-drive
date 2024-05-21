/*
 * Copyright (c) 2024 Proton AG.
 * This file is part of Proton Drive.
 *
 * Proton Drive is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Drive is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Drive.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.core.drive.test.entity

import me.proton.core.drive.volume.data.api.entity.ShareUrlDto

fun NullableShareUrlDto(
    shareId: String,
    shareUrlId: String = "share-url-id-$shareId",
) = ShareUrlDto(
    shareUrlId = shareUrlId,
    shareId = shareId,
    token = "",
    name = "",
    createTime = 0,
    expirationTime = null,
    lastAccessTime = null,
    maxAccesses = null,
    numberOfAccesses = null,
    creatorEmail = "",
    permissions = 1,
    flags = 1,
    urlPasswordSalt = "",
    sharePasswordSalt = "",
    srpVerifier = "",
    srpModulusId = "",
    encryptedUrlPassword = "",
    sharePassphraseKeyPacket = "",
    publicUrl = "",
)
