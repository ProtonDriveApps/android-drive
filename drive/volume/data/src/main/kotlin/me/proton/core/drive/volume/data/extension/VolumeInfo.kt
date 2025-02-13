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
package me.proton.core.drive.volume.data.extension

import me.proton.core.drive.volume.data.api.request.CreatePhotoVolumeRequest
import me.proton.core.drive.volume.data.api.request.CreateVolumeRequest
import me.proton.core.drive.volume.domain.entity.VolumeInfo

fun VolumeInfo.toCreateVolumeRequest() =
    CreateVolumeRequest(
        addressId = addressId.id,
        shareKey = shareKey,
        sharePassphrase = sharePassphrase,
        sharePassphraseSignature = sharePassphraseSignature,
        folderName = folderName,
        folderKey = folderKey,
        folderPassphrase = folderPassphrase,
        folderPassphraseSignature = folderPassphraseSignature,
        folderHashKey = folderHashKey,
        addressKeyId = addressKeyId,
    )

fun VolumeInfo.toCreatePhotoVolumeRequest() =
    CreatePhotoVolumeRequest(
        share = CreatePhotoVolumeRequest.Share(
            addressId = addressId.id,
            key = shareKey,
            passphrase = sharePassphrase,
            passphraseSignature = sharePassphraseSignature,
            addressKeyId = addressKeyId,
        ),
        link = CreatePhotoVolumeRequest.Link(
            name = folderName,
            nodeKey = folderKey,
            nodePassphrase = folderPassphrase,
            nodePassphraseSignature = folderPassphraseSignature,
            nodeHashKey = folderHashKey,
        ),
    )
