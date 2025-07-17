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

import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.drivelink.crypto.domain.usecase.GetDecryptedDriveLink
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.PhotoTag
import me.proton.core.drive.photo.domain.provider.TagsProvider
import me.proton.core.drive.upload.domain.resolver.UriResolver
import javax.inject.Inject

class FileNameTagsProvider @Inject constructor(
    private val uriResolver: UriResolver,
    private val getDecryptedDriveLink: GetDecryptedDriveLink,
) : TagsProvider {
    override suspend fun invoke(
        uriString: String,
    ): List<PhotoTag> = uriResolver.getName(uriString)?.photoTags().orEmpty()

    override suspend fun invoke(fileId: FileId): List<PhotoTag> =
        getDecryptedDriveLink(fileId).toResult().getOrThrow().name.photoTags()

    private fun String?.photoTags() = listOfNotNull(
        when {
            this == null -> null
            lowercase().startsWith(FILE_NAME_SCREENSHOT_PREFIX) -> PhotoTag.Screenshots
            else -> null
        }
    )

    private companion object {
        const val FILE_NAME_SCREENSHOT_PREFIX = "screenshot"
    }
}
