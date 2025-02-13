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
package me.proton.core.drive.volume.domain.entity

import me.proton.core.user.domain.entity.AddressId

sealed interface VolumeInfo {
    val addressId: AddressId
    val shareKey: String
    val sharePassphrase: String
    val sharePassphraseSignature: String
    val folderName: String
    val folderKey: String
    val folderPassphrase: String
    val folderPassphraseSignature: String
    val folderHashKey: String
    val addressKeyId: String

    data class Regular(
        override val addressId: AddressId,
        override val shareKey: String,
        override val sharePassphrase: String,
        override val sharePassphraseSignature: String,
        override val addressKeyId: String,
        override val folderName: String,
        override val folderKey: String,
        override val folderPassphrase: String,
        override val folderPassphraseSignature: String,
        override val folderHashKey: String,
    ) : VolumeInfo

    data class Photo(
        override val addressId: AddressId,
        override val shareKey: String,
        override val sharePassphrase: String,
        override val sharePassphraseSignature: String,
        override val addressKeyId: String,
        override val folderName: String,
        override val folderKey: String,
        override val folderPassphrase: String,
        override val folderPassphraseSignature: String,
        override val folderHashKey: String,
    ) : VolumeInfo

    companion object {
        const val DEFAULT_ROOT_FOLDER_NAME = "root"
        const val DEFAULT_PHOTOS_ROOT_FOLDER_NAME = "PhotosRoot"
    }
}
