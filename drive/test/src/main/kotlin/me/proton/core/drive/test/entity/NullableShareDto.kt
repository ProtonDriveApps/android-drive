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

import me.proton.core.drive.db.test.volumeId
import me.proton.core.drive.share.data.api.ShareDto

fun NullableShareDto(
    id: String,
    type: Long = ShareDto.TYPE_MAIN,
) = ShareDto(
    id = id,
    type = type,
    state = 1,
    linkId = "root-id-$id",
    volumeId = volumeId.id,
    creator = "",
    flags = 0,
    locked = false,
    key = "",
    passphrase = "s".repeat(32),
    passphraseSignature = "",
    addressId = "address-id",
    creationTime = null
)
