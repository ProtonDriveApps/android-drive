/*
 * Copyright (c) 2025 Proton AG.
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

package me.proton.android.drive.photos.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.SnackbarData
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import me.proton.android.drive.photos.presentation.extension.processAdd
import me.proton.core.compose.component.ProtonSnackbar
import me.proton.core.compose.component.ProtonSnackbarType
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.drive.base.domain.api.ProtonApiCode
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import me.proton.core.drive.photo.domain.entity.AddToRemoveFromAlbumResult

@Preview
@Composable
fun AddToAlbumResultLightPreview() {
    ProtonTheme(isDark = false) {
        AddToAlbumResultPreview()
    }
}

@Preview
@Composable
fun AddToAlbumResultDarkPreview() {
    ProtonTheme(isDark = true) {
        AddToAlbumResultPreview()
    }
}

@Preview
@Composable
private fun AddToAlbumResultPreview() {
    Surface(
        color = ProtonTheme.colors.backgroundNorm,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
        ) {
            PreviewHelper(allSuccess)
            PreviewHelper(alreadyExistsAndSuccess)
            PreviewHelper(allAlreadyExists)
            PreviewHelper(allFailed)
            PreviewHelper(failedAndSucceed)
            PreviewHelper(failedAndAlreadyExists)
            PreviewHelper(failedSucceedAndAlreadyExists)
        }
    }
}

@Composable
private fun PreviewHelper(
    addToRemoveFromAlbumResult: AddToRemoveFromAlbumResult,
) {
    val localContext = LocalContext.current
    var message = ""
    var type = BroadcastMessage.Type.INFO
    addToRemoveFromAlbumResult.processAdd(localContext) { resultMessage, resultType ->
        message = resultMessage
        type = resultType
    }
    ProtonSnackbar(
        snackbarData = previewSnackbarData(message),
        type = type.toProtonSnackbarType(),
    )
}

private fun BroadcastMessage.Type.toProtonSnackbarType() = when (this) {
    BroadcastMessage.Type.INFO -> ProtonSnackbarType.NORM
    BroadcastMessage.Type.SUCCESS -> ProtonSnackbarType.SUCCESS
    BroadcastMessage.Type.WARNING -> ProtonSnackbarType.WARNING
    BroadcastMessage.Type.ERROR -> ProtonSnackbarType.ERROR
}

private fun previewSnackbarData(message: String) = object : SnackbarData {
    override val actionLabel: String? = null
    override val duration: SnackbarDuration = SnackbarDuration.Indefinite
    override val message: String = message
    override fun dismiss() = Unit
    override fun performAction() = Unit
}

private val allSuccess = AddToRemoveFromAlbumResult(
    listOf(
        AddToRemoveFromAlbumResult.AddRemovePhotoResult.Success("link-1"),
        AddToRemoveFromAlbumResult.AddRemovePhotoResult.Success("link-2"),
    )
)

private val alreadyExistsAndSuccess = AddToRemoveFromAlbumResult(
    listOf(
        AddToRemoveFromAlbumResult.AddRemovePhotoResult.Error("link-1", ProtonApiCode.ALREADY_EXISTS.toLong(), null),
        AddToRemoveFromAlbumResult.AddRemovePhotoResult.Success("link-2"),
        AddToRemoveFromAlbumResult.AddRemovePhotoResult.Error("link-3", ProtonApiCode.ALREADY_EXISTS.toLong(), null),
    )
)

private val allAlreadyExists = AddToRemoveFromAlbumResult(
    listOf(
        AddToRemoveFromAlbumResult.AddRemovePhotoResult.Error("link-1", ProtonApiCode.ALREADY_EXISTS.toLong(), null),
        AddToRemoveFromAlbumResult.AddRemovePhotoResult.Error("link-2", ProtonApiCode.ALREADY_EXISTS.toLong(), null),
        AddToRemoveFromAlbumResult.AddRemovePhotoResult.Error("link-3", ProtonApiCode.ALREADY_EXISTS.toLong(), null),
        AddToRemoveFromAlbumResult.AddRemovePhotoResult.Error("link-4", ProtonApiCode.ALREADY_EXISTS.toLong(), null),
        AddToRemoveFromAlbumResult.AddRemovePhotoResult.Error("link-5", ProtonApiCode.ALREADY_EXISTS.toLong(), null),
    )
)

private val allFailed = AddToRemoveFromAlbumResult(
    listOf(
        AddToRemoveFromAlbumResult.AddRemovePhotoResult.Error("link-1", ProtonApiCode.NOT_ALLOWED.toLong(), null),
        AddToRemoveFromAlbumResult.AddRemovePhotoResult.Error("link-2", ProtonApiCode.NOT_ALLOWED.toLong(), null),
        AddToRemoveFromAlbumResult.AddRemovePhotoResult.Error("link-3", ProtonApiCode.NOT_ALLOWED.toLong(), null),
        AddToRemoveFromAlbumResult.AddRemovePhotoResult.Error("link-4", ProtonApiCode.NOT_ALLOWED.toLong(), null),
        AddToRemoveFromAlbumResult.AddRemovePhotoResult.Error("link-5", ProtonApiCode.NOT_ALLOWED.toLong(), null),
        AddToRemoveFromAlbumResult.AddRemovePhotoResult.Error("link-6", ProtonApiCode.NOT_ALLOWED.toLong(), null),
        AddToRemoveFromAlbumResult.AddRemovePhotoResult.Error("link-7", ProtonApiCode.NOT_ALLOWED.toLong(), null),
    )
)

private val failedAndSucceed = AddToRemoveFromAlbumResult(
    listOf(
        AddToRemoveFromAlbumResult.AddRemovePhotoResult.Error("link-1", ProtonApiCode.NOT_ALLOWED.toLong(), null),
        AddToRemoveFromAlbumResult.AddRemovePhotoResult.Error("link-2", ProtonApiCode.NOT_ALLOWED.toLong(), null),
        AddToRemoveFromAlbumResult.AddRemovePhotoResult.Error("link-3", ProtonApiCode.NOT_ALLOWED.toLong(), null),
        AddToRemoveFromAlbumResult.AddRemovePhotoResult.Success("link-4"),
    )
)

private val failedAndAlreadyExists = AddToRemoveFromAlbumResult(
    listOf(
        AddToRemoveFromAlbumResult.AddRemovePhotoResult.Error("link-1", ProtonApiCode.NOT_ALLOWED.toLong(), null),
        AddToRemoveFromAlbumResult.AddRemovePhotoResult.Error("link-2", ProtonApiCode.NOT_ALLOWED.toLong(), null),
        AddToRemoveFromAlbumResult.AddRemovePhotoResult.Error("link-3", ProtonApiCode.ALREADY_EXISTS.toLong(), null),
        AddToRemoveFromAlbumResult.AddRemovePhotoResult.Error("link-4", ProtonApiCode.ALREADY_EXISTS.toLong(), null),
        AddToRemoveFromAlbumResult.AddRemovePhotoResult.Error("link-5", ProtonApiCode.ALREADY_EXISTS.toLong(), null),
    )
)

private val failedSucceedAndAlreadyExists = AddToRemoveFromAlbumResult(
    listOf(
        AddToRemoveFromAlbumResult.AddRemovePhotoResult.Success("link-1"),
        AddToRemoveFromAlbumResult.AddRemovePhotoResult.Success("link-2"),
        AddToRemoveFromAlbumResult.AddRemovePhotoResult.Success("link-3"),
        AddToRemoveFromAlbumResult.AddRemovePhotoResult.Error("link-4", ProtonApiCode.NOT_ALLOWED.toLong(), null),
        AddToRemoveFromAlbumResult.AddRemovePhotoResult.Error("link-5", ProtonApiCode.ALREADY_EXISTS.toLong(), null),
        AddToRemoveFromAlbumResult.AddRemovePhotoResult.Error("link-6", ProtonApiCode.ALREADY_EXISTS.toLong(), null),
    )
)
