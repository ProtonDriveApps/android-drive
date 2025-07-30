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

import me.proton.core.drive.base.domain.entity.FileTypeCategory
import me.proton.core.drive.base.domain.entity.toFileTypeCategory
import me.proton.core.drive.base.domain.extension.resolution
import me.proton.core.drive.base.domain.usecase.GetFileAttributes
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.PhotoTag
import me.proton.core.drive.photo.domain.provider.TagsProvider
import me.proton.core.drive.upload.domain.resolver.UriResolver
import javax.inject.Inject

class RatioTagsProvider @Inject constructor(
    private val getFileAttributes: GetFileAttributes,
    private val uriResolver: UriResolver,
) : TagsProvider {
    override suspend fun invoke(
        uriString: String,
    ): List<PhotoTag> {
        val mimeType = uriResolver.getMimeType(uriString)
        if (mimeType?.toFileTypeCategory() != FileTypeCategory.Image) {
            return emptyList()
        }

        val fileAttributes = getFileAttributes(uriString, mimeType)
        val resolution = fileAttributes.resolution ?: return emptyList()

        val ratioVertical = resolution.height.toFloat() / resolution.width
        val ratioHorizontal = resolution.width.toFloat() / resolution.height
        return if (ratioVertical >= RATIO_THRESHOLD || ratioHorizontal >= RATIO_THRESHOLD) {
            listOf(PhotoTag.Panoramas)
        } else {
            emptyList()
        }
    }

    override suspend fun invoke(fileId: FileId): List<PhotoTag> = emptyList()

    private companion object {
        const val RATIO_THRESHOLD = 2F
    }
}
