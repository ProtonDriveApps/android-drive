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

package me.proton.core.drive.photo.data.extension

import me.proton.core.drive.photo.data.api.request.CreatePhotoRequest
import me.proton.core.drive.photo.data.api.request.Link
import me.proton.core.drive.photo.data.api.request.Share
import me.proton.core.drive.photo.domain.entity.PhotoInfo

fun PhotoInfo.toCreatePhotoRequest() =
    CreatePhotoRequest(
        share = Share(
            addressId = addressId.id,
            key = shareKey,
            passphrase = sharePassphrase,
            passphraseSignature = sharePassphraseSignature,
        ),
        link = Link(
            name = folderName,
            nodeKey = folderKey,
            nodePassphrase = folderPassphrase,
            nodePassphraseSignature = folderPassphraseSignature,
            nodeHashKey = folderHashKey,
        ),
    )
