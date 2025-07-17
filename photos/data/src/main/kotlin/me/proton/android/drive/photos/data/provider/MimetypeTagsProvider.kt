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
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.PhotoTag
import me.proton.core.drive.link.domain.usecase.GetLink
import me.proton.core.drive.photo.domain.provider.TagsProvider
import me.proton.core.drive.upload.domain.resolver.UriResolver
import javax.inject.Inject

class MimetypeTagsProvider @Inject constructor(
    private val uriResolver: UriResolver,
    private val getLink: GetLink,
) : TagsProvider {
    override suspend fun invoke(uriString: String): List<PhotoTag> {
        return uriResolver.getMimeType(uriString).photoTags()
    }

    override suspend fun invoke(fileId: FileId): List<PhotoTag> =
        getLink(fileId).toResult().getOrThrow().mimeType.photoTags()

    private fun String?.photoTags() = listOfNotNull(
        when {
            this == null -> null
            this in rawMimetypes -> PhotoTag.Raw
            toFileTypeCategory() == FileTypeCategory.Video -> PhotoTag.Videos
            else -> null
        }
    )

    private companion object {
        val rawMimetypes = listOf(
            "image/x-adobe-dng",
            "image/x-canon-cr2",
            "image/x-canon-crw",
            "image/x-dcraw",
            "image/x-epson-erf",
            "image/x-fuji-raf",
            "image/x-kodak-dcr",
            "image/x-kodak-k25",
            "image/x-kodak-kdc",
            "image/x-minolta-mrw",
            "image/x-nikon-nef",
            "image/x-olympus-orf",
            "image/x-panasonic-raw",
            "image/x-pentax-pef",
            "image/x-sigma-x3f",
            "image/x-sony-arw",
            "image/x-sony-sr2",
            "image/x-sony-srf",
        )
    }
}
