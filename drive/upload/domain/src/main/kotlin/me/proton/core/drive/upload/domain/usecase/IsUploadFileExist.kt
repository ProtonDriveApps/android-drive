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

package me.proton.core.drive.upload.domain.usecase

import me.proton.core.drive.linkupload.domain.entity.UploadFileDescription
import me.proton.core.drive.upload.domain.resolver.UriResolver
import javax.inject.Inject

class IsUploadFileExist @Inject constructor(
    private val uriResolver: UriResolver,
) {
    suspend operator fun invoke(uriString: String): Boolean = uriResolver.exists(uriString)

    suspend operator fun invoke(description: UploadFileDescription): Boolean =
        if (description.properties != null) {
            true // optimization for large batch with already fetch properties
        } else {
            uriResolver.exists(description.uri)
        }
}
