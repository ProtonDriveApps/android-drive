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

package me.proton.android.drive.photos.data.provider

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.proton.core.drive.base.domain.entity.FileTypeCategory
import me.proton.core.drive.base.domain.entity.toFileTypeCategory
import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.link.domain.PhotoTag
import me.proton.core.drive.upload.domain.provider.TagsProvider
import me.proton.core.drive.upload.domain.resolver.UriResolver
import javax.inject.Inject

class XmpTagsProvider @Inject internal constructor(
    @ApplicationContext private val context: Context,
    private val uriResolver: UriResolver,
    private val parser: XmpTagsMetadataParser,
) : TagsProvider {
    override suspend operator fun invoke(
        uriString: String,
    ): List<PhotoTag> = coRunCatching {
        val mimeType = uriResolver.getMimeType(uriString)
        Uri.parse(uriString)
            .takeIf { mimeType?.toFileTypeCategory() == FileTypeCategory.Image }
            ?.let { fileUri -> extractXmp(fileUri) }
            ?.let { xmpData -> parseXmpToPhotoTags(xmpData) }
    }.getOrNull(LogTag.UPLOAD, "Cannot get tags from xmp").orEmpty()

    private suspend fun extractXmp(fileUri: Uri): String? = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(fileUri)?.use { inputStream ->
            ExifInterface(inputStream).getAttribute(ExifInterface.TAG_XMP)
        }
    }

    private suspend fun parseXmpToPhotoTags(
        xmpData: String,
    ): List<PhotoTag> = parser(xmpData)?.let { metadata ->
        listOfNotNull(
            when {
                metadata.isMotionPhoto -> PhotoTag.MotionPhotos
                metadata.isPanorama -> PhotoTag.Panoramas
                metadata.isPortrait -> PhotoTag.Portraits
                else -> null
            }
        )
    }.orEmpty()
}
