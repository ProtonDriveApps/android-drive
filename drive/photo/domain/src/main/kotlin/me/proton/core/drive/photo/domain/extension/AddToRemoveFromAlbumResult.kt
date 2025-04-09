/*
 * Copyright (c) 2025 Proton AG.
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

package me.proton.core.drive.photo.domain.extension

import me.proton.core.drive.base.domain.api.ProtonApiCode
import me.proton.core.drive.photo.domain.entity.AddToRemoveFromAlbumResult

fun AddToRemoveFromAlbumResult.onSuccess(block: (Int) -> Unit) = apply {
    val total = results.size
    val successCount = results.count { addRemovePhotoResult ->
        addRemovePhotoResult is AddToRemoveFromAlbumResult.AddRemovePhotoResult.Success
    }
    if (successCount == total) {
        block(successCount)
    }
}

fun AddToRemoveFromAlbumResult.onAddToAlreadyExists(block: (Int, Int) -> Unit) = apply {
    val total = results.size
    val successCount = results.count { addRemovePhotoResult ->
        addRemovePhotoResult is AddToRemoveFromAlbumResult.AddRemovePhotoResult.Success
    }
    val alreadyExistsCount = results.count { addRemovePhotoResult ->
        addRemovePhotoResult is AddToRemoveFromAlbumResult.AddRemovePhotoResult.Error &&
                addRemovePhotoResult.code == ProtonApiCode.ALREADY_EXISTS.toLong()
    }
    if (alreadyExistsCount > 0 && successCount + alreadyExistsCount == total) {
        block(alreadyExistsCount, successCount)
    }
}

fun AddToRemoveFromAlbumResult.onAddToFailure(block: (Int, Int, Int) -> Unit) = apply {
    val successCount = results.count { addRemovePhotoResult ->
        addRemovePhotoResult is AddToRemoveFromAlbumResult.AddRemovePhotoResult.Success
    }
    val alreadyExistsCount = results.count { addRemovePhotoResult ->
        addRemovePhotoResult is AddToRemoveFromAlbumResult.AddRemovePhotoResult.Error &&
                addRemovePhotoResult.code == ProtonApiCode.ALREADY_EXISTS.toLong()
    }
    val failedCount = results.count { addRemovePhotoResult ->
        addRemovePhotoResult is AddToRemoveFromAlbumResult.AddRemovePhotoResult.Error &&
                addRemovePhotoResult.code != ProtonApiCode.ALREADY_EXISTS.toLong()
    }
    if (failedCount > 0) {
        block(failedCount, successCount, alreadyExistsCount)
    }
}

fun AddToRemoveFromAlbumResult.onRemoveFromFailure(block: (Int, Int) -> Unit) = apply {
    val successCount = results.count { addRemovePhotoResult ->
        addRemovePhotoResult is AddToRemoveFromAlbumResult.AddRemovePhotoResult.Success
    }
    val failedCount = results.count { addRemovePhotoResult ->
        addRemovePhotoResult is AddToRemoveFromAlbumResult.AddRemovePhotoResult.Error
    }
    if (failedCount > 0) {
        block(failedCount, successCount)
    }
}
