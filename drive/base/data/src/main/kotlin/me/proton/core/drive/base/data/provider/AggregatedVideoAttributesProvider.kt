/*
 * Copyright (c) 2023 Proton AG.
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

package me.proton.core.drive.base.data.provider

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import me.proton.core.drive.base.domain.entity.FileAttributes
import me.proton.core.drive.base.domain.entity.VideoAttributes
import me.proton.core.drive.base.domain.provider.FileAttributesProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class AggregatedVideoAttributesProvider @Inject constructor(
    private val exifVideoAttributesProvider: ExifVideoAttributesProvider,
    private val mediaRetrieverVideoAttributesProvider: MetadataRetrieverVideoAttributesProvider,
    private val coroutineContext: CoroutineContext = Job() + Dispatchers.IO,
) : FileAttributesProvider {

    override suspend fun getFileAttributes(
        uriString: String,
        mimeType: String,
    ): FileAttributes? = coRunCatching(coroutineContext) {
        val exifAttributes = exifVideoAttributesProvider.getFileAttributes(uriString, mimeType)
        val mediaRetrieverAttributes = mediaRetrieverVideoAttributesProvider.getFileAttributes(uriString, mimeType)
        takeIf { exifAttributes != null || mediaRetrieverAttributes != null }?.let {
            VideoAttributes(
                cameraExifTags = exifAttributes?.cameraExifTags ?: mediaRetrieverAttributes?.cameraExifTags,
                creationDateTime = exifAttributes?.creationDateTime ?: mediaRetrieverAttributes?.creationDateTime,
                duration = mediaRetrieverAttributes?.duration,
                location = exifAttributes?.location,
                resolution = mediaRetrieverAttributes?.resolution ?: exifAttributes?.resolution,
            )
        }
    }.getOrNull()
}
