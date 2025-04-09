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
package me.proton.android.drive.photos.presentation.extension

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Scale
import coil.size.Size
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.entity.toFileTypeCategory
import me.proton.core.drive.base.presentation.R
import me.proton.core.drive.base.presentation.extension.asHumanReadableString
import me.proton.core.drive.base.presentation.extension.iconResId
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.thumbnail.presentation.extension.photoThumbnailVO
import me.proton.core.drive.thumbnail.presentation.extension.thumbnailVO
import me.proton.core.drive.thumbnail.presentation.painter.ThumbnailPainterWrapper
import me.proton.core.drive.i18n.R as I18N


@Composable
fun DriveLink.thumbnailPainter(
    usePhotoThumbnailVO: Boolean = false,
) = ThumbnailPainterWrapper(
    painter = when {
        this is DriveLink.Folder -> painterResource(id = R.drawable.ic_folder_48)
        this is DriveLink.File && hasThumbnail -> rememberAsyncImagePainter(
            ImageRequest.Builder(LocalContext.current)
                .scale(Scale.FILL)
                .data(if (usePhotoThumbnailVO) photoThumbnailVO() else thumbnailVO())
                .size(Size.ORIGINAL)
                .build()
        )
        else -> painterResource(id = mimeType.toFileTypeCategory().iconResId)
    }
)

fun DriveLink.Album.details(appContext: Context, useCreationTime: Boolean = true): String =
    albumDetails(
        appContext = appContext,
        photoCount = photoCount,
        isShared = link.sharingDetails != null,
        creationTime = link.creationTime.takeIf { useCreationTime },
    )

@VisibleForTesting
fun albumDetails(
    appContext: Context,
    photoCount: Long,
    isShared: Boolean,
    creationTime: TimestampS? = null,
) = buildString {
    if (creationTime != null) {
        append(creationTime.asHumanReadableString())
        append(" \u2022 ")
    }
    append(
        appContext.resources.getQuantityString(
            I18N.plurals.albums_photo_count,
            photoCount.toInt(),
            photoCount.toInt(),
        )
    )
    if (isShared) {
        append(" \u2022 ")
        append(appContext.getString(I18N.string.common_shared))
    }
}
