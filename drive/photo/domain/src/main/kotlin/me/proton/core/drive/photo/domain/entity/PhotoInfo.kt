/*
 * Copyright (c) 2023 Proton AG.
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

package me.proton.core.drive.photo.domain.entity

import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.user.domain.entity.AddressId

data class PhotoInfo(
    val volumeId: VolumeId,
    val addressId: AddressId,
    val shareKey: String,
    val sharePassphrase: String,
    val sharePassphraseSignature: String,
    val folderName: String,
    val folderKey: String,
    val folderPassphrase: String,
    val folderPassphraseSignature: String,
    val folderHashKey: String,
) {
    companion object {
        const val DEFAULT_ROOT_FOLDER_NAME = "PhotosRoot"
    }
}
