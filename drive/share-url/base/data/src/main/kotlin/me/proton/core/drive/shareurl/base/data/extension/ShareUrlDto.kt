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
package me.proton.core.drive.shareurl.base.data.extension

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.shareurl.base.data.api.entity.ShareUrlDto
import me.proton.core.drive.shareurl.base.data.db.entity.ShareUrlEntity
import me.proton.core.drive.shareurl.base.domain.entity.ShareUrl
import me.proton.core.drive.shareurl.base.domain.entity.ShareUrlId
import me.proton.core.drive.shareurl.base.domain.entity.ShareUrlPasswordFlags

fun ShareUrlDto.toShareUrl(userId: UserId) = ShareUrl(
    id = ShareUrlId(ShareId(userId, shareId), shareUrlId),
    name = name,
    token = token,
    creatorEmail = creatorEmail,
    permissions = Permissions(permissions),
    creationTime = TimestampS(createTime),
    expirationTime = expirationTime?.let { expirationTime -> TimestampS(expirationTime) },
    lastAccessTime = lastAccessTime?.let { lastAccessTime -> TimestampS(lastAccessTime) },
    maxAccesses = maxAccesses,
    numberOfAccesses = numberOfAccesses ?: 0,
    flags = ShareUrlPasswordFlags(flags),
    urlPasswordSalt = urlPasswordSalt,
    sharePasswordSalt = sharePasswordSalt,
    srpVerifier = srpVerifier,
    srpModulusId = srpModulusId,
    encryptedUrlPassword = encryptedUrlPassword,
    sharePassphraseKeyPacket = sharePassphraseKeyPacket,
    publicUrl = publicUrl,
)

fun ShareUrlDto.toShareUrlEntity(userId: UserId) = ShareUrlEntity(
    id = shareUrlId,
    userId = userId,
    shareId = shareId,
    name = name,
    token = token,
    creatorEmail = creatorEmail,
    permissions = permissions,
    creationTime = createTime,
    expirationTime = expirationTime,
    lastAccessTime = lastAccessTime,
    maxAccesses = maxAccesses,
    numberOfAccesses = numberOfAccesses ?: 0,
    flags = flags,
    urlPasswordSalt = urlPasswordSalt,
    sharePasswordSalt = sharePasswordSalt,
    srpVerifier = srpVerifier,
    srpModulusId = srpModulusId,
    encryptedUrlPassword = encryptedUrlPassword,
    sharePassphraseKeyPacket = sharePassphraseKeyPacket,
    publicUrl = publicUrl,
)
