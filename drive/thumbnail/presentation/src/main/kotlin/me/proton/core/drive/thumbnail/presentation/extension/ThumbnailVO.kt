/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.core.drive.thumbnail.presentation.extension

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Scale
import coil.size.Size
import me.proton.core.drive.link.domain.entity.RevisionContext
import me.proton.core.drive.link.domain.extension.nodeUid
import me.proton.core.drive.link.domain.extension.revisionId
import me.proton.core.drive.thumbnail.presentation.entity.ThumbnailVO
import me.proton.core.drive.thumbnail.presentation.painter.ThumbnailPainterWrapper
import me.proton.core.drive.volume.domain.extension.volumeId

val ThumbnailVO.revisionContext: RevisionContext get() = RevisionContext(
    userId = userId,
    revisionUid = revisionUid,
    volumeType = volumeType,
)

val ThumbnailVO.cacheKey: String get() =
    "${revisionUid.nodeUid.volumeId}-${revisionUid.revisionId}-$type"

fun ThumbnailVO.preCache(context: Context, imageLoader: ImageLoader) = imageLoader.enqueue(
    request = ImageRequest.Builder(context)
        .data(this)
        .memoryCacheKey(cacheKey)
        .build()
)

@Composable
fun ThumbnailVO.painter() = ThumbnailPainterWrapper(
    painter = rememberAsyncImagePainter(
        ImageRequest.Builder(LocalContext.current)
            .data(this)
            .scale(Scale.FILL)
            .size(Size.ORIGINAL)
            .build()
    )
)
