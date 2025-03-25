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

import me.proton.core.drive.link.domain.PhotoTag
import me.proton.core.drive.upload.domain.provider.TagsProvider
import me.proton.core.drive.upload.domain.resolver.UriResolver
import javax.inject.Inject

class ParentNameTagsProvider @Inject constructor(
    private val uriResolver: UriResolver,
) : TagsProvider {
    override suspend fun invoke(
        uriString: String,
    ): List<PhotoTag> = listOfNotNull(
        when (uriResolver.getParentName(uriString)) {
            PARENT_NAME_SCREENSHOT -> PhotoTag.Screenshots
            else -> null
        }
    )

    private companion object {
        const val PARENT_NAME_SCREENSHOT = "Screenshots"
    }
}
