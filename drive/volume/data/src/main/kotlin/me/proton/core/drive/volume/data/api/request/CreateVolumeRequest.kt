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
package me.proton.core.drive.volume.data.api.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.proton.core.drive.base.data.api.Dto.ADDRESS_ID
import me.proton.core.drive.base.data.api.Dto.ADDRESS_KEY_ID
import me.proton.core.drive.base.data.api.Dto.FOLDER_HASH_KEY
import me.proton.core.drive.base.data.api.Dto.FOLDER_KEY
import me.proton.core.drive.base.data.api.Dto.FOLDER_NAME
import me.proton.core.drive.base.data.api.Dto.FOLDER_PASSPHRASE
import me.proton.core.drive.base.data.api.Dto.FOLDER_PASSPHRASE_SIGNATURE
import me.proton.core.drive.base.data.api.Dto.SHARE_KEY
import me.proton.core.drive.base.data.api.Dto.SHARE_NAME
import me.proton.core.drive.base.data.api.Dto.SHARE_PASSPHRASE
import me.proton.core.drive.base.data.api.Dto.SHARE_PASSPHRASE_SIGNATURE
import me.proton.core.drive.base.data.api.Dto.VOLUME_MAX_SPACE
import me.proton.core.drive.base.data.api.Dto.VOLUME_NAME

@Serializable
data class CreateVolumeRequest(
    @SerialName(ADDRESS_ID)
    val addressId: String,
    @SerialName(SHARE_KEY)
    val shareKey: String,
    @SerialName(SHARE_PASSPHRASE)
    val sharePassphrase: String,
    @SerialName(SHARE_PASSPHRASE_SIGNATURE)
    val sharePassphraseSignature: String,
    @SerialName(FOLDER_NAME)
    val folderName: String,
    @SerialName(FOLDER_KEY)
    val folderKey: String,
    @SerialName(FOLDER_PASSPHRASE)
    val folderPassphrase: String,
    @SerialName(FOLDER_PASSPHRASE_SIGNATURE)
    val folderPassphraseSignature: String,
    @SerialName(FOLDER_HASH_KEY)
    val folderHashKey: String,
    @SerialName(ADDRESS_KEY_ID)
    val addressKeyId: String,
)
